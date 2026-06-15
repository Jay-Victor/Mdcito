package com.mdcito.app.data.sync

import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebDavSyncProvider @Inject constructor() : CloudSyncProvider {

    companion object {
        private val XML_MEDIA_TYPE = "application/xml; charset=utf-8".toMediaType()
        private val OCTET_STREAM_MEDIA_TYPE = "application/octet-stream".toMediaType()

        private val PROPFIND_BODY = """
            <?xml version="1.0" encoding="utf-8"?>
            <d:propfind xmlns:d="DAV:">
                <d:prop>
                    <d:resourcetype/>
                    <d:getcontentlength/>
                    <d:getlastmodified/>
                    <d:getetag/>
                    <d:displayname/>
                </d:prop>
            </d:propfind>
        """.trimIndent()
    }

    override val serviceType: CloudSyncServiceType = CloudSyncServiceType.WEBDAV

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun testConnection(config: CloudSyncConfig): ConnectionTestResult =
        withContext(Dispatchers.IO) {
            try {
                val url = buildUrl(config, "/")
                val request = buildRequest(config, url)
                    .header("Depth", "0")
                    .method("PROPFIND", PROPFIND_BODY.toRequestBody(XML_MEDIA_TYPE))
                    .build()

                val response = client.newCall(request).execute()
                val serverInfo = response.header("Server", response.header("X-Powered-By"))

                if (response.isSuccessful || response.code == 207) {
                    Timber.tag("CloudSync").i("WebDAV 连接测试成功: ${config.serverUrl}")
                    ConnectionTestResult(
                        success = true,
                        message = "连接成功",
                        serverInfo = serverInfo,
                    )
                } else if (response.code == 401) {
                    ConnectionTestResult(
                        success = false,
                        message = "认证失败：用户名或密码错误",
                        serverInfo = serverInfo,
                    )
                } else {
                    ConnectionTestResult(
                        success = false,
                        message = "连接失败：HTTP ${response.code}",
                        serverInfo = serverInfo,
                    )
                }
            } catch (e: Exception) {
                Timber.tag("CloudSync").e(e, "WebDAV 连接测试失败: ${config.serverUrl}")
                ConnectionTestResult(
                    success = false,
                    message = "连接失败：${e.message}",
                )
            }
        }

    override suspend fun listFiles(
        config: CloudSyncConfig,
        remotePath: String,
    ): List<RemoteFileInfo> = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl(config, remotePath)
            val request = buildRequest(config, url)
                .header("Depth", "1")
                .method("PROPFIND", PROPFIND_BODY.toRequestBody(XML_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful && response.code != 207) {
                Timber.tag("CloudSync").w("WebDAV 列出文件失败：HTTP ${response.code}, path=$remotePath")
                return@withContext emptyList()
            }

            val body = response.body?.string() ?: return@withContext emptyList()
            val basePathPrefix = extractBasePathPrefix(config)
            val result = parsePropfindResponse(body, remotePath, basePathPrefix)
            Timber.tag("CloudSync").d("WebDAV 列出文件: $remotePath, 共 ${result.size} 个")
            result
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "WebDAV 列出文件失败：$remotePath")
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
            val url = buildUrl(config, remotePath)
            val request = buildRequest(config, url)
                .put(content.toRequestBody(OCTET_STREAM_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Timber.tag("CloudSync").w("WebDAV 上传文件失败：HTTP ${response.code}, path=$remotePath, size=${content.size}")
                return@withContext false
            }

            // Try to set last-modified via PROPPATCH (best-effort)
            try {
                setLastModified(config, remotePath, lastModified)
            } catch (e: Exception) {
                Timber.tag("CloudSync").w(e, "WebDAV 设置 lastModified 失败: $remotePath")
                // Not all WebDAV servers support PROPPATCH for lastmodified
            }

            Timber.tag("CloudSync").i("WebDAV 上传成功: $remotePath (${content.size} bytes)")
            true
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "WebDAV 上传文件失败：$remotePath")
            throw RuntimeException("上传文件失败: ${e.message}", e)
        }
    }

    override suspend fun downloadFile(
        config: CloudSyncConfig,
        remotePath: String,
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl(config, remotePath)
            val request = buildRequest(config, url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Timber.tag("CloudSync").w("WebDAV 下载文件失败：HTTP ${response.code}, path=$remotePath")
                return@withContext null
            }

            val bytes = response.body?.bytes()
            if (bytes != null) {
                Timber.tag("CloudSync").i("WebDAV 下载成功: $remotePath (${bytes.size} bytes)")
            }
            bytes
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "WebDAV 下载文件失败：$remotePath")
            throw RuntimeException("下载文件失败: ${e.message}", e)
        }
    }

    override suspend fun deleteFile(
        config: CloudSyncConfig,
        remotePath: String,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl(config, remotePath)
            val request = buildRequest(config, url)
                .delete()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Timber.tag("CloudSync").w("WebDAV 删除文件失败：HTTP ${response.code}, path=$remotePath")
                return@withContext false
            }
            Timber.tag("CloudSync").i("WebDAV 删除文件: $remotePath")
            true
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "WebDAV 删除文件失败：$remotePath")
            throw RuntimeException("删除文件失败: ${e.message}", e)
        }
    }

    override suspend fun createDirectory(
        config: CloudSyncConfig,
        remotePath: String,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl(config, remotePath)
            val request = buildRequest(config, url)
                .method("MKCOL", null)
                .build()

            val response = client.newCall(request).execute()
            // 405 means the directory already exists
            if (!response.isSuccessful && response.code != 405) {
                Timber.tag("CloudSync").w("WebDAV 创建目录失败：HTTP ${response.code}, path=$remotePath")
                return@withContext false
            }
            Timber.tag("CloudSync").i("WebDAV 创建目录成功: $remotePath")
            true
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "WebDAV 创建目录失败：$remotePath")
            throw RuntimeException("创建目录失败: ${e.message}", e)
        }
    }

    override suspend fun getFileInfo(
        config: CloudSyncConfig,
        remotePath: String,
    ): RemoteFileInfo? = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl(config, remotePath)
            val request = buildRequest(config, url)
                .header("Depth", "0")
                .method("PROPFIND", PROPFIND_BODY.toRequestBody(XML_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful && response.code != 207) {
                Timber.tag("CloudSync").w("WebDAV 获取文件信息失败：HTTP ${response.code}, path=$remotePath")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val basePathPrefix = extractBasePathPrefix(config)
            val files = parsePropfindResponse(body, remotePath, basePathPrefix)
            // With Depth:0, the first entry is the resource itself
            val info = files.firstOrNull()
            if (info != null) {
                Timber.tag("CloudSync").d("WebDAV 获取文件信息: $remotePath -> isDir=${info.isDirectory}, size=${info.size}")
            }
            info
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "WebDAV 获取文件信息失败：$remotePath")
            throw RuntimeException("获取文件信息失败: ${e.message}", e)
        }
    }

    // --- Internal helpers ---

    private fun buildUrl(config: CloudSyncConfig, remotePath: String): String {
        // 强制使用 HTTPS，防止凭据通过明文 HTTP 传输
        var base = config.serverUrl.trimEnd('/')
        if (base.startsWith("http://")) {
            base = "https://" + base.substring(7)
        }
        val path = if (remotePath.startsWith("/")) remotePath else "/$remotePath"
        return if (config.port > 0) {
            // Reconstruct URL with custom port
            val url = java.net.URL(base)
            val scheme = url.protocol
            val host = url.host
            val basePath = url.path.trimEnd('/')
            "$scheme://$host:${config.port}$basePath$path"
        } else {
            "$base$path"
        }
    }

    private fun buildRequest(config: CloudSyncConfig, url: String): Request.Builder {
        return Request.Builder()
            .url(url)
            .apply {
                if (config.username.isNotEmpty()) {
                    addHeader("Authorization", Credentials.basic(config.username, config.password))
                }
            }
    }

    private fun setLastModified(
        config: CloudSyncConfig,
        remotePath: String,
        lastModified: Long,
    ) {
        val url = buildUrl(config, remotePath)
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }
        val dateStr = dateFormat.format(lastModified)
        val proppatchBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <d:propertyupdate xmlns:d="DAV:">
                <d:set>
                    <d:prop>
                        <d:getlastmodified>$dateStr</d:getlastmodified>
                    </d:prop>
                </d:set>
            </d:propertyupdate>
        """.trimIndent()

        val request = buildRequest(config, url)
            .method("PROPPATCH", proppatchBody.toRequestBody(XML_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().close()
    }

    private fun parsePropfindResponse(xml: String, requestPath: String, basePathPrefix: String = ""): List<RemoteFileInfo> {
        val results = mutableListOf<RemoteFileInfo>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var href: String? = null
            var displayName: String? = null
            var isDirectory = false
            var size: Long = 0L
            var lastModified: Long = 0L
            var etag: String? = null
            var inResponse = false
            var currentTag = ""

            val nsDav = "DAV:"

            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        val ns = parser.namespace
                        when {
                            parser.name == "response" && ns == nsDav -> {
                                inResponse = true
                                href = null
                                displayName = null
                                isDirectory = false
                                size = 0L
                                lastModified = 0L
                                etag = null
                            }
                            inResponse && parser.name == "href" && ns == nsDav -> {
                                href = parser.nextText()
                            }
                            inResponse && parser.name == "displayname" && ns == nsDav -> {
                                displayName = parser.nextText()
                            }
                            inResponse && parser.name == "resourcetype" && ns == nsDav -> {
                                // Look ahead for collection element
                                val depth = parser.depth
                                while (true) {
                                    parser.next()
                                    if (parser.eventType == XmlPullParser.END_TAG &&
                                        parser.name == "resourcetype" && parser.namespace == nsDav &&
                                        parser.depth == depth
                                    ) {
                                        break
                                    }
                                    if (parser.eventType == XmlPullParser.START_TAG &&
                                        parser.name == "collection" && parser.namespace == nsDav
                                    ) {
                                        isDirectory = true
                                    }
                                }
                            }
                            inResponse && parser.name == "getcontentlength" && ns == nsDav -> {
                                val text = parser.nextText()
                                size = text.toLongOrNull() ?: 0L
                            }
                            inResponse && parser.name == "getlastmodified" && ns == nsDav -> {
                                val text = parser.nextText()
                                lastModified = parseHttpDate(text)
                            }
                            inResponse && parser.name == "getetag" && ns == nsDav -> {
                                etag = parser.nextText()
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "response" && parser.namespace == nsDav && inResponse) {
                            inResponse = false
                            if (href != null) {
                                val decodedHref = try {
                                    URLDecoder.decode(href, "UTF-8")
                                } catch (e: Exception) {
                                    Timber.tag("CloudSync").w(e, "WebDAV URL 解码失败")
                                    href
                                }
                                val name = displayName ?: extractFileName(decodedHref)
                                val path = normalizePath(decodedHref, basePathPrefix)

                                results.add(
                                    RemoteFileInfo(
                                        path = path,
                                        name = name,
                                        isDirectory = isDirectory,
                                        size = size,
                                        lastModified = lastModified,
                                        etag = etag,
                                    )
                                )
                            }
                        }
                    }
                }
                parser.next()
            }
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "WebDAV 解析 PROPFIND 响应失败")
        }

        // Filter out the parent directory entry (the request path itself)
        val normalizedRequestPath = normalizePath(requestPath, basePathPrefix)
        return results.filter { info ->
            val normalizedInfoPath = info.path.trimEnd('/')
            val normalizedReqPath = normalizedRequestPath.trimEnd('/')
            normalizedInfoPath != normalizedReqPath
        }
    }

    private fun extractFileName(href: String): String {
        val trimmed = href.trimEnd('/')
        val lastSlash = trimmed.lastIndexOf('/')
        return if (lastSlash >= 0) trimmed.substring(lastSlash + 1) else trimmed
    }

    /**
     * 规范化路径：从完整 URL 提取路径部分，并去除 serverUrl 中的路径前缀。
     * 例如：serverUrl=https://example.com/dav, href=/dav/Mdcito/file.txt → /Mdcito/file.txt
     * 这样返回的路径与 config.remotePath 格式一致，确保同步逻辑能正确匹配。
     */
    private fun normalizePath(href: String, basePathPrefix: String = ""): String {
        var path = try {
            if (href.startsWith("http://") || href.startsWith("https://")) {
                java.net.URL(href).path
            } else {
                href
            }
        } catch (e: Exception) {
            Timber.tag("CloudSync").w(e, "WebDAV 路径规范化失败")
            href
        }

        // 去除 serverUrl 中的路径前缀，使路径与 config.remotePath 格式一致
        if (basePathPrefix.isNotBlank() && path.startsWith(basePathPrefix)) {
            path = path.substring(basePathPrefix.length)
            if (!path.startsWith("/")) path = "/$path"
        }

        return path
    }

    /**
     * 从 config.serverUrl 中提取路径前缀。
     * 例如：https://example.com/dav → /dav
     * 用于在 PROPFIND 响应中去除此前缀，使路径与 config.remotePath 格式一致。
     */
    private fun extractBasePathPrefix(config: CloudSyncConfig): String {
        return try {
            val base = config.serverUrl.trimEnd('/')
            val url = java.net.URL(if (base.startsWith("http://") || base.startsWith("https://")) base else "https://$base")
            val path = url.path.trimEnd('/')
            if (path.isNotBlank() && path != "/") path else ""
        } catch (e: Exception) {
            Timber.tag("CloudSync").w(e, "WebDAV 提取路径前缀失败")
            ""
        }
    }

    private fun parseHttpDate(dateStr: String): Long {
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss z",
            "EEE, dd-MMM-yy HH:mm:ss z",
            "EEE MMM dd HH:mm:ss yyyy",
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("GMT")
                return sdf.parse(dateStr)?.time ?: 0L
            } catch (e: Exception) {
                Timber.tag("CloudSync").w(e, "WebDAV HTTP 日期解析失败: $dateStr")
                continue
            }
        }
        return 0L
    }

    override suspend fun getStorageInfo(config: CloudSyncConfig): StorageInfo? = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl(config, "/")
            val propfindBody = """
                <?xml version="1.0" encoding="utf-8"?>
                <d:propfind xmlns:d="DAV:">
                    <d:prop>
                        <d:quota-available-bytes/>
                        <d:quota-used-bytes/>
                    </d:prop>
                </d:propfind>
            """.trimIndent()

            val request = buildRequest(config, url)
                .header("Depth", "0")
                .method("PROPFIND", propfindBody.toRequestBody(XML_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful && response.code != 207) return@withContext null

            val body = response.body?.string() ?: return@withContext null

            // 解析 quota 信息
            var usedBytes = 0L
            var availableBytes = 0L

            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(body))

            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "quota-used-bytes" -> {
                            val text = parser.nextText()
                            usedBytes = text.toLongOrNull() ?: 0L
                        }
                        "quota-available-bytes" -> {
                            val text = parser.nextText()
                            availableBytes = text.toLongOrNull() ?: 0L
                        }
                    }
                }
                parser.next()
            }

            if (usedBytes > 0 || availableBytes > 0) {
                val info = StorageInfo(
                    usedBytes = usedBytes,
                    totalBytes = usedBytes + availableBytes,
                )
                Timber.tag("CloudSync").d("WebDAV 存储空间: 已用 %d bytes, 总计 %d bytes", info.usedBytes, info.totalBytes)
                info
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.tag("CloudSync").w(e, "WebDAV 获取存储空间信息失败")
            null
        }
    }
}
