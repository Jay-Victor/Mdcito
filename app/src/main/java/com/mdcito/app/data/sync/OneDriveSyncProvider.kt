package com.mdcito.app.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class OneDriveSyncProvider @Inject constructor(
    private val settingsRepository: com.mdcito.app.data.repository.SettingsRepository,
) : CloudSyncProvider {

    companion object {
        private const val GRAPH_BASE_URL = "https://graph.microsoft.com/v1.0"
        private const val DEFAULT_AUTH_URL =
            "https://login.microsoftonline.com/common/oauth2/v2.0/authorize"
        private const val DEFAULT_TOKEN_URL =
            "https://login.microsoftonline.com/common/oauth2/v2.0/token"
        private const val REDIRECT_URI = "mdcito://cloud-sync/onedrive/callback"
        private const val SCOPES = "Files.ReadWrite offline_access"
    }

    override val serviceType: CloudSyncServiceType = CloudSyncServiceType.ONEDRIVE

    private val httpClient = OkHttpClient()

    /**
     * 最近一次上传后远程文件的实际修改时间。
     * OneDrive 的 lastModifiedDateTime 是只读属性，上传后会被设为当前时间，
     * CloudSyncManager 需要在上传后读取此值来同步本地文件时间戳。
     */
    var lastUploadRemoteModified: Long = 0L
        private set

    //region OAuth2 / PKCE

    override fun getAuthorizationUrl(config: CloudSyncConfig): String {
        Timber.tag("CloudSync").i("OneDrive: 生成授权 URL")
        val codeVerifier = generateCodeVerifier()
        // 持久化 PKCE code_verifier，防止进程被杀后丢失
        settingsRepository.saveCloudSyncPkceCodeVerifier(codeVerifier)
        val codeChallenge = generateCodeChallengeS256(codeVerifier)

        // 生成随机 state 参数，防止 CSRF 攻击
        val state = generateState()
        settingsRepository.saveCloudSyncOAuthState(state)

        val authUrl = if (config.useCustomAuthEndpoint && config.customAuthEndpoint.isNotBlank()) {
            config.customAuthEndpoint
        } else {
            DEFAULT_AUTH_URL
        }

        return buildString {
            append(authUrl)
            append("?client_id=").append(config.oauthClientId)
            append("&scope=").append(java.net.URLEncoder.encode(SCOPES, "UTF-8"))
            append("&response_type=code")
            append("&redirect_uri=").append(java.net.URLEncoder.encode(REDIRECT_URI, "UTF-8"))
            append("&code_challenge=").append(codeChallenge)
            append("&code_challenge_method=S256")
            append("&state=").append(state)
        }
    }

    override suspend fun exchangeCodeForToken(
        config: CloudSyncConfig,
        authorizationCode: String,
    ): CloudSyncConfig = withContext(Dispatchers.IO) {
        val tokenUrl = if (config.useCustomAuthEndpoint && config.customTokenEndpoint.isNotBlank()) {
            config.customTokenEndpoint
        } else {
            DEFAULT_TOKEN_URL
        }

        val formBody = FormBody.Builder()
            .add("client_id", config.oauthClientId)
            .add("grant_type", "authorization_code")
            .add("code", authorizationCode)
            .add("redirect_uri", REDIRECT_URI)
            .add("code_verifier", settingsRepository.getCloudSyncPkceCodeVerifier())
            .build()

        val request = Request.Builder()
            .url(tokenUrl)
            .post(formBody)
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw IllegalStateException("Empty token response")

            if (!response.isSuccessful) {
                throw IllegalStateException("Token exchange failed: $responseBody")
            }

            val json = JSONObject(responseBody)
            val accessToken = json.optString("access_token", "")
            val refreshToken = json.optString("refresh_token", "")
            val expiresIn = json.optLong("expires_in", 0L)

            val updatedConfig = config.copy(
                accessToken = accessToken,
                refreshToken = refreshToken.ifBlank { config.refreshToken },
                tokenExpiryTime = System.currentTimeMillis() + expiresIn * 1000,
            )
            // 清除加密存储中的 PKCE code_verifier，防止重放攻击
            settingsRepository.saveCloudSyncPkceCodeVerifier("")
            Timber.tag("CloudSync").i("OneDrive: 令牌交换成功，有效期 %ds", expiresIn)
            updatedConfig
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "OneDrive: 令牌交换失败")
            throw IllegalStateException("Failed to exchange authorization code: ${e.message}", e)
        }
    }

    override suspend fun ensureValidToken(config: CloudSyncConfig): CloudSyncConfig {
        if (config.accessToken.isBlank()) return config
        if (config.tokenExpiryTime > System.currentTimeMillis() + 60_000) return config

        Timber.tag("CloudSync").i("OneDrive: 刷新访问令牌")

        return withContext(Dispatchers.IO) {
            if (config.refreshToken.isBlank()) return@withContext config

            val tokenUrl =
                if (config.useCustomAuthEndpoint && config.customTokenEndpoint.isNotBlank()) {
                    config.customTokenEndpoint
                } else {
                    DEFAULT_TOKEN_URL
                }

            val formBody = FormBody.Builder()
                .add("client_id", config.oauthClientId)
                .add("grant_type", "refresh_token")
                .add("refresh_token", config.refreshToken)
                .add("scope", SCOPES)
                .build()

            val request = Request.Builder()
                .url(tokenUrl)
                .post(formBody)
                .build()

            try {
                val response = httpClient.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext config

                if (!response.isSuccessful) throw IllegalStateException("Token refresh failed: HTTP ${response.code}")

                val json = JSONObject(body)
                val accessToken = json.optString("access_token", "")
                val refreshToken = json.optString("refresh_token", "")
                val expiresIn = json.optLong("expires_in", 0L)

                config.copy(
                    accessToken = accessToken,
                    refreshToken = refreshToken.ifBlank { config.refreshToken },
                    tokenExpiryTime = System.currentTimeMillis() + expiresIn * 1000,
                )
            } catch (e: Exception) {
                Timber.tag("CloudSync").e(e, "OneDrive: 刷新令牌失败")
                throw IllegalStateException("Failed to refresh token: ${e.message}", e)
            }
        }
    }

    //endregion

    override suspend fun revokeAuthorization(config: CloudSyncConfig) {
        if (config.accessToken.isBlank()) return
        Timber.tag("CloudSync").i("OneDrive: 撤销授权")
        try {
            // 尝试通过 Microsoft Graph 使刷新令牌失效
            // 参考: https://learn.microsoft.com/en-us/graph/api/user-revokesigninsessions
            val request = Request.Builder()
                .url("https://graph.microsoft.com/v1.0/me/revokeSignInSessions")
                .header("Authorization", "Bearer ${config.accessToken}")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            httpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            // Graph API 撤销失败时，尝试 logout 端点作为后备
            Timber.tag("CloudSync").w(e, "OneDrive: Graph 撤销失败，尝试 logout 后备方案")
            try {
                val fallbackRequest = Request.Builder()
                    .url("https://login.microsoftonline.com/common/oauth2/v2.0/logout")
                    .get()
                    .build()
                httpClient.newCall(fallbackRequest).execute().close()
            } catch (e2: Exception) {
                // 服务端撤销失败不应阻止本地清除
                Timber.tag("CloudSync").w(e2, "OneDrive: logout 后备方案也失败")
            }
        }
    }

    override suspend fun getStorageInfo(config: CloudSyncConfig): StorageInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://graph.microsoft.com/v1.0/me/drive")
                .header("Authorization", "Bearer ${config.accessToken}")
                .get()
                .build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            if (!response.isSuccessful) {
                Timber.tag("CloudSync").w("OneDrive: 获取存储空间信息失败，HTTP %d", response.code)
                return@withContext null
            }
            val json = JSONObject(body)
            val quota = json.optJSONObject("quota") ?: return@withContext null
            val info = StorageInfo(
                usedBytes = quota.optLong("used", 0),
                totalBytes = quota.optLong("total", 0),
            )
            Timber.tag("CloudSync").d("OneDrive: 存储空间 - 已用: %d, 总计: %d", info.usedBytes, info.totalBytes)
            info
        } catch (e: Exception) {
            Timber.tag("CloudSync").w(e, "OneDrive: 获取存储空间信息失败")
            null
        }
    }

    //region CloudSyncProvider implementation

    override suspend fun testConnection(config: CloudSyncConfig): ConnectionTestResult =
        withContext(Dispatchers.IO) {
            try {
                val validConfig = ensureValidToken(config)
                val request = Request.Builder()
                    .url("$GRAPH_BASE_URL/me/drive")
                    .header("Authorization", "Bearer ${validConfig.accessToken}")
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()
                val body = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val driveType = json.optString("driveType", "unknown")
                    val ownerName = json.optJSONObject("owner")
                        ?.optJSONObject("user")
                        ?.optString("displayName", "")
                        ?: ""
                    Timber.tag("CloudSync").i("OneDrive: 连接测试成功 - driveType: %s, owner: %s", driveType, ownerName)
                    ConnectionTestResult(
                        success = true,
                        message = "连接成功",
                        serverInfo = "OneDrive ($driveType)${
                            if (ownerName.isNotBlank()) " - $ownerName" else ""
                        }",
                    )
                } else {
                    Timber.tag("CloudSync").w("OneDrive: 连接测试失败，HTTP %d", response.code)
                    ConnectionTestResult(
                        success = false,
                        message = "连接失败: ${response.code}",
                    )
                }
            } catch (e: Exception) {
                Timber.tag("CloudSync").e(e, "OneDrive: 连接测试失败")
                ConnectionTestResult(
                    success = false,
                    message = "连接失败: ${e.message}",
                )
            }
        }

    override suspend fun listFiles(
        config: CloudSyncConfig,
        remotePath: String,
    ): List<RemoteFileInfo> = withContext(Dispatchers.IO) {
        try {
            val validConfig = ensureValidToken(config)
            val encodedPath = encodePath(remotePath)
            val allFiles = mutableListOf<RemoteFileInfo>()
            var url: String? = "$GRAPH_BASE_URL/me/drive/root:$encodedPath:/children"

            while (url != null) {
                val request = buildGetRequest(url, validConfig.accessToken)
                val response = httpClient.newCall(request).execute()
                val body = response.body?.string() ?: break

                if (!response.isSuccessful) {
                    Timber.tag("CloudSync").w("OneDrive: 列出文件失败，HTTP %d, path=%s", response.code, remotePath)
                    break
                }

                val json = JSONObject(body)
                val valueArray = json.optJSONArray("value")
                if (valueArray != null) {
                    for (i in 0 until valueArray.length()) {
                        parseRemoteFileInfo(valueArray.getJSONObject(i), remotePath).let { allFiles.add(it) }
                    }
                }

                // 处理分页：@odata.nextLink 包含下一页的完整 URL
                url = json.optString("@odata.nextLink", "")
                if (url.isBlank()) url = null
            }

            Timber.tag("CloudSync").d("OneDrive: 列出文件 '%s'，共 %d 项", remotePath, allFiles.size)
            allFiles
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "OneDrive: 列出文件失败：$remotePath")
            throw RuntimeException("列出远程文件失败: ${e.message}", e)
        }
    }

    override suspend fun uploadFile(
        config: CloudSyncConfig,
        remotePath: String,
        content: ByteArray,
        lastModified: Long,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val validConfig = ensureValidToken(config)
            val encodedPath = encodePath(remotePath)
            val url = "$GRAPH_BASE_URL/me/drive/root:$encodedPath:/content"

            val mediaType = "application/octet-stream".toMediaType()
            val requestBody = content.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${validConfig.accessToken}")
                .put(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Timber.tag("CloudSync").w("OneDrive: 上传文件失败，HTTP %d, path=%s", response.code, remotePath)
                return@withContext false
            }

            // OneDrive 的 lastModifiedDateTime 是只读属性，上传后会被设为当前时间
            // 获取上传后的文件元数据，返回实际的修改时间供调用方同步本地时间戳
            try {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val json = JSONObject(responseBody)
                    val remoteModifiedStr = json.optString("lastModifiedDateTime", "")
                    if (remoteModifiedStr.isNotBlank()) {
                        val remoteModified = parseIso8601ToMillis(remoteModifiedStr)
                        if (remoteModified > 0) {
                            lastUploadRemoteModified = remoteModified
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.tag("CloudSync").w(e, "OneDrive: 获取上传后文件元数据失败")
            }

            Timber.tag("CloudSync").i("OneDrive: 上传成功 '%s' (%d bytes)", remotePath, content.size)
            true
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "OneDrive: 上传文件失败：$remotePath")
            throw RuntimeException("上传文件失败: ${e.message}", e)
        }
    }

    override suspend fun downloadFile(
        config: CloudSyncConfig,
        remotePath: String,
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val validConfig = ensureValidToken(config)
            val encodedPath = encodePath(remotePath)
            val url = "$GRAPH_BASE_URL/me/drive/root:$encodedPath:/content"

            val request = buildGetRequest(url, validConfig.accessToken)
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Timber.tag("CloudSync").w("OneDrive: 下载文件失败，HTTP %d, path=%s", response.code, remotePath)
                return@withContext null
            }

            val body = response.body ?: return@withContext null
            val outputStream = ByteArrayOutputStream()
            body.byteStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
            }
            val bytes = outputStream.toByteArray()
            Timber.tag("CloudSync").i("OneDrive: 下载成功 '%s' (%d bytes)", remotePath, bytes.size)
            bytes
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "OneDrive: 下载文件失败：$remotePath")
            throw RuntimeException("下载文件失败: ${e.message}", e)
        }
    }

    override suspend fun deleteFile(
        config: CloudSyncConfig,
        remotePath: String,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val validConfig = ensureValidToken(config)
            val encodedPath = encodePath(remotePath)
            val url = "$GRAPH_BASE_URL/me/drive/root:$encodedPath"

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${validConfig.accessToken}")
                .delete()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Timber.tag("CloudSync").w("OneDrive: 删除文件失败，HTTP %d, path=%s", response.code, remotePath)
            } else {
                Timber.tag("CloudSync").i("OneDrive: 删除文件：$remotePath")
            }
            response.isSuccessful
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "OneDrive: 删除文件失败：$remotePath")
            throw RuntimeException("删除文件失败: ${e.message}", e)
        }
    }

    override suspend fun createDirectory(
        config: CloudSyncConfig,
        remotePath: String,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val validConfig = ensureValidToken(config)
            val pathParts = remotePath.trim('/')
            val lastSlash = pathParts.lastIndexOf('/')
            val parentPath = if (lastSlash > 0) pathParts.substring(0, lastSlash) else ""
            val dirName = if (lastSlash >= 0) pathParts.substring(lastSlash + 1) else pathParts

            val parentEncoded = if (parentPath.isNotBlank()) ":${encodePath(parentPath)}:" else ""
            val url = "$GRAPH_BASE_URL/me/drive/root$parentEncoded/children"

            val jsonBody = JSONObject().apply {
                put("name", dirName)
                put("folder", JSONObject())
                put("@microsoft.graph.conflictBehavior", "rename")
            }

            val mediaType = "application/json".toMediaType()
            val requestBody = jsonBody.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${validConfig.accessToken}")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Timber.tag("CloudSync").w("OneDrive: 创建目录失败，HTTP %d, path=%s", response.code, remotePath)
            } else {
                Timber.tag("CloudSync").i("OneDrive: 创建目录成功：$remotePath")
            }
            response.isSuccessful
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "OneDrive: 创建目录失败：$remotePath")
            throw RuntimeException("创建目录失败: ${e.message}", e)
        }
    }

    override suspend fun getFileInfo(
        config: CloudSyncConfig,
        remotePath: String,
    ): RemoteFileInfo? = withContext(Dispatchers.IO) {
        try {
            val validConfig = ensureValidToken(config)
            val encodedPath = encodePath(remotePath)
            val url = "$GRAPH_BASE_URL/me/drive/root:$encodedPath"

            val request = buildGetRequest(url, validConfig.accessToken)
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null

            if (!response.isSuccessful) {
                Timber.tag("CloudSync").w("OneDrive: 获取文件信息失败，HTTP %d, path=%s", response.code, remotePath)
                return@withContext null
            }

            val json = JSONObject(body)
            val parentPath = remotePath.substringBeforeLast('/', "").let {
                if (it.isBlank()) "/" else it
            }
            val info = parseRemoteFileInfo(json, parentPath)
            Timber.tag("CloudSync").d("OneDrive: 获取文件信息 '%s' -> isDir=%b, size=%d", remotePath, info.isDirectory, info.size)
            info
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "OneDrive: 获取文件信息失败：$remotePath")
            throw RuntimeException("获取文件信息失败: ${e.message}", e)
        }
    }

    //endregion

    //region Helpers

    private fun buildGetRequest(url: String, accessToken: String): Request =
        Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

    private fun encodePath(path: String): String {
        val normalized = path.trimStart('/')
        return normalized.split("/").joinToString("/") {
            java.net.URLEncoder.encode(it, "UTF-8")
        }
    }

    private fun parseRemoteFileInfo(json: JSONObject, parentPath: String): RemoteFileInfo {
        val name = json.optString("name", "")
        val isDirectory = json.has("folder")
        val size = json.optLong("size", 0L)
        val lastModifiedStr = json.optString("lastModifiedDateTime", "")
        val lastModified = parseIso8601ToMillis(lastModifiedStr)
        val etag = json.optString("eTag", "")
        val path = if (parentPath.endsWith("/")) "$parentPath$name" else "$parentPath/$name"

        return RemoteFileInfo(
            path = path,
            name = name,
            isDirectory = isDirectory,
            size = size,
            lastModified = lastModified,
            etag = etag,
        )
    }

    private fun parseIso8601ToMillis(iso8601: String): Long {
        if (iso8601.isBlank()) return 0L
        return try {
            // Microsoft Graph returns format like: 2024-01-15T10:30:00.000Z
            // or with timezone offset: 2024-01-15T10:30:00+08:00 or 2024-01-15T10:30:00-05:00
            val normalized = iso8601.replace("Z", "+00:00")

            // 提取时区偏移（支持正负偏移）
            val offsetRegex = """([+-]\d{2}:\d{2})$""".toRegex()
            val offsetMatch = offsetRegex.find(normalized)
            val offsetPart = offsetMatch?.groupValues?.get(1) ?: "00:00"
            val dateTimePart = normalized.substringBeforeLast(offsetPart).trimEnd('+', '-')

            val parts = dateTimePart.split("T")
            val dateParts = parts[0].split("-")
            val timeParts = parts.getOrNull(1)?.split(":") ?: listOf("0", "0", "0")
            val secondParts = timeParts.getOrNull(2)?.split(".") ?: listOf("0")

            val year = dateParts.getOrNull(0)?.toIntOrNull() ?: 0
            val month = dateParts.getOrNull(1)?.toIntOrNull() ?: 1
            val day = dateParts.getOrNull(2)?.toIntOrNull() ?: 1
            val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
            val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
            val second = secondParts.getOrNull(0)?.toIntOrNull() ?: 0

            val offsetParts = offsetPart.removePrefix("+").split(":")
            val offsetHours = offsetParts.getOrNull(0)?.toIntOrNull() ?: 0
            val offsetMinutes = offsetParts.getOrNull(1)?.toIntOrNull() ?: 0
            val isNegativeOffset = offsetPart.startsWith("-")
            val offsetMillis = (offsetHours * 3600 + offsetMinutes * 60) * 1000L *
                if (isNegativeOffset) -1 else 1

            val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            calendar.set(year, month - 1, day, hour, minute, second)
            calendar.set(java.util.Calendar.MILLISECOND, 0)

            calendar.timeInMillis + offsetMillis - calendar.timeZone.getOffset(calendar.timeInMillis)
        } catch (e: Exception) {
            Timber.tag("CloudSync").w(e, "OneDrive: 解析 ISO8601 日期失败：%s", iso8601)
            0L
        }
    }

    private fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun generateState(): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun generateCodeChallengeS256(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(Charsets.US_ASCII)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    //endregion
}
