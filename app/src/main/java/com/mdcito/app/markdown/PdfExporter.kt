package com.mdcito.app.markdown

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.mdcito.app.R
import timber.log.Timber
import java.io.File

/**
 * PDF 导出器：使用 WebView + Android PrintManager 将 HTML 内容导出为 PDF。
 * 利用 Chromium 渲染引擎确保导出的 PDF 与预览模式完全一致（所见即所得）。
 *
 * 技术方案：WebView.createPrintDocumentAdapter() + PrintManager.print()
 * - 使用与预览相同的 Chromium 渲染引擎，保证视觉一致性
 * - 输出矢量 PDF（文本可选可搜索），自动 A4 分页
 * - 通过系统打印服务生成 PDF，无需额外第三方依赖
 * - PrintManager 内部处理 LayoutResultCallback/WriteResultCallback，
 *   避免了 Kotlin 无法直接创建这些 package-private 回调类的问题
 *
 * 性能优化：
 * - 内联 highlight.js（从 raw 资源读取），消除 CDN 网络请求延迟
 * - 使用 JS 监听图片加载完成，替代固定延迟，确保所有图片渲染后再打印
 * - content:// URI 直接从路径推断 MIME 类型，避免 getType() IPC 调用
 */
class PdfExporter(private val context: Context) {

    private var webView: WebView? = null
    private var timeoutHandler: Handler? = null
    private var timeoutRunnable: Runnable? = null

    /** 缓存的内联 highlight.js 脚本内容 */
    private val inlineHighlightJs: String by lazy {
        try {
            context.resources.openRawResource(R.raw.highlight_min_js)
                .bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Timber.tag("PdfExport").w(e, "highlight.js加载失败，代码高亮将不可用")
            ""
        }
    }

    /**
     * 将 HTML 内容导出为 PDF
     *
     * 调用后会弹出系统打印对话框，用户可选择"保存为 PDF"来导出文件。
     * 此方法需要 Activity 级别的 Context（PrintManager 要求）。
     *
     * @param htmlContent 完整的 HTML 文档内容（含 CSS 样式）
     * @param jobName 打印任务名称（用于系统打印对话框显示）
     * @param onReady WebView 渲染完成、打印对话框即将弹出时回调（主线程）
     * @param onComplete 导出完成回调（主线程，包括用户取消打印时也会调用）
     * @param onError 导出失败回调（主线程）
     */
    fun export(
        htmlContent: String,
        jobName: String,
        onReady: () -> Unit = {},
        onComplete: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        Timber.tag("PdfExport").i("开始导出PDF: %s", jobName)
        Handler(Looper.getMainLooper()).post {
            try {
                val wv = WebView(context)
                webView = wv

                wv.settings.javaScriptEnabled = true
                wv.settings.loadWithOverviewMode = true
                wv.settings.useWideViewPort = true
                wv.settings.allowFileAccess = true

                // 超时保护：30 秒内未完成则取消导出
                val handler = Handler(Looper.getMainLooper())
                timeoutHandler = handler
                timeoutRunnable = Runnable {
                    Timber.tag("PdfExport").e("PDF导出超时: %s", jobName)
                    cleanup(wv)
                    onError(Exception("PDF export timed out"))
                }
                handler.postDelayed(timeoutRunnable!!, 30_000)

                wv.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // 注入图片加载监听脚本：
                        // 等待所有 <img> 加载完成后再触发打印，
                        // 如果没有图片则立即触发
                        view?.evaluateJavascript(
                            """
                            (function() {
                                var images = document.querySelectorAll('img');
                                var total = images.length;
                                if (total === 0) {
                                    window._pdfImagesReady = true;
                                } else {
                                    var loaded = 0;
                                    var failed = 0;
                                    function checkDone() {
                                        if (loaded + failed >= total) {
                                            window._pdfImagesReady = true;
                                        }
                                    }
                                    images.forEach(function(img) {
                                        if (img.complete && img.naturalWidth > 0) {
                                            loaded++;
                                            checkDone();
                                        } else {
                                            img.addEventListener('load', function() { loaded++; checkDone(); });
                                            img.addEventListener('error', function() { failed++; checkDone(); });
                                        }
                                    });
                                    // 兜底：3 秒后强制就绪，避免单张图片加载失败导致永久等待
                                    setTimeout(function() { window._pdfImagesReady = true; }, 3000);
                                }
                            })();
                            """.trimIndent(),
                            null
                        )
                        // 轮询检查图片是否全部加载完成
                        pollImagesReady(wv, handler, jobName, onReady, onComplete, onError)
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val url = request?.url?.toString()
                            ?: return super.shouldInterceptRequest(view, request)

                        // 处理本地文件路径的图片（应用内部存储，file:// URI）
                        if (url.startsWith("file:///data/")) {
                            try {
                                val path = android.net.Uri.parse(url).path
                                    ?: return super.shouldInterceptRequest(view, request)
                                val file = File(path)
                                if (file.exists()) {
                                    return WebResourceResponse(
                                        guessImageMimeType(file.name),
                                        "UTF-8",
                                        file.inputStream()
                                    )
                                }
                            } catch (e: Exception) {
                                Timber.tag("PdfExport").w(e, "拦截file://资源异常: %s", url)
                            }
                        }

                        // 兼容旧数据：绝对路径无 file:// 前缀
                        if (url.startsWith("https://cdnjs.cloudflare.com/data/data/")
                            || url.startsWith("/data/data/")
                        ) {
                            try {
                                val filePath = if (url.startsWith("https://")) {
                                    url.removePrefix("https://cdnjs.cloudflare.com")
                                } else {
                                    url
                                }
                                val file = File(filePath)
                                if (file.exists()) {
                                    return WebResourceResponse(
                                        guessImageMimeType(file.name),
                                        "UTF-8",
                                        file.inputStream()
                                    )
                                }
                            } catch (e: Exception) {
                                Timber.tag("PdfExport").w(e, "拦截旧路径资源异常: %s", url)
                            }
                        }

                        // 处理 content:// URI 的图片（Photo Picker 等来源）
                        if (url.startsWith("content://")) {
                            try {
                                val uri = android.net.Uri.parse(url)
                                val inputStream = context.contentResolver.openInputStream(uri)
                                if (inputStream != null) {
                                    // 直接从 URI 路径推断 MIME 类型，避免 getType() 的 IPC 调用
                                    val mimeType = guessImageMimeTypeFromUri(url)
                                    return WebResourceResponse(mimeType, "UTF-8", inputStream)
                                }
                            } catch (e: Exception) {
                                Timber.tag("PdfExport").w(e, "拦截content://资源异常: %s", url)
                            }
                        }

                        return super.shouldInterceptRequest(view, request)
                    }
                }

                // 将 CDN 外部脚本替换为内联脚本，消除网络请求延迟
                val optimizedHtml = inlineScripts(htmlContent)

                wv.loadDataWithBaseURL(
                    "https://cdnjs.cloudflare.com",
                    optimizedHtml,
                    "text/html",
                    "UTF-8",
                    null
                )
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    /**
     * 将 HTML 中的 CDN 外部脚本替换为内联脚本，消除网络请求。
     * 替换 highlight.js 和 KaTeX 的 CDN 引用。
     */
    private fun inlineScripts(html: String): String {
        var result = html
        val hljs = inlineHighlightJs
        if (hljs.isNotEmpty()) {
            result = result.replace(
                """<script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>""",
                "<script>$hljs</script>"
            )
        }
        val katexJs = inlineKatexJs
        if (katexJs.isNotEmpty()) {
            result = result.replace(
                """<script src="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.js"></script>""",
                "<script>$katexJs</script>"
            )
        }
        val autoRenderJs = inlineKatexAutoRenderJs
        if (autoRenderJs.isNotEmpty()) {
            result = result.replace(
                """<script src="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/contrib/auto-render.min.js"></script>""",
                "<script>$autoRenderJs</script>"
            )
        }
        return result
    }

    /** 缓存的内联 KaTeX 脚本内容 */
    private val inlineKatexJs: String by lazy {
        try {
            context.resources.openRawResource(R.raw.katex_min_js)
                .bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Timber.tag("PdfExport").w(e, "KaTeX JS加载失败，数学公式将不可用")
            ""
        }
    }

    /** 缓存的内联 KaTeX auto-render 脚本内容 */
    private val inlineKatexAutoRenderJs: String by lazy {
        try {
            context.resources.openRawResource(R.raw.katex_auto_render_min_js)
                .bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Timber.tag("PdfExport").w(e, "KaTeX auto-render JS加载失败")
            ""
        }
    }

    /**
     * 轮询检查图片是否全部加载完成，完成后触发打印。
     * 使用 100ms 间隔轮询，最多等待 5 秒（含兜底 3 秒 JS 超时）。
     */
    private fun pollImagesReady(
        webView: WebView,
        handler: Handler,
        jobName: String,
        onReady: () -> Unit,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        var attempts = 0
        val maxAttempts = 50 // 50 * 100ms = 5 秒
        val runnable = object : Runnable {
            override fun run() {
                attempts++
                webView.evaluateJavascript("window._pdfImagesReady === true") { result ->
                    if (result == "true" || attempts >= maxAttempts) {
                        if (attempts >= maxAttempts) {
                            Timber.tag("PdfExport").w("图片加载轮询超时，强制触发打印")
                        }
                        // 取消超时
                        timeoutRunnable?.let { handler.removeCallbacks(it) }
                        // 图片就绪或超时，触发打印
                        printWebView(webView, jobName, onReady, onComplete, onError)
                    } else {
                        handler.postDelayed(this, 100)
                    }
                }
            }
        }
        handler.postDelayed(runnable, 100)
    }

    private fun printWebView(
        webView: WebView,
        jobName: String,
        onReady: () -> Unit,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                cleanup(webView)
                onError(Exception("Print service not available"))
                return
            }

            val printAdapter = webView.createPrintDocumentAdapter(jobName)

            val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("pdf", "PDF", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()

            val printJob = printManager.print(jobName, printAdapter, printAttributes)

            // 通知调用方 WebView 渲染完成，打印对话框已弹出
            onReady()

            // 监控打印任务状态
            monitorPrintJob(printJob, webView, onComplete, onError)
        } catch (e: Exception) {
            cleanup(webView)
            onError(e)
        }
    }

    private fun monitorPrintJob(
        printJob: android.print.PrintJob,
        webView: WebView,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                when {
                    printJob.isCompleted -> {
                        Timber.tag("PdfExport").i("PDF导出完成")
                        cleanup(webView)
                        onComplete()
                    }
                    printJob.isCancelled -> {
                        cleanup(webView)
                        // 用户取消打印视为正常完成
                        onComplete()
                    }
                    printJob.isFailed -> {
                        Timber.tag("PdfExport").e("PDF导出失败")
                        cleanup(webView)
                        onError(Exception("PDF export failed"))
                    }
                    else -> handler.postDelayed(this, 500)
                }
            }
        }
        handler.postDelayed(runnable, 500)
    }

    private fun cleanup(webView: WebView) {
        try {
            webView.destroy()
        } catch (e: Exception) {
            Timber.tag("PdfExport").w(e, "WebView清理失败")
        }
        this.webView = null
        timeoutHandler = null
    }

    /** 取消正在进行的导出 */
    fun cancel() {
        webView?.let {
            it.stopLoading()
            it.destroy()
        }
        webView = null
        timeoutHandler = null
    }

    private fun guessImageMimeType(fileName: String): String = when {
        fileName.endsWith(".png", ignoreCase = true) -> "image/png"
        fileName.endsWith(".gif", ignoreCase = true) -> "image/gif"
        fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
        fileName.endsWith(".bmp", ignoreCase = true) -> "image/bmp"
        fileName.endsWith(".svg", ignoreCase = true) -> "image/svg+xml"
        else -> "image/jpeg"
    }

    /** 从 content:// URI 路径推断 MIME 类型，避免 getType() 的 IPC 调用 */
    private fun guessImageMimeTypeFromUri(uri: String): String {
        // 尝试从 URI 的最后路径段提取文件扩展名
        val lastSegment = uri.substringAfterLast('/')
        return guessImageMimeType(lastSegment)
    }
}
