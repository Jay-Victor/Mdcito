package com.mdcito.app.ui.editor

import android.graphics.Color as AndroidColor
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mdcito.app.markdown.MarkdownDialect
import com.mdcito.app.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.delay

@Composable
fun MarkdownPreview(
    content: String,
    modifier: Modifier = Modifier,
    onLinkClick: ((String) -> Unit)? = null,
    codeFontFace: String? = null,
    dialect: MarkdownDialect = MarkdownDialect.GFM,
    syncScrollEnabled: Boolean = false,
    editorScrollPosition: Float = 0f,
    scrollToAnchor: String? = null,
    onScrollToAnchorConsumed: () -> Unit = {},
    onPreviewScrollPositionChanged: ((Float) -> Unit)? = null,
    hasBackgroundImage: Boolean = false,
) {
    val context = LocalContext.current
    val webView = remember { WebView(context) }
    val isDark = LocalIsDarkTheme.current

    // 跟踪 WebView 是否已加载完成
    var isPageLoaded by remember { mutableStateOf(false) }
    // 缓存待执行的锚点滚动
    var pendingAnchor by remember { mutableStateOf<String?>(null) }
    // 防止双向滚动死循环：记录上次由编辑器触发的滚动位置
    var lastEditorDrivenScroll by remember { mutableFloatStateOf(-1f) }
    // 记录是否正在处理编辑器侧的滚动同步
    var isSyncingFromEditor by remember { mutableStateOf(false) }

    // 从 Compose 主题获取颜色并转为 CSS 十六进制
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onBackground
    val onTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    val bgCss = colorToCssHex(backgroundColor)
    val surfaceCss = colorToCssHex(surfaceColor)
    val textCss = colorToCssHex(textColor)
    val onTextCss = colorToCssHex(onTextColor)

    val fullHtml = remember(content, isDark, codeFontFace, bgCss, surfaceCss, textCss, onTextCss, dialect, hasBackgroundImage) {
        val html = com.mdcito.app.markdown.MarkdownRenderer.renderToHtml(content, dialect)
        // 有背景图片时使用透明背景，否则使用主题背景色
        val effectiveBgCss = if (hasBackgroundImage) "transparent" else bgCss
        com.mdcito.app.markdown.MarkdownRenderer.wrapHtml(
            html,
            isDark = isDark,
            codeFontFace = codeFontFace,
            backgroundColor = effectiveBgCss,
            surfaceColor = surfaceCss,
            textColor = textCss,
            onTextColor = onTextCss,
            dialect = dialect,
        )
    }

    DisposableEffect(webView) {
        webView.settings.javaScriptEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        webView.settings.allowFileAccess = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: android.webkit.WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                onLinkClick?.invoke(url)
                return true
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)

                // 处理本地文件路径的图片（应用内部存储，file:// URI）
                if (url.startsWith("file:///data/")) {
                    try {
                        val file = java.io.File(android.net.Uri.parse(url).path ?: return super.shouldInterceptRequest(view, request))
                        if (file.exists()) {
                            val mimeType = guessImageMimeType(file.name)
                            return WebResourceResponse(mimeType, "UTF-8", file.inputStream())
                        }
                    } catch (_: Exception) {}
                }

                // 兼容旧数据：Markdown 中使用绝对路径（无 file:// 前缀），
                // WebView 会将其解析为 https://cdnjs.cloudflare.com/data/data/... 的相对 URL
                if (url.startsWith("https://cdnjs.cloudflare.com/data/data/") || url.startsWith("/data/data/")) {
                    try {
                        // 从 URL 中提取实际文件路径
                        val filePath = if (url.startsWith("https://")) {
                            url.removePrefix("https://cdnjs.cloudflare.com")
                        } else {
                            url
                        }
                        val file = java.io.File(filePath)
                        if (file.exists()) {
                            val mimeType = guessImageMimeType(file.name)
                            return WebResourceResponse(mimeType, "UTF-8", file.inputStream())
                        }
                    } catch (_: Exception) {}
                }

                // 处理 content:// URI 的图片（Photo Picker 等来源）
                if (url.startsWith("content://")) {
                    try {
                        val uri = android.net.Uri.parse(url)
                        val inputStream = context.contentResolver.openInputStream(uri)
                        if (inputStream != null) {
                            val mimeType = context.contentResolver.getType(uri) ?: "image/*"
                            return WebResourceResponse(mimeType, "UTF-8", inputStream)
                        }
                    } catch (_: Exception) {}
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isPageLoaded = true
                // 注入滚动监听 JavaScript，实现预览→编辑器方向同步
                view?.evaluateJavascript(
                    """
                    (function() {
                        var lastScrollTop = 0;
                        var scrollTimeout = null;
                        function onScroll() {
                            if (scrollTimeout) clearTimeout(scrollTimeout);
                            scrollTimeout = setTimeout(function() {
                                var scrollTop = document.documentElement.scrollTop || document.body.scrollTop || 0;
                                var scrollHeight = document.documentElement.scrollHeight || document.body.scrollHeight || 1;
                                var ratio = scrollTop / (scrollHeight - window.innerHeight);
                                if (ratio < 0) ratio = 0;
                                if (ratio > 1) ratio = 1;
                                if (Math.abs(scrollTop - lastScrollTop) > 5) {
                                    lastScrollTop = scrollTop;
                                    if (typeof Android !== 'undefined') {
                                        Android.onPreviewScroll(ratio);
                                    }
                                }
                            }, 50);
                        }
                        window.removeEventListener('scroll', onScroll);
                        window.addEventListener('scroll', onScroll, true);
                    })();
                    """.trimIndent(),
                    null
                )
                // 页面加载完成后，如果有同步滚动需求，立即应用
                if (syncScrollEnabled && editorScrollPosition > 0f) {
                    view?.evaluateJavascript(
                        "var sh = document.documentElement.scrollHeight || document.body.scrollHeight; " +
                        "document.documentElement.scrollTop = document.body.scrollTop = sh * $editorScrollPosition;",
                        null
                    )
                }
            }
        }
        onDispose { webView.destroy() }
    }

    // ── 同步滚动：编辑器 → 预览（带防抖优化） ──
    LaunchedEffect(editorScrollPosition, syncScrollEnabled, isPageLoaded) {
        if (syncScrollEnabled && editorScrollPosition > 0f && isPageLoaded) {
            isSyncingFromEditor = true
            lastEditorDrivenScroll = editorScrollPosition
            // 使用兼容性更好的滚动方式，同时设置 documentElement 和 body
            webView.evaluateJavascript(
                "var sh = document.documentElement.scrollHeight || document.body.scrollHeight; " +
                "var st = sh * $editorScrollPosition; " +
                "document.documentElement.scrollTop = st; " +
                "document.body.scrollTop = st;",
                null
            )
            // 短暂延迟后重置同步标记，避免双向滚动死循环
            delay(100)
            isSyncingFromEditor = false
        }
    }

    // ── 目录点击：缓存锚点，页面加载完成后执行滚动 ──
    LaunchedEffect(scrollToAnchor) {
        if (scrollToAnchor != null) {
            pendingAnchor = scrollToAnchor
            onScrollToAnchorConsumed()
        }
    }

    // 页面加载完成后执行缓存的锚点滚动
    LaunchedEffect(isPageLoaded, pendingAnchor) {
        if (isPageLoaded && pendingAnchor != null) {
            val anchor = pendingAnchor!!
            webView.evaluateJavascript(
                """
                (function() {
                    var el = document.getElementById('$anchor');
                    if (el) {
                        el.scrollIntoView({behavior: 'smooth', block: 'start'});
                    }
                })();
                """.trimIndent(),
                null
            )
            pendingAnchor = null
        }
    }

    // 内容变化时重置加载状态
    LaunchedEffect(fullHtml) {
        isPageLoaded = false
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                webView.apply {
                    // 有背景图片时设置 WebView 透明
                    if (hasBackgroundImage) {
                        setBackgroundColor(AndroidColor.TRANSPARENT)
                    }
                    // 注入 JavaScript 接口，用于预览→编辑器方向同步
                    addJavascriptInterface(object : Any() {
                        @android.webkit.JavascriptInterface
                        fun onPreviewScroll(ratio: Float) {
                            // 仅在非编辑器驱动滚动时回调，防止死循环
                            if (!isSyncingFromEditor && syncScrollEnabled) {
                                onPreviewScrollPositionChanged?.invoke(ratio.coerceIn(0f, 1f))
                            }
                        }
                    }, "Android")
                }
            },
            update = { view ->
                // 根据是否有背景图片设置透明或不透明背景
                if (hasBackgroundImage) {
                    view.setBackgroundColor(AndroidColor.TRANSPARENT)
                } else {
                    view.setBackgroundColor(backgroundColor.toArgb())
                }
                view.loadDataWithBaseURL(
                    "https://cdnjs.cloudflare.com",
                    fullHtml,
                    "text/html",
                    "UTF-8",
                    null,
                )
            },
            modifier = Modifier.fillMaxSize(),
        )

        // ── 同步滚动状态指示器 ──
        if (syncScrollEnabled && isPageLoaded) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    tonalElevation = 1.dp,
                    modifier = Modifier.size(8.dp),
                ) {}
            }
        }
    }
}

/** 将 Compose Color 转为 CSS 可用的 #RRGGBB 格式 */
private fun colorToCssHex(color: androidx.compose.ui.graphics.Color): String {
    return "#${String.format("%02X%02X%02X", (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())}"
}

/** 根据文件扩展名推断图片 MIME 类型 */
private fun guessImageMimeType(fileName: String): String = when {
    fileName.endsWith(".png", ignoreCase = true) -> "image/png"
    fileName.endsWith(".gif", ignoreCase = true) -> "image/gif"
    fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
    fileName.endsWith(".bmp", ignoreCase = true) -> "image/bmp"
    fileName.endsWith(".svg", ignoreCase = true) -> "image/svg+xml"
    else -> "image/jpeg"
}
