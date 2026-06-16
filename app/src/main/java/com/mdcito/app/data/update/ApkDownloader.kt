package com.mdcito.app.data.update

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApkDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    @Volatile private var isPaused = false
    @Volatile private var isCancelled = false

    fun pauseDownload() {
        isPaused = true
        val current = _downloadState.value
        if (current is DownloadState.Downloading) {
            _downloadState.value = DownloadState.Paused(
                progress = current.progress,
                downloadedBytes = current.downloadedBytes,
                totalBytes = current.totalBytes
            )
        }
    }

    fun resumeDownload() {
        isPaused = false
    }

    fun cancelDownload() {
        isCancelled = true
        // 清理暂停状态下残留的部分文件
        val current = _downloadState.value
        if (current is DownloadState.Paused) {
            val downloadDir = File(
                context.externalCacheDir ?: context.cacheDir,
                "updates"
            )
            downloadDir.listFiles()?.forEach { it.delete() }
        }
        _downloadState.value = DownloadState.Idle
    }

    fun resetState() {
        isPaused = false
        isCancelled = false
        _downloadState.value = DownloadState.Idle
    }

    /**
     * 下载 APK 文件，支持断点续传和进度跟踪
     * 如果目标文件已部分存在，通过 HTTP Range 头从中断处继续
     */
    suspend fun downloadApk(
        url: String,
        fileName: String
    ): String? = withContext(Dispatchers.IO) {
        isCancelled = false

        // URL 验证：确保包含有效的 HTTP/HTTPS 协议前缀
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Timber.tag("ApkDownloader").e("下载 URL 格式无效，缺少 HTTP/HTTPS 协议前缀: $url")
            _downloadState.value = DownloadState.Error("URL 格式无效：缺少协议前缀")
            return@withContext null
        }

        val downloadDir = File(
            context.externalCacheDir ?: context.cacheDir,
            "updates"
        )
        downloadDir.mkdirs()
        val targetFile = File(downloadDir, fileName)

        // 如果之前的状态是 Paused，文件保留了部分数据，断点续传
        val resumeFrom: Long = if (targetFile.exists() && targetFile.length() > 0) {
            targetFile.length()
        } else {
            if (targetFile.exists()) targetFile.delete()
            0L
        }

        try {
            val request = Request.Builder()
                .url(url)
                .apply {
                    if (resumeFrom > 0) {
                        header("Range", "bytes=$resumeFrom-")
                    }
                }
                .build()

            val response = httpClient.newCall(request).execute()

            val code = response.code
            // 206 = Partial Content (Range 成功), 200 = 完整下载
            if (code != 200 && code != 206) {
                _downloadState.value = DownloadState.Error("HTTP $code")
                return@withContext null
            }

            val body = response.body ?: run {
                _downloadState.value = DownloadState.Error("响应体为空")
                return@withContext null
            }

            val totalBytes = if (code == 206) {
                // 206 时 contentLength 是剩余字节，总大小需从 Content-Range 解析
                val contentRange = response.header("Content-Range")
                val total = contentRange?.substringAfterLast('/')?.toLongOrNull()
                total ?: (resumeFrom + body.contentLength())
            } else {
                body.contentLength()
            }

            var downloadedBytes = resumeFrom
            var lastSpeedCalcTime = System.currentTimeMillis()
            var lastSpeedCalcBytes = downloadedBytes
            var currentSpeed = 0L

            isPaused = false

            body.byteStream().use { input ->
                // 使用 FileOutputStream append 模式实现真正的断点续传
                FileOutputStream(targetFile, resumeFrom > 0).buffered()
                    .use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            if (isCancelled) {
                                targetFile.delete()
                                _downloadState.value = DownloadState.Idle
                                return@withContext null
                            }

                            while (isPaused && !isCancelled) {
                                Thread.sleep(200)
                            }
                            if (isCancelled) {
                                targetFile.delete()
                                _downloadState.value = DownloadState.Idle
                                return@withContext null
                            }

                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            val now = System.currentTimeMillis()
                            val timeDiff = now - lastSpeedCalcTime
                            if (timeDiff >= 1000) {
                                currentSpeed = (downloadedBytes - lastSpeedCalcBytes) * 1000 / timeDiff
                                lastSpeedCalcTime = now
                                lastSpeedCalcBytes = downloadedBytes
                            }

                            val progress = if (totalBytes > 0) {
                                downloadedBytes.toFloat() / totalBytes
                            } else 0f

                            val remainingTimeSecs = if (currentSpeed > 0 && totalBytes > 0) {
                                ((totalBytes - downloadedBytes) / currentSpeed)
                            } else 0L

                            _downloadState.value = DownloadState.Downloading(
                                progress = progress.coerceIn(0f, 1f),
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes,
                                speedBytesPerSec = currentSpeed,
                                remainingTimeSecs = remainingTimeSecs
                            )
                        }
                    }
            }

            if (targetFile.exists() && targetFile.length() > 0) {
                _downloadState.value = DownloadState.Completed(targetFile.absolutePath)
                Timber.tag("ApkDownloader").i("下载完成: ${targetFile.absolutePath}")
                targetFile.absolutePath
            } else {
                _downloadState.value = DownloadState.Error("下载文件异常")
                null
            }
        } catch (e: CancellationException) {
            Timber.tag("ApkDownloader").w("下载被取消")
            _downloadState.value = DownloadState.Idle
            null
        } catch (e: Exception) {
            Timber.tag("ApkDownloader").e(e, "下载失败")
            _downloadState.value = DownloadState.Error(e.message ?: "下载失败")
            null
        }
    }
}
