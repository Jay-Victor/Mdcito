package com.mdcito.app.data.image

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.mdcito.app.data.datastore.SecureSettingsDataStore
import com.mdcito.app.data.model.ImageHostProfile
import com.mdcito.app.data.model.ImageProcessingSettings
import com.mdcito.app.data.model.ProviderFieldDefs
import com.mdcito.app.data.model.TestResult
import com.mdcito.app.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import com.mdcito.app.R
import timber.log.Timber

/**
 * 上传结果数据类
 */
data class UploadResult(
    val success: Boolean,
    val url: String = "",
    val message: String = "",
    val deleteUrl: String = "",
)

/**
 * 图片上传器接口（策略模式）
 */
interface ImageUploader {
    suspend fun upload(imageData: ByteArray, fileName: String, config: Map<String, String>): UploadResult
    suspend fun testConnection(config: Map<String, String>): TestResult
}

/**
 * 图片上传服务
 * 使用策略模式支持多种图床供应商
 * 负责协调图片处理、配置合并和上传调度
 */
@Singleton
class ImageUploadService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val secureSettingsDataStore: SecureSettingsDataStore,
    private val imageProcessor: ImageProcessor,
) {

    // OkHttp 客户端，30s 连接超时，60s 读取超时
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val gson = Gson()

    /**
     * 上传图片文件
     * 1. 获取当前激活的图床方案
     * 2. 获取图片处理设置
     * 3. 处理图片（压缩/缩放/水印/重命名）
     * 4. 合并非敏感配置和敏感配置
     * 5. 获取对应供应商的上传器并执行上传
     */
    suspend fun uploadImage(imageFile: File, fileName: String): UploadResult = withContext(Dispatchers.IO) {
        try {
            // 获取当前激活的图床方案
            val profiles = settingsRepository.imageHostProfiles.first()
            val activeProfileId = settingsRepository.imageHostActiveProfileId.first()
            val profile = profiles.find { it.id == activeProfileId }
            if (profile == null) {
                Timber.tag("ImageUpload").w("No active image host profile found, fileName=%s", fileName)
                return@withContext UploadResult(success = false, message = context.getString(R.string.upload_no_active_profile))
            }
            Timber.tag("ImageUpload").i("Uploading image: fileName=%s, provider=%s", fileName, profile.provider)

            // 获取图片处理设置
            val processingSettings = settingsRepository.imageProcessingSettings.first()

            // 处理图片
            val processed = imageProcessor.process(imageFile, fileName, processingSettings)
            Timber.tag("ImageUpload").d("Image processed: outputSize=%d bytes, outputFileName=%s", processed.imageData.size, processed.fileName)

            // 合并非敏感配置和敏感配置
            val mergedConfig = mergeConfig(profile)

            // 获取对应供应商的上传器并上传
            val uploader = getUploader(profile.provider)
            val result = uploader.upload(processed.imageData, processed.fileName, mergedConfig)
            if (result.success) {
                Timber.tag("ImageUpload").i("Upload succeeded: fileName=%s, url=%s", processed.fileName, result.url)
            } else {
                Timber.tag("ImageUpload").w("Upload failed: fileName=%s, message=%s", processed.fileName, result.message)
            }
            result
        } catch (e: Exception) {
            Timber.tag("ImageUpload").e(e, "Upload exception: fileName=%s", fileName)
            UploadResult(success = false, message = context.getString(R.string.upload_failed_with_error, e.message ?: ""))
        }
    }

    /**
     * 测试图床连接
     * @param provider 供应商类型
     * @param config 配置参数（已合并敏感字段）
     */
    suspend fun testConnection(provider: String, config: Map<String, String>): TestResult = withContext(Dispatchers.IO) {
        try {
            Timber.tag("ImageUpload").i("Testing connection: provider=%s", provider)
            val uploader = getUploader(provider)
            val result = uploader.testConnection(config)
            if (result.success) {
                Timber.tag("ImageUpload").i("Connection test succeeded: provider=%s", provider)
            } else {
                Timber.tag("ImageUpload").w("Connection test failed: provider=%s, message=%s", provider, result.message)
            }
            result
        } catch (e: Exception) {
            Timber.tag("ImageUpload").e(e, "Connection test exception: provider=%s", provider)
            TestResult(success = false, message = context.getString(R.string.test_failed_with_error, e.message ?: ""))
        }
    }

    /**
     * 合并方案配置：非敏感字段来自 profile.config，敏感字段来自加密存储
     */
    private fun mergeConfig(profile: ImageHostProfile): Map<String, String> {
        val fields = ProviderFieldDefs.getFields(profile.provider)
        val secretKeys = fields.filter { it.isSecret }.map { it.key }.toSet()

        // 从加密存储读取敏感字段
        val secrets = secureSettingsDataStore.getAllSecrets(profile.id, secretKeys.toList())
        Timber.tag("ImageUpload").d("Merging config for profile=%s, provider=%s, secretKeys=%s", profile.id, profile.provider, secretKeys)

        // 合并非敏感配置和敏感配置
        val merged = profile.config.toMutableMap()
        merged.putAll(secrets)
        return merged
    }

    /**
     * 根据供应商类型获取对应的上传器
     */
    private fun getUploader(provider: String): ImageUploader = when (provider) {
        "github" -> GithubUploader()
        "smms" -> SmmsUploader()
        "imgur" -> ImgurUploader()
        "qiniu" -> QiniuUploader()
        "aliyun" -> AliyunUploader()
        "tencent" -> TencentUploader()
        "custom" -> CustomUploader()
        else -> throw IllegalArgumentException(context.getString(R.string.upload_unsupported_provider, provider))
    }

    // ── GitHub 图床上传器 ──

    /**
     * GitHub 图床上传器
     * 通过 GitHub Contents API 上传图片到仓库
     * 支持自定义域名替换
     */
    inner class GithubUploader : ImageUploader {

        override suspend fun upload(imageData: ByteArray, fileName: String, config: Map<String, String>): UploadResult =
            withContext(Dispatchers.IO) {
                try {
                    val repo = config["repo"] ?: return@withContext UploadResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.upload_field_github_repo)))
                    val branch = config["branch"] ?: "main"
                    val token = config["token"] ?: return@withContext UploadResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.upload_field_github_token)))
                    val path = config["path"] ?: "images"

                    // 构建 Base64 编码的图片数据
                    val base64Content = Base64.encodeToString(imageData, Base64.NO_WRAP)

                    // 构建请求体
                    val requestBody = gson.toJson(
                        mapOf(
                            "message" to "Upload image",
                            "content" to base64Content,
                            "branch" to branch,
                        )
                    ).toRequestBody("application/json".toMediaType())

                    // 构建请求
                    val url = "https://api.github.com/repos/$repo/contents/$path/$fileName"
                    val request = Request.Builder()
                        .url(url)
                        .put(requestBody)
                        .addHeader("Authorization", "Bearer $token")
                        .addHeader("Accept", "application/vnd.github.v3+json")
                        .build()

                    // 执行请求
                    val response = httpClient.newCall(request).execute()
                    response.use {
                        if (!response.isSuccessful) {
                            val errorBody = response.body?.string() ?: ""
                            Timber.tag("ImageUpload").e("GitHub upload HTTP error: code=%d, body=%s", response.code, errorBody)
                            return@withContext UploadResult(
                                success = false,
                                message = context.getString(R.string.upload_github_failed, response.code.toString(), errorBody),
                            )
                        }

                        // 解析响应，获取下载链接
                        val responseBody = response.body?.string() ?: return@withContext UploadResult(
                            success = false,
                            message = context.getString(R.string.upload_response_empty, "GitHub"),
                        )
                        val json = JsonParser.parseString(responseBody).asJsonObject
                        var downloadUrl = json.getAsJsonObject("content")?.get("download_url")?.asString
                            ?: return@withContext UploadResult(success = false, message = context.getString(R.string.upload_cannot_get_download_url))

                        // 如果配置了自定义域名，替换 GitHub 原始链接
                        val customDomain = config["domain"]
                        if (!customDomain.isNullOrBlank() && customDomain.startsWith("http")) {
                            // raw.githubusercontent.com/user/repo/branch/path/file -> customDomain/path/file
                            val rawPattern = Regex("""https://raw\.githubusercontent\.com/[^/]+/[^/]+/[^/]+/""")
                            if (rawPattern.containsMatchIn(downloadUrl)) {
                                downloadUrl = rawPattern.replace(downloadUrl, "$customDomain/")
                            }
                        }

                        Timber.tag("ImageUpload").i("GitHub upload succeeded: fileName=%s, url=%s", fileName, downloadUrl)
                        UploadResult(success = true, url = downloadUrl)
                    }
                } catch (e: Exception) {
                    Timber.tag("ImageUpload").e(e, "GitHub upload exception: fileName=%s", fileName)
                    UploadResult(success = false, message = context.getString(R.string.upload_exception, "GitHub", e.message ?: ""))
                }
            }

        override suspend fun testConnection(config: Map<String, String>): TestResult = withContext(Dispatchers.IO) {
            try {
                val repo = config["repo"] ?: return@withContext TestResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.field_repo_name)))
                val token = config["token"] ?: return@withContext TestResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.field_token)))

                val request = Request.Builder()
                    .url("https://api.github.com/repos/$repo")
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        TestResult(success = true, message = context.getString(R.string.upload_connection_success, "GitHub"))
                    } else {
                        Timber.tag("ImageUpload").e("GitHub connection test failed: code=%d", response.code)
                        TestResult(success = false, message = context.getString(R.string.upload_auth_failed, "GitHub", response.code))
                    }
                }
            } catch (e: Exception) {
                Timber.tag("ImageUpload").e(e, "GitHub connection test exception")
                TestResult(success = false, message = context.getString(R.string.upload_connection_exception, "GitHub", e.message ?: ""))
            }
        }
    }

    // ── SM.MS 图床上传器 ──

    /**
     * SM.MS 图床上传器
     * 通过 SM.MS API v2 上传图片
     */
    inner class SmmsUploader : ImageUploader {

        override suspend fun upload(imageData: ByteArray, fileName: String, config: Map<String, String>): UploadResult =
            withContext(Dispatchers.IO) {
                try {
                    val token = config["token"] ?: return@withContext UploadResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.upload_field_smms_token)))

                    // 构建 Multipart 请求体
                    val fileBody = imageData.toRequestBody("image/*".toMediaType())
                    val multipartBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", fileName, fileBody)
                        .build()

                    val request = Request.Builder()
                        .url("https://sm.ms/api/v2/upload")
                        .post(multipartBody)
                        .addHeader("Authorization", "Bearer $token")
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            Timber.tag("ImageUpload").e("SM.MS upload HTTP error: code=%d", response.code)
                        }
                        val responseBody = response.body?.string() ?: return@withContext UploadResult(
                            success = false,
                            message = context.getString(R.string.upload_response_empty, context.getString(R.string.upload_provider_smms)),
                        )

                        val json = JsonParser.parseString(responseBody).asJsonObject
                        val success = json.get("success")?.asBoolean ?: false
                        if (!success) {
                            val message = json.get("message")?.asString ?: context.getString(R.string.upload_failed_generic)
                            Timber.tag("ImageUpload").w("SM.MS upload API failure: %s", message)
                            return@withContext UploadResult(success = false, message = context.getString(R.string.upload_smms_failed, message))
                        }

                        val data = json.getAsJsonObject("data")
                        val url = data?.get("url")?.asString ?: ""
                        val deleteUrl = data?.get("delete")?.asString ?: ""

                        Timber.tag("ImageUpload").i("SM.MS upload succeeded: fileName=%s, url=%s", fileName, url)
                        UploadResult(success = true, url = url, deleteUrl = deleteUrl)
                    }
                } catch (e: Exception) {
                    Timber.tag("ImageUpload").e(e, "SM.MS upload exception: fileName=%s", fileName)
                    UploadResult(success = false, message = context.getString(R.string.upload_exception, context.getString(R.string.upload_provider_smms), e.message ?: ""))
                }
            }

        override suspend fun testConnection(config: Map<String, String>): TestResult = withContext(Dispatchers.IO) {
            try {
                val token = config["token"] ?: return@withContext TestResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.field_token)))

                val request = Request.Builder()
                    .url("https://sm.ms/api/v2/profile")
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        TestResult(success = true, message = context.getString(R.string.upload_connection_success, context.getString(R.string.upload_provider_smms)))
                    } else {
                        Timber.tag("ImageUpload").e("SM.MS connection test failed: code=%d", response.code)
                        TestResult(success = false, message = context.getString(R.string.upload_auth_failed, context.getString(R.string.upload_provider_smms), response.code))
                    }
                }
            } catch (e: Exception) {
                Timber.tag("ImageUpload").e(e, "SM.MS connection test exception")
                TestResult(success = false, message = context.getString(R.string.upload_connection_exception, context.getString(R.string.upload_provider_smms), e.message ?: ""))
            }
        }
    }

    // ── Imgur 图床上传器 ──

    /**
     * Imgur 图床上传器
     * 通过 Imgur API v3 上传图片
     */
    inner class ImgurUploader : ImageUploader {

        override suspend fun upload(imageData: ByteArray, fileName: String, config: Map<String, String>): UploadResult =
            withContext(Dispatchers.IO) {
                try {
                    val clientId = config["clientId"]
                        ?: return@withContext UploadResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.upload_field_imgur_client_id)))

                    // 将图片转为 Base64
                    val base64Image = Base64.encodeToString(imageData, Base64.NO_WRAP)

                    val requestBody = base64Image.toRequestBody("text/plain".toMediaType())

                    val request = Request.Builder()
                        .url("https://api.imgur.com/3/image")
                        .post(requestBody)
                        .addHeader("Authorization", "Client-ID $clientId")
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            Timber.tag("ImageUpload").e("Imgur upload HTTP error: code=%d", response.code)
                        }
                        val responseBody = response.body?.string() ?: return@withContext UploadResult(
                            success = false,
                            message = context.getString(R.string.upload_response_empty, "Imgur"),
                        )

                        val json = JsonParser.parseString(responseBody).asJsonObject
                        val success = json.get("success")?.asBoolean ?: false
                        if (!success) {
                            val status = json.get("status")?.asInt ?: -1
                            Timber.tag("ImageUpload").w("Imgur upload API failure: status=%d", status)
                            return@withContext UploadResult(success = false, message = context.getString(R.string.upload_imgur_failed, status))
                        }

                        val data = json.getAsJsonObject("data")
                        val link = data?.get("link")?.asString ?: ""
                        val deleteHash = data?.get("deletehash")?.asString ?: ""

                        Timber.tag("ImageUpload").i("Imgur upload succeeded: fileName=%s, url=%s", fileName, link)
                        UploadResult(success = true, url = link, deleteUrl = deleteHash)
                    }
                } catch (e: Exception) {
                    Timber.tag("ImageUpload").e(e, "Imgur upload exception: fileName=%s", fileName)
                    UploadResult(success = false, message = context.getString(R.string.upload_exception, "Imgur", e.message ?: ""))
                }
            }

        override suspend fun testConnection(config: Map<String, String>): TestResult = withContext(Dispatchers.IO) {
            try {
                val clientId = config["clientId"]
                    ?: return@withContext TestResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.upload_field_imgur_client_id)))

                val request = Request.Builder()
                    .url("https://api.imgur.com/3/credits")
                    .addHeader("Authorization", "Client-ID $clientId")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        TestResult(success = true, message = context.getString(R.string.upload_connection_success, "Imgur"))
                    } else {
                        Timber.tag("ImageUpload").e("Imgur connection test failed: code=%d", response.code)
                        TestResult(success = false, message = context.getString(R.string.upload_auth_failed, "Imgur", response.code))
                    }
                }
            } catch (e: Exception) {
                Timber.tag("ImageUpload").e(e, "Imgur connection test exception")
                TestResult(success = false, message = context.getString(R.string.upload_connection_exception, "Imgur", e.message ?: ""))
            }
        }
    }

    // ── 七牛云图床上传器 ──

    /**
     * 七牛云图床上传器
     * 使用 HMAC-SHA1 生成上传凭证，通过 multipart 上传
     * 支持多区域上传
     */
    inner class QiniuUploader : ImageUploader {

        /** 根据区域代码获取上传地址 */
        private fun getUploadHost(region: String): String = when (region) {
            "z0" -> "https://up.qiniup.com"       // 华东
            "z1" -> "https://up-z1.qiniup.com"     // 华北
            "z2" -> "https://up-z2.qiniup.com"     // 华南
            "na0" -> "https://up-na0.qiniup.com"   // 北美
            "as0" -> "https://up-as0.qiniup.com"   // 东南亚
            else -> "https://up.qiniup.com"        // 默认华东
        }

        override suspend fun upload(imageData: ByteArray, fileName: String, config: Map<String, String>): UploadResult =
            withContext(Dispatchers.IO) {
                try {
                    val ak = config["ak"] ?: return@withContext UploadResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.field_access_key)))
                    val sk = config["sk"] ?: return@withContext UploadResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.field_secret_key)))
                    val bucket = config["bucket"] ?: return@withContext UploadResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.field_bucket)))
                    val domain = config["domain"] ?: return@withContext UploadResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.upload_field_domain)))
                    val region = config["region"] ?: "z0"

                    // 生成上传凭证
                    val uploadToken = generateQiniuToken(ak, sk, bucket)

                    // 构建 Multipart 请求体
                    val fileBody = imageData.toRequestBody("image/*".toMediaType())
                    val multipartBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("token", uploadToken)
                        .addFormDataPart("key", fileName)
                        .addFormDataPart("file", fileName, fileBody)
                        .build()

                    val uploadHost = getUploadHost(region)
                    val request = Request.Builder()
                        .url(uploadHost)
                        .post(multipartBody)
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            val errorBody = response.body?.string() ?: ""
                            Timber.tag("ImageUpload").e("Qiniu upload HTTP error: code=%d, body=%s", response.code, errorBody)
                            return@withContext UploadResult(
                                success = false,
                                message = context.getString(R.string.upload_aliyun_failed, response.code),
                            )
                        }
                        val responseBody = response.body?.string() ?: return@withContext UploadResult(
                            success = false,
                            message = context.getString(R.string.upload_response_empty, context.getString(R.string.provider_qiniu)),
                        )

                        val json = JsonParser.parseString(responseBody).asJsonObject
                        val key = json.get("key")?.asString ?: fileName

                        // 拼接最终 URL
                        val finalDomain = if (domain.endsWith("/")) domain else "$domain/"
                        val url = "$finalDomain$key"

                        Timber.tag("ImageUpload").i("Qiniu upload succeeded: fileName=%s, url=%s", fileName, url)
                        UploadResult(success = true, url = url)
                    }
                } catch (e: Exception) {
                    Timber.tag("ImageUpload").e(e, "Qiniu upload exception: fileName=%s", fileName)
                    UploadResult(success = false, message = context.getString(R.string.upload_exception, context.getString(R.string.provider_qiniu), e.message ?: ""))
                }
            }

        override suspend fun testConnection(config: Map<String, String>): TestResult {
            val ak = config["ak"]
            val sk = config["sk"]
            val bucket = config["bucket"]
            val domain = config["domain"]
            val missing = mutableListOf<String>()
            if (ak.isNullOrBlank()) missing.add(context.getString(R.string.field_access_key))
            if (sk.isNullOrBlank()) missing.add(context.getString(R.string.field_secret_key))
            if (bucket.isNullOrBlank()) missing.add(context.getString(R.string.field_bucket))
            if (domain.isNullOrBlank()) missing.add(context.getString(R.string.upload_field_domain))
            return if (missing.isEmpty()) {
                TestResult(success = true, message = context.getString(R.string.upload_config_validated, context.getString(R.string.provider_qiniu)))
            } else {
                TestResult(success = false, message = context.getString(R.string.upload_missing_required_fields, missing.joinToString()))
            }
        }

        /**
         * 生成七牛云上传凭证
         * 使用 HMAC-SHA1 签名
         */
        private fun generateQiniuToken(ak: String, sk: String, bucket: String): String {
            // 构建上传策略
            val deadline = (System.currentTimeMillis() / 1000) + 3600 // 1小时有效期
            val putPolicy = """{"scope":"$bucket","deadline":$deadline}"""

            // URL 安全的 Base64 编码上传策略
            val encodedPolicy = base64UrlEncode(putPolicy.toByteArray())

            // HMAC-SHA1 签名
            val sign = hmacSha1(sk.toByteArray(), encodedPolicy.toByteArray())
            val encodedSign = base64UrlEncode(sign)

            // 拼接上传凭证: AccessKey:EncodedSign:EncodedPolicy
            return "$ak:$encodedSign:$encodedPolicy"
        }

        /**
         * URL 安全的 Base64 编码
         */
        private fun base64UrlEncode(data: ByteArray): String {
            return Base64.encodeToString(data, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        }
    }

    // ── 阿里云 OSS 图床上传器 ──

    /**
     * 阿里云 OSS 图床上传器
     * 使用 OSS V1 签名通过 PUT 请求上传图片
     */
    inner class AliyunUploader : ImageUploader {

        override suspend fun upload(imageData: ByteArray, fileName: String, config: Map<String, String>): UploadResult =
            withContext(Dispatchers.IO) {
                try {
                    val akId = config["akId"] ?: return@withContext UploadResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.field_access_key_id)))
                    val akSecret = config["akSecret"] ?: return@withContext UploadResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.field_access_key_secret)))
                    val bucket = config["bucket"] ?: return@withContext UploadResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.field_bucket)))
                    val region = config["region"] ?: return@withContext UploadResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.field_region)))
                    val domain = config["domain"]

                    val objectKey = fileName
                    val host = "$bucket.$region.aliyuncs.com"
                    val date = gmtDateString()

                    // 构建 V1 签名
                    val contentType = guessContentType(fileName)
                    val resource = "/$bucket/$objectKey"
                    val stringToSign = "PUT\n\n$contentType\n$date\n$resource"
                    val signature = try {
                        val sign = hmacSha1(akSecret.toByteArray(), stringToSign.toByteArray())
                        Base64.encodeToString(sign, Base64.NO_WRAP)
                    } catch (e: Exception) {
                        Timber.tag("ImageUpload").e(e, "Aliyun OSS sign error")
                        return@withContext UploadResult(success = false, message = context.getString(R.string.upload_aliyun_sign_error, e.message ?: ""))
                    }

                    val request = Request.Builder()
                        .url("https://$host/$objectKey")
                        .put(imageData.toRequestBody(contentType.toMediaType()))
                        .addHeader("Host", host)
                        .addHeader("Date", date)
                        .addHeader("Content-Type", contentType)
                        .addHeader("Authorization", "OSS $akId:$signature")
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            val errorBody = response.body?.string() ?: ""
                            Timber.tag("ImageUpload").e("Aliyun OSS upload HTTP error: code=%d, body=%s", response.code, errorBody)
                            return@withContext UploadResult(
                                success = false,
                                message = context.getString(R.string.upload_aliyun_failed, response.code),
                            )
                        }

                        val finalDomain = if (!domain.isNullOrBlank()) {
                            if (domain.endsWith("/")) domain else "$domain/"
                        } else {
                            "https://$host/"
                        }
                        val url = "$finalDomain$objectKey"

                        Timber.tag("ImageUpload").i("Aliyun OSS upload succeeded: fileName=%s, url=%s", fileName, url)
                        UploadResult(success = true, url = url)
                    }
                } catch (e: Exception) {
                    Timber.tag("ImageUpload").e(e, "Aliyun OSS upload exception: fileName=%s", fileName)
                    UploadResult(success = false, message = context.getString(R.string.upload_exception, context.getString(R.string.provider_aliyun), e.message ?: ""))
                }
            }

        override suspend fun testConnection(config: Map<String, String>): TestResult = withContext(Dispatchers.IO) {
            try {
                val akId = config["akId"] ?: return@withContext TestResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.field_access_key_id)))
                val akSecret = config["akSecret"] ?: return@withContext TestResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.field_access_key_secret)))
                val bucket = config["bucket"] ?: return@withContext TestResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.field_bucket)))
                val region = config["region"] ?: return@withContext TestResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.field_region)))

                val host = "$bucket.$region.aliyuncs.com"
                val date = gmtDateString()
                val resource = "/$bucket/"
                val stringToSign = "GET\n\n\n$date\n$resource"
                val signature = try {
                    val sign = hmacSha1(akSecret.toByteArray(), stringToSign.toByteArray())
                    Base64.encodeToString(sign, Base64.NO_WRAP)
                } catch (e: Exception) {
                    return@withContext TestResult(success = false, message = context.getString(R.string.upload_aliyun_sign_error, e.message ?: ""))
                }

                val request = Request.Builder()
                    .url("https://$host/")
                    .get()
                    .addHeader("Host", host)
                    .addHeader("Date", date)
                    .addHeader("Authorization", "OSS $akId:$signature")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        TestResult(success = true, message = context.getString(R.string.upload_connection_success, context.getString(R.string.provider_aliyun)))
                    } else {
                        Timber.tag("ImageUpload").e("Aliyun OSS test failed: code=%d", response.code)
                        TestResult(success = false, message = context.getString(R.string.upload_auth_failed, context.getString(R.string.provider_aliyun), response.code))
                    }
                }
            } catch (e: Exception) {
                Timber.tag("ImageUpload").e(e, "Aliyun OSS test exception")
                TestResult(success = false, message = context.getString(R.string.upload_connection_exception, context.getString(R.string.provider_aliyun), e.message ?: ""))
            }
        }
    }

    // ── 腾讯云 COS 图床上传器 ──

    /**
     * 腾讯云 COS 图床上传器
     * 使用 COS 签名 v1 通过 PUT 请求上传图片
     */
    inner class TencentUploader : ImageUploader {

        override suspend fun upload(imageData: ByteArray, fileName: String, config: Map<String, String>): UploadResult =
            withContext(Dispatchers.IO) {
                try {
                    val secretId = config["secretId"] ?: return@withContext UploadResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.upload_field_secret_id)))
                    val secretKey = config["secretKey"] ?: return@withContext UploadResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.upload_field_secret_key)))
                    val bucket = config["bucket"] ?: return@withContext UploadResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.field_bucket)))
                    val region = config["region"] ?: return@withContext UploadResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.field_region)))
                    val domain = config["domain"]

                    val objectKey = fileName
                    val host = "$bucket.cos.$region.myqcloud.com"
                    val contentType = guessContentType(fileName)

                    // 构建 COS 签名
                    val authorization = try {
                        generateCosAuthorization(secretId, secretKey, host, "PUT", "/$objectKey", contentType)
                    } catch (e: Exception) {
                        Timber.tag("ImageUpload").e(e, "Tencent COS sign error")
                        return@withContext UploadResult(success = false, message = context.getString(R.string.upload_tencent_sign_error, e.message ?: ""))
                    }

                    val request = Request.Builder()
                        .url("https://$host/$objectKey")
                        .put(imageData.toRequestBody(contentType.toMediaType()))
                        .addHeader("Host", host)
                        .addHeader("Content-Type", contentType)
                        .addHeader("Authorization", authorization)
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            val errorBody = response.body?.string() ?: ""
                            Timber.tag("ImageUpload").e("Tencent COS upload HTTP error: code=%d, body=%s", response.code, errorBody)
                            return@withContext UploadResult(
                                success = false,
                                message = context.getString(R.string.upload_tencent_failed, response.code),
                            )
                        }

                        val finalDomain = if (!domain.isNullOrBlank()) {
                            if (domain.endsWith("/")) domain else "$domain/"
                        } else {
                            "https://$host/"
                        }
                        val url = "$finalDomain$objectKey"

                        Timber.tag("ImageUpload").i("Tencent COS upload succeeded: fileName=%s, url=%s", fileName, url)
                        UploadResult(success = true, url = url)
                    }
                } catch (e: Exception) {
                    Timber.tag("ImageUpload").e(e, "Tencent COS upload exception: fileName=%s", fileName)
                    UploadResult(success = false, message = context.getString(R.string.upload_exception, context.getString(R.string.provider_tencent), e.message ?: ""))
                }
            }

        override suspend fun testConnection(config: Map<String, String>): TestResult = withContext(Dispatchers.IO) {
            try {
                val secretId = config["secretId"] ?: return@withContext TestResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.upload_field_secret_id)))
                val secretKey = config["secretKey"] ?: return@withContext TestResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.upload_field_secret_key)))
                val bucket = config["bucket"] ?: return@withContext TestResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.field_bucket)))
                val region = config["region"] ?: return@withContext TestResult(success = false, message = context.getString(R.string.upload_not_configured, context.getString(R.string.field_region)))

                val host = "$bucket.cos.$region.myqcloud.com"
                val authorization = try {
                    generateCosAuthorization(secretId, secretKey, host, "GET", "/", "")
                } catch (e: Exception) {
                    return@withContext TestResult(success = false, message = context.getString(R.string.upload_tencent_sign_error, e.message ?: ""))
                }

                val request = Request.Builder()
                    .url("https://$host/")
                    .get()
                    .addHeader("Host", host)
                    .addHeader("Authorization", authorization)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        TestResult(success = true, message = context.getString(R.string.upload_connection_success, context.getString(R.string.provider_tencent)))
                    } else {
                        Timber.tag("ImageUpload").e("Tencent COS test failed: code=%d", response.code)
                        TestResult(success = false, message = context.getString(R.string.upload_auth_failed, context.getString(R.string.provider_tencent), response.code))
                    }
                }
            } catch (e: Exception) {
                Timber.tag("ImageUpload").e(e, "Tencent COS test exception")
                TestResult(success = false, message = context.getString(R.string.upload_connection_exception, context.getString(R.string.provider_tencent), e.message ?: ""))
            }
        }

        /**
         * 生成腾讯云 COS 临时签名
         * 使用 COS 签名 v1（兼容性最好）
         */
        private fun generateCosAuthorization(
            secretId: String,
            secretKey: String,
            host: String,
            httpMethod: String,
            uri: String,
            contentType: String,
        ): String {
            val now = System.currentTimeMillis() / 1000
            val keyTime = "$now;${now + 3600}"

            // SignKey = HMAC-SHA1(SecretKey, KeyTime)
            val signKey = hmacSha1(secretKey.toByteArray(), keyTime.toByteArray())

            // HttpString = HttpMethod\nUri\n\nhost=$host\n
            val httpString = "$httpMethod\n$uri\n\nhost=$host\n"
            val httpStringSha1 = sha1(httpString.toByteArray())

            // StringToSign = sha1\nKeyTime\nHttpStringSha1\n
            val stringToSign = "sha1\n$keyTime\n$httpStringSha1\n"
            val signature = Base64.encodeToString(hmacSha1(signKey, stringToSign.toByteArray()), Base64.NO_WRAP)

            return "q-sign-algorithm=sha1&q-ak=$secretId&q-sign-time=$keyTime&q-key-time=$keyTime&q-header-list=host&signature=$signature"
        }
    }

    // ── 自定义图床上传器 ──

    /**
     * 自定义图床上传器
     * 支持自定义接口地址、请求方法、请求头和响应解析规则
     */
    inner class CustomUploader : ImageUploader {

        override suspend fun upload(imageData: ByteArray, fileName: String, config: Map<String, String>): UploadResult =
            withContext(Dispatchers.IO) {
                try {
                    val url = config["url"] ?: return@withContext UploadResult(success = false, message = context.getString(R.string.upload_custom_no_url))
                    val method = config["method"]?.uppercase() ?: "POST"
                    val headersJson = config["headers"] ?: "{}"
                    val responseRule = config["response"] ?: "$.data.url"

                    // 构建请求体：POST 使用 multipart，PUT/PATCH 使用原始字节
                    val requestBuilder = Request.Builder().url(url)

                    when (method) {
                        "PUT" -> {
                            val fileBody = imageData.toRequestBody("application/octet-stream".toMediaType())
                            requestBuilder.put(fileBody)
                        }
                        "PATCH" -> {
                            val fileBody = imageData.toRequestBody("application/octet-stream".toMediaType())
                            requestBuilder.patch(fileBody)
                        }
                        else -> {
                            // POST 使用 multipart
                            val fileBody = imageData.toRequestBody("image/*".toMediaType())
                            val multipartBody = MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("file", fileName, fileBody)
                                .build()
                            requestBuilder.post(multipartBody)
                        }
                    }

                    // 解析并添加自定义请求头
                    try {
                        val headersObj = JsonParser.parseString(headersJson).asJsonObject
                        headersObj.entrySet().forEach { (key, value) ->
                            requestBuilder.addHeader(key, value.asString)
                        }
                    } catch (e: Exception) {
                        Timber.tag("ImageUpload").w(e, "Custom uploader: failed to parse headers JSON, using defaults")
                    }

                    val request = requestBuilder.build()
                    httpClient.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string() ?: return@withContext UploadResult(
                            success = false,
                            message = context.getString(R.string.upload_custom_empty_response),
                        )

                        if (!response.isSuccessful) {
                            Timber.tag("ImageUpload").e("Custom upload HTTP error: code=%d, url=%s", response.code, url)
                            return@withContext UploadResult(
                                success = false,
                                message = context.getString(R.string.upload_custom_api_error, response.code),
                            )
                        }

                        // 使用简化的 JSONPath 解析响应
                        val imageUrl = parseJsonPath(responseBody, responseRule)
                        if (imageUrl == null) {
                            Timber.tag("ImageUpload").w("Custom upload: failed to extract URL with rule=%s", responseRule)
                            return@withContext UploadResult(
                                success = false,
                                message = context.getString(R.string.upload_custom_cannot_extract_url, responseRule),
                            )
                        }

                        Timber.tag("ImageUpload").i("Custom upload succeeded: fileName=%s, url=%s", fileName, imageUrl)
                        UploadResult(success = true, url = imageUrl)
                    }
                } catch (e: Exception) {
                    Timber.tag("ImageUpload").e(e, "Custom upload exception: fileName=%s", fileName)
                    UploadResult(success = false, message = context.getString(R.string.upload_custom_exception, e.message ?: ""))
                }
            }

        override suspend fun testConnection(config: Map<String, String>): TestResult {
            val url = config["url"]
            return if (url.isNullOrBlank()) {
                TestResult(success = false, message = context.getString(R.string.upload_custom_no_url))
            } else if (!url.startsWith("http")) {
                TestResult(success = false, message = context.getString(R.string.upload_custom_url_must_http))
            } else {
                TestResult(success = true, message = context.getString(R.string.upload_custom_config_valid))
            }
        }

        /**
     * 简化的 JSONPath 解析
     * 支持点号分隔的路径，如 $.data.url
     */
        private fun parseJsonPath(json: String, path: String): String? {
            return try {
                // 去掉 "$." 前缀
                val cleanPath = path.removePrefix("$.")
                val segments = cleanPath.split(".")
                var current: Any? = JsonParser.parseString(json)

                for (segment in segments) {
                    current = when (current) {
                        is com.google.gson.JsonObject -> current.get(segment)
                        else -> return null
                    }
                }

                when (current) {
                    is com.google.gson.JsonPrimitive -> current.asString
                    else -> current?.toString()
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    // ── 通用工具方法 ──

    /**
     * 生成 GMT 格式的日期字符串（用于 HTTP 请求头）
     */
    private fun gmtDateString(): String {
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        return sdf.format(Date())
    }

    /**
     * 根据文件扩展名猜测 Content-Type
     */
    private fun guessContentType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "bmp" -> "image/bmp"
            "svg" -> "image/svg+xml"
            else -> "image/jpeg"
        }
    }

    /**
     * SHA1 哈希（十六进制字符串）
     */
    private fun sha1(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * HMAC-SHA1 签名（通用方法，供各上传器使用）
     */
    private fun hmacSha1(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        return mac.doFinal(data)
    }
}
