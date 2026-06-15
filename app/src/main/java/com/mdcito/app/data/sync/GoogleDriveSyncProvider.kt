package com.mdcito.app.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class GoogleDriveSyncProvider @Inject constructor(
    private val settingsRepository: com.mdcito.app.data.repository.SettingsRepository,
) : CloudSyncProvider {

    companion object {
        private const val BASE_URL = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD_BASE_URL = "https://www.googleapis.com/upload/drive/v3"
        private const val DEFAULT_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
        private const val DEFAULT_TOKEN_URL = "https://oauth2.googleapis.com/token"
        private const val REDIRECT_URI = "mdcito://cloud-sync/google-drive/callback"
        private const val SCOPE = "https://www.googleapis.com/auth/drive.file"
        private const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
        private const val MULTIPART_UPLOAD_SIZE_THRESHOLD = 5 * 1024 * 1024L // 5MB
        private const val FIELDS = "id,name,mimeType,size,modifiedTime,md5Checksum,parents"
    }

    override val serviceType: CloudSyncServiceType = CloudSyncServiceType.GOOGLE_DRIVE

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    /** Cache: path -> Google Drive file ID */
    private val folderIdCache = mutableMapOf<String, String>()

    // ======================== OAuth2 / PKCE ========================

    override fun getAuthorizationUrl(config: CloudSyncConfig): String? {
        if (config.oauthClientId.isBlank()) return null

        Timber.tag("CloudSync").i("GoogleDrive: 生成授权 URL")
        val codeVerifier = generateCodeVerifier()
        // 持久化 PKCE code_verifier，防止进程被杀后丢失
        settingsRepository.saveCloudSyncPkceCodeVerifier(codeVerifier)
        val codeChallenge = generateCodeChallenge(codeVerifier)

        // 生成随机 state 参数，防止 CSRF 攻击
        val state = generateState()
        settingsRepository.saveCloudSyncOAuthState(state)

        val authUrl = if (config.useCustomAuthEndpoint && config.customAuthEndpoint.isNotBlank()) {
            config.customAuthEndpoint
        } else {
            DEFAULT_AUTH_URL
        }

        val params = buildMap {
            put("client_id", config.oauthClientId)
            put("redirect_uri", REDIRECT_URI)
            put("response_type", "code")
            put("scope", SCOPE)
            put("code_challenge", codeChallenge)
            put("code_challenge_method", "S256")
            put("state", state)
            put("access_type", "offline")
            put("prompt", "consent")
        }

        return authUrl + "?" + params.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
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

        val body = buildMap {
            put("client_id", config.oauthClientId)
            put("code", authorizationCode)
            put("redirect_uri", REDIRECT_URI)
            put("grant_type", "authorization_code")
            put("code_verifier", settingsRepository.getCloudSyncPkceCodeVerifier())
        }.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }

        val request = Request.Builder()
            .url(tokenUrl)
            .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw IllegalStateException("Empty token response")

            if (!response.isSuccessful) {
                throw IllegalStateException("Token exchange failed: $responseBody")
            }

            val json = JSONObject(responseBody)
            val accessToken = json.getString("access_token")
            val refreshToken = json.optString("refresh_token", "")
            val expiresIn = json.optLong("expires_in", 3600)

            val updatedConfig = config.copy(
                accessToken = accessToken,
                refreshToken = if (refreshToken.isNotBlank()) refreshToken else config.refreshToken,
                tokenExpiryTime = System.currentTimeMillis() + expiresIn * 1000,
            )
            // 清除加密存储中的 PKCE code_verifier，防止重放攻击
            settingsRepository.saveCloudSyncPkceCodeVerifier("")
            Timber.tag("CloudSync").i("GoogleDrive: 令牌交换成功，有效期 %ds", expiresIn)
            updatedConfig
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "GoogleDrive: 令牌交换失败")
            throw IllegalStateException("Failed to exchange authorization code: ${e.message}", e)
        }
    }

    override suspend fun ensureValidToken(config: CloudSyncConfig): CloudSyncConfig {
        if (config.accessToken.isBlank()) return config
        if (System.currentTimeMillis() < config.tokenExpiryTime - 60_000) return config

        Timber.tag("CloudSync").i("GoogleDrive: 刷新访问令牌")

        return withContext(Dispatchers.IO) {
            if (config.refreshToken.isBlank()) {
                throw IllegalStateException("Access token expired and no refresh token available")
            }

            val tokenUrl = if (config.useCustomAuthEndpoint && config.customTokenEndpoint.isNotBlank()) {
                config.customTokenEndpoint
            } else {
                DEFAULT_TOKEN_URL
            }

            val body = buildMap {
                put("client_id", config.oauthClientId)
                put("refresh_token", config.refreshToken)
                put("grant_type", "refresh_token")
            }.entries.joinToString("&") { (k, v) ->
                "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
            }

            val request = Request.Builder()
                .url(tokenUrl)
                .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .build()

            try {
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                    ?: throw IllegalStateException("Empty refresh token response")

                if (!response.isSuccessful) {
                    throw IllegalStateException("Token refresh failed: $responseBody")
                }

                val json = JSONObject(responseBody)
                val accessToken = json.getString("access_token")
                val expiresIn = json.optLong("expires_in", 3600)

                // Token 已刷新，清理文件夹 ID 缓存以防过期
                folderIdCache.clear()

                config.copy(
                    accessToken = accessToken,
                    tokenExpiryTime = System.currentTimeMillis() + expiresIn * 1000,
                )
            } catch (e: Exception) {
                Timber.tag("CloudSync").e(e, "GoogleDrive: 刷新令牌失败")
                throw IllegalStateException("Failed to refresh token: ${e.message}", e)
            }
        }
    }

    override suspend fun revokeAuthorization(config: CloudSyncConfig) {
        if (config.accessToken.isBlank()) return
        Timber.tag("CloudSync").i("GoogleDrive: 撤销授权")
        folderIdCache.clear()
        try {
            val request = okhttp3.Request.Builder()
                .url("https://oauth2.googleapis.com/revoke?token=${config.accessToken}")
                .get()
                .build()
            httpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            // 服务端撤销失败不应阻止本地清除
            Timber.tag("CloudSync").w(e, "GoogleDrive: 撤销授权失败")
        }
    }

    override suspend fun getStorageInfo(config: CloudSyncConfig): StorageInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/about?fields=storageQuota")
                .header("Authorization", "Bearer ${config.accessToken}")
                .get()
                .build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            if (!response.isSuccessful) {
                Timber.tag("CloudSync").w("GoogleDrive: 获取存储空间信息失败，HTTP %d", response.code)
                return@withContext null
            }
            val json = JSONObject(body)
            val quota = json.optJSONObject("storageQuota") ?: return@withContext null
            val limit = quota.optLong("limit", 0)
            val info = StorageInfo(
                usedBytes = quota.optLong("usageInDrive", 0),
                totalBytes = if (limit > 0) limit else 0,
            )
            Timber.tag("CloudSync").d("GoogleDrive: 存储空间 - 已用: %d, 总计: %d", info.usedBytes, info.totalBytes)
            info
        } catch (e: Exception) {
            Timber.tag("CloudSync").w(e, "GoogleDrive: 获取存储空间信息失败")
            null
        }
    }

    // ======================== Connection test ========================

    override suspend fun testConnection(config: CloudSyncConfig): ConnectionTestResult =
        withContext(Dispatchers.IO) {
            try {
                val validConfig = ensureValidToken(config)
                val request = Request.Builder()
                    .url("$BASE_URL/about?fields=user")
                    .header("Authorization", "Bearer ${validConfig.accessToken}")
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    Timber.tag("CloudSync").w("GoogleDrive: 连接测试失败，HTTP %d", response.code)
                    return@withContext ConnectionTestResult(
                        success = false,
                        message = "Connection failed: ${response.code}",
                        serverInfo = responseBody,
                    )
                }

                val json = JSONObject(responseBody ?: "{}")
                val user = json.optJSONObject("user")
                val email = user?.optString("displayName", "Unknown") ?: "Unknown"

                Timber.tag("CloudSync").i("GoogleDrive: 连接测试成功 - 用户: %s", email)
                ConnectionTestResult(
                    success = true,
                    message = "Connected to Google Drive",
                    serverInfo = "User: $email",
                )
            } catch (e: Exception) {
                Timber.tag("CloudSync").e(e, "GoogleDrive: 连接测试失败")
                ConnectionTestResult(
                    success = false,
                    message = "Connection failed: ${e.message}",
                )
            }
        }

    // ======================== List files ========================

    override suspend fun listFiles(
        config: CloudSyncConfig,
        remotePath: String,
    ): List<RemoteFileInfo> = withContext(Dispatchers.IO) {
        val validConfig = ensureValidToken(config)
        val folderId = resolveFolderId(validConfig, remotePath)
        if (folderId == null) {
            Timber.tag("CloudSync").w("GoogleDrive: 列出文件 - 无法解析文件夹：'%s'", remotePath)
            return@withContext emptyList()
        }

        val allFiles = mutableListOf<RemoteFileInfo>()
        val query = "'$folderId' in parents and trashed = false"
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        var pageToken: String? = null

        do {
            val urlBuilder = StringBuilder("$BASE_URL/files?q=$encodedQuery&fields=nextPageToken,files($FIELDS)&pageSize=1000")
            pageToken?.let { urlBuilder.append("&pageToken=$it") }

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .header("Authorization", "Bearer ${validConfig.accessToken}")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Timber.tag("CloudSync").w("GoogleDrive: 列出文件失败，HTTP %d, path=%s", response.code, remotePath)
                throw IllegalStateException("List files failed: ${response.code}")
            }

            val responseBody = response.body?.string() ?: break
            val json = JSONObject(responseBody)
            val filesArray = json.optJSONArray("files")
            if (filesArray != null) {
                for (i in 0 until filesArray.length()) {
                    val fileObj = filesArray.getJSONObject(i)
                    allFiles.add(parseRemoteFileInfo(fileObj, remotePath))
                }
            }
            pageToken = json.optString("nextPageToken", "")
            if (pageToken.isNullOrEmpty()) pageToken = null
        } while (pageToken != null)

        Timber.tag("CloudSync").d("GoogleDrive: 列出文件 '%s'，共 %d 项", remotePath, allFiles.size)
        allFiles
    }

    // ======================== Upload file ========================

    override suspend fun uploadFile(
        config: CloudSyncConfig,
        remotePath: String,
        content: ByteArray,
        lastModified: Long,
    ): Boolean = withContext(Dispatchers.IO) {
        val validConfig = ensureValidToken(config)

        val pathParts = remotePath.trim('/').split('/')
        val fileName = pathParts.last()
        val parentPath = "/" + pathParts.dropLast(1).joinToString("/")

        val parentId = resolveFolderId(validConfig, parentPath, createIfMissing = true)
        if (parentId == null) {
            Timber.tag("CloudSync").w("GoogleDrive: 上传文件 - 无法解析父文件夹：'%s'", remotePath)
            return@withContext false
        }

        // Check if file already exists -> update it
        val existingFileId = findFileIdByName(validConfig, fileName, parentId)
        val fileMetadata = JSONObject().apply {
            put("name", fileName)
            if (existingFileId == null) {
                put("parents", org.json.JSONArray().put(parentId))
                // 新建文件时设置 modifiedTime，保留本地文件的修改时间
                put("modifiedTime", java.time.Instant.ofEpochMilli(lastModified).toString())
            }
        }

        val success = if (content.size >= MULTIPART_UPLOAD_SIZE_THRESHOLD) {
            Timber.tag("CloudSync").i("GoogleDrive: 上传文件 '%s' 使用可续传上传 (%d bytes)", remotePath, content.size)
            uploadResumable(validConfig, fileMetadata, content, existingFileId)
        } else {
            uploadMultipart(validConfig, fileMetadata, content, existingFileId)
        }

        if (success) {
            Timber.tag("CloudSync").i("GoogleDrive: 上传成功 '%s' (%d bytes)", remotePath, content.size)
            if (existingFileId != null) {
                updateModifiedTime(validConfig, existingFileId, lastModified)
            }
        } else {
            Timber.tag("CloudSync").w("GoogleDrive: 上传文件失败：'%s'", remotePath)
        }

        success
    }

    // ======================== Download file ========================

    override suspend fun downloadFile(config: CloudSyncConfig, remotePath: String): ByteArray? =
        withContext(Dispatchers.IO) {
            val validConfig = ensureValidToken(config)
            val fileId = resolveFileId(validConfig, remotePath)
            if (fileId == null) {
                Timber.tag("CloudSync").w("GoogleDrive: 下载文件 - 无法解析文件：'%s'", remotePath)
                return@withContext null
            }

            val url = "$BASE_URL/files/$fileId?alt=media"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${validConfig.accessToken}")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Timber.tag("CloudSync").w("GoogleDrive: 下载文件失败，HTTP %d, path=%s", response.code, remotePath)
                return@withContext null
            }

            val bytes = response.body?.bytes()
            if (bytes != null) {
                Timber.tag("CloudSync").i("GoogleDrive: 下载成功 '%s' (%d bytes)", remotePath, bytes.size)
            }
            bytes
        }

    // ======================== Delete file ========================

    override suspend fun deleteFile(config: CloudSyncConfig, remotePath: String): Boolean =
        withContext(Dispatchers.IO) {
            val validConfig = ensureValidToken(config)
            val fileId = resolveFileId(validConfig, remotePath)
            if (fileId == null) {
                Timber.tag("CloudSync").w("GoogleDrive: 删除文件 - 无法解析文件：'%s'", remotePath)
                return@withContext false
            }

            val url = "$BASE_URL/files/$fileId"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${validConfig.accessToken}")
                .delete()
                .build()

            val success = httpClient.newCall(request).execute().isSuccessful
            if (success) {
                Timber.tag("CloudSync").i("GoogleDrive: 删除文件：$remotePath")
            } else {
                Timber.tag("CloudSync").e("GoogleDrive: 删除文件失败：$remotePath")
            }
            success
        }

    // ======================== Create directory ========================

    override suspend fun createDirectory(config: CloudSyncConfig, remotePath: String): Boolean =
        withContext(Dispatchers.IO) {
            val validConfig = ensureValidToken(config)
            val result = resolveFolderId(validConfig, remotePath, createIfMissing = true) != null
            if (result) {
                Timber.tag("CloudSync").i("GoogleDrive: 创建目录成功：$remotePath")
            } else {
                Timber.tag("CloudSync").w("GoogleDrive: 创建目录失败：$remotePath")
            }
            result
        }

    // ======================== Get file info ========================

    override suspend fun getFileInfo(config: CloudSyncConfig, remotePath: String): RemoteFileInfo? =
        withContext(Dispatchers.IO) {
            val validConfig = ensureValidToken(config)
            val fileId = resolveFileId(validConfig, remotePath)
            if (fileId == null) {
                Timber.tag("CloudSync").w("GoogleDrive: 获取文件信息 - 无法解析文件：'%s'", remotePath)
                return@withContext null
            }

            val url = "$BASE_URL/files/$fileId?fields=$FIELDS"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${validConfig.accessToken}")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Timber.tag("CloudSync").w("GoogleDrive: 获取文件信息失败，HTTP %d, path=%s", response.code, remotePath)
                return@withContext null
            }

            val responseBody = response.body?.string() ?: return@withContext null
            val json = JSONObject(responseBody)
            val parentPath = remotePath.substringBeforeLast('/').ifBlank { "/" }
            val info = parseRemoteFileInfo(json, parentPath)
            Timber.tag("CloudSync").d("GoogleDrive: 获取文件信息 '%s' -> isDir=%b, size=%d", remotePath, info.isDirectory, info.size)
            info
        }

    // ======================== Internal: PKCE helpers ========================

    private fun generateCodeVerifier(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun generateState(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun generateCodeChallenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(codeVerifier.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
    }

    // ======================== Internal: Path <-> ID resolution ========================

    /**
     * Resolve a virtual path like "/Mdcito/sub" to a Google Drive folder ID.
     * If [createIfMissing] is true, missing folders are created along the way.
     */
    private suspend fun resolveFolderId(
        config: CloudSyncConfig,
        path: String,
        createIfMissing: Boolean = false,
    ): String? {
        val normalizedPath = path.trimEnd('/')
        if (normalizedPath.isBlank() || normalizedPath == "/") {
            return "root"
        }

        folderIdCache[normalizedPath]?.let { return it }

        val parts = normalizedPath.trim('/').split('/').filter { it.isNotBlank() }
        var currentParentId = "root"
        var currentPath = ""

        for (part in parts) {
            currentPath = "$currentPath/$part"
            folderIdCache[currentPath]?.let {
                currentParentId = it
                continue
            }

            val foundId = findFolderIdByName(config, part, currentParentId)
            if (foundId != null) {
                Timber.tag("CloudSync").d("GoogleDrive: 解析文件夹 '%s' -> %s", currentPath, foundId)
                folderIdCache[currentPath] = foundId
                currentParentId = foundId
            } else if (createIfMissing) {
                val createdId = createFolder(config, part, currentParentId)
                if (createdId != null) {
                    Timber.tag("CloudSync").i("GoogleDrive: 创建文件夹 '%s' -> %s", currentPath, createdId)
                    folderIdCache[currentPath] = createdId
                    currentParentId = createdId
                } else {
                    Timber.tag("CloudSync").e("GoogleDrive: 创建文件夹失败：'%s'", currentPath)
                    return null
                }
            } else {
                return null
            }
        }

        return currentParentId
    }

    /**
     * Resolve a file path to its Google Drive file ID.
     */
    private suspend fun resolveFileId(config: CloudSyncConfig, remotePath: String): String? {
        val normalizedPath = remotePath.trimEnd('/')
        val pathParts = normalizedPath.trim('/').split('/')
        if (pathParts.isEmpty()) return null

        val fileName = pathParts.last()
        val parentPath = if (pathParts.size > 1) "/" + pathParts.dropLast(1).joinToString("/") else "/"

        val parentId = resolveFolderId(config, parentPath) ?: return null
        return findFileIdByName(config, fileName, parentId)
    }

    /**
     * Search for a folder by name within a parent.
     */
    private fun findFolderIdByName(
        config: CloudSyncConfig,
        name: String,
        parentId: String,
    ): String? {
        val escapedName = name.replace("\\", "\\\\").replace("'", "\\'")
        val query = "name='$escapedName' and '$parentId' in parents and mimeType='$FOLDER_MIME_TYPE' and trashed = false"
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$BASE_URL/files?q=$encodedQuery&fields=files(id)&pageSize=1"

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${config.accessToken}")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null

        val responseBody = response.body?.string() ?: return null
        val json = JSONObject(responseBody)
        val files = json.optJSONArray("files") ?: return null
        if (files.length() == 0) return null
        return files.getJSONObject(0).getString("id")
    }

    /**
     * Search for a file by name within a parent folder.
     */
    private fun findFileIdByName(
        config: CloudSyncConfig,
        name: String,
        parentId: String,
    ): String? {
        val escapedName = name.replace("\\", "\\\\").replace("'", "\\'")
        val query = "name='$escapedName' and '$parentId' in parents and trashed = false"
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$BASE_URL/files?q=$encodedQuery&fields=files(id)&pageSize=1"

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${config.accessToken}")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null

        val responseBody = response.body?.string() ?: return null
        val json = JSONObject(responseBody)
        val files = json.optJSONArray("files") ?: return null
        if (files.length() == 0) return null
        return files.getJSONObject(0).getString("id")
    }

    /**
     * Create a folder in Google Drive.
     */
    private fun createFolder(config: CloudSyncConfig, name: String, parentId: String): String? {
        val metadata = JSONObject().apply {
            put("name", name)
            put("mimeType", FOLDER_MIME_TYPE)
            put("parents", org.json.JSONArray().put(parentId))
        }

        val request = Request.Builder()
            .url("$BASE_URL/files?fields=id")
            .header("Authorization", "Bearer ${config.accessToken}")
            .header("Content-Type", "application/json; charset=utf-8")
            .post(metadata.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Timber.tag("CloudSync").w("GoogleDrive: 创建文件夹 API 失败，HTTP %d, name=%s", response.code, name)
            return null
        }

        val responseBody = response.body?.string() ?: return null
        val id = JSONObject(responseBody).getString("id")
        Timber.tag("CloudSync").d("GoogleDrive: 创建文件夹 API 成功，name=%s, id=%s", name, id)
        return id
    }

    // ======================== Internal: Upload helpers ========================

    /**
     * Multipart upload for files < 5MB.
     */
    private fun uploadMultipart(
        config: CloudSyncConfig,
        metadata: JSONObject,
        content: ByteArray,
        existingFileId: String?,
    ): Boolean {
        val boundary = "mdcito_boundary_${System.currentTimeMillis()}"
        val metadataContentType = "application/json; charset=UTF-8"
        val mediaContentType = "application/octet-stream"

        val baos = ByteArrayOutputStream()
        fun writePart(contentType: String, data: ByteArray) {
            baos.write("--$boundary\r\n".toByteArray())
            baos.write("Content-Type: $contentType\r\n\r\n".toByteArray())
            baos.write(data)
            baos.write("\r\n".toByteArray())
        }

        if (existingFileId != null) {
            // PATCH for update
            writePart(metadataContentType, metadata.toString().toByteArray())
            baos.write("--$boundary\r\n".toByteArray())
            baos.write("Content-Type: $mediaContentType\r\n\r\n".toByteArray())
            baos.write(content)
            baos.write("\r\n--$boundary--\r\n".toByteArray())

            val url = "$UPLOAD_BASE_URL/files/$existingFileId?uploadType=multipart&fields=id"
            val requestBody = baos.toByteArray()
                .toRequestBody("multipart/related; boundary=$boundary".toMediaType())

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${config.accessToken}")
                .patch(requestBody)
                .build()

            val result = httpClient.newCall(request).execute().isSuccessful
            if (!result) {
                Timber.tag("CloudSync").w("GoogleDrive: multipart 更新文件失败，fileId=%s", existingFileId)
            }
            return result
        } else {
            // POST for create
            writePart(metadataContentType, metadata.toString().toByteArray())
            baos.write("--$boundary\r\n".toByteArray())
            baos.write("Content-Type: $mediaContentType\r\n\r\n".toByteArray())
            baos.write(content)
            baos.write("\r\n--$boundary--\r\n".toByteArray())

            val url = "$UPLOAD_BASE_URL/files?uploadType=multipart&fields=id"
            val requestBody = baos.toByteArray()
                .toRequestBody("multipart/related; boundary=$boundary".toMediaType())

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${config.accessToken}")
                .post(requestBody)
                .build()

            val result = httpClient.newCall(request).execute().isSuccessful
            if (!result) {
                Timber.tag("CloudSync").w("GoogleDrive: multipart 创建文件失败")
            }
            return result
        }
    }

    /**
     * Resumable upload for files >= 5MB.
     */
    private fun uploadResumable(
        config: CloudSyncConfig,
        metadata: JSONObject,
        content: ByteArray,
        existingFileId: String?,
    ): Boolean {
        // Step 1: Initiate resumable session
        val initiateUrl = if (existingFileId != null) {
            "$UPLOAD_BASE_URL/files/$existingFileId?uploadType=resumable"
        } else {
            "$UPLOAD_BASE_URL/files?uploadType=resumable"
        }

        val initiateRequest = Request.Builder()
            .url(initiateUrl)
            .header("Authorization", "Bearer ${config.accessToken}")
            .header("Content-Type", "application/json; charset=UTF-8")
            .header("X-Upload-Content-Length", content.size.toString())
            .apply {
                if (existingFileId != null) {
                    patch(metadata.toString().toRequestBody("application/json".toMediaType()))
                } else {
                    post(metadata.toString().toRequestBody("application/json".toMediaType()))
                }
            }
            .build()

        val initiateResponse = httpClient.newCall(initiateRequest).execute()
        if (!initiateResponse.isSuccessful) {
            Timber.tag("CloudSync").w("GoogleDrive: 可续传上传初始化失败，HTTP %d", initiateResponse.code)
            return false
        }

        val uploadUrl = initiateResponse.header("Location") ?: run {
            Timber.tag("CloudSync").e("GoogleDrive: 可续传上传初始化成功但未返回 Location")
            return false
        }

        // Step 2: Upload the content
        val uploadRequest = Request.Builder()
            .url(uploadUrl)
            .header("Content-Length", content.size.toString())
            .put(content.toRequestBody("application/octet-stream".toMediaType()))
            .build()

        val result = httpClient.newCall(uploadRequest).execute().isSuccessful
        if (!result) {
            Timber.tag("CloudSync").w("GoogleDrive: 可续传上传内容失败，size=%d", content.size)
        }
        return result
    }

    /**
     * Update the modifiedTime of an existing file.
     */
    private fun updateModifiedTime(config: CloudSyncConfig, fileId: String, lastModified: Long): Boolean {
        val isoTime = java.time.Instant.ofEpochMilli(lastModified)
            .toString()

        val metadata = JSONObject().apply {
            put("modifiedTime", isoTime)
        }

        val request = Request.Builder()
            .url("$BASE_URL/files/$fileId?fields=id")
            .header("Authorization", "Bearer ${config.accessToken}")
            .header("Content-Type", "application/json; charset=UTF-8")
            .patch(metadata.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return httpClient.newCall(request).execute().isSuccessful
    }

    // ======================== Internal: Parsing ========================

    private fun parseRemoteFileInfo(json: JSONObject, parentPath: String): RemoteFileInfo {
        val name = json.getString("name")
        val mimeType = json.optString("mimeType", "")
        val isDirectory = mimeType == FOLDER_MIME_TYPE
        val size = json.optLong("size", 0L)
        val modifiedTimeStr = json.optString("modifiedTime", "")
        val lastModified = try {
            java.time.Instant.parse(modifiedTimeStr).toEpochMilli()
        } catch (e: Exception) {
            Timber.tag("CloudSync").w(e, "GoogleDrive: 解析修改时间失败：%s", modifiedTimeStr)
            0L
        }
        val md5 = json.optString("md5Checksum", "")
        val path = if (parentPath.endsWith('/')) "$parentPath$name" else "$parentPath/$name"

        return RemoteFileInfo(
            path = path,
            name = name,
            isDirectory = isDirectory,
            size = size,
            lastModified = lastModified,
            etag = md5,
        )
    }
}
