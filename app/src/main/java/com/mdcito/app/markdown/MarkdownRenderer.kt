package com.mdcito.app.markdown

import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

enum class MarkdownDialect(val key: String) {
    COMMONMARK("commonmark"),
    GFM("gfm"),
    JETBRAINS("jetbrains");

    companion object {
        fun fromKey(key: String): MarkdownDialect =
            entries.find { it.key == key } ?: GFM
    }
}

object MarkdownRenderer {

    // ── CommonMark 解析器（无扩展） ──
    private val commonmarkParser: Parser by lazy {
        Parser.builder().build()
    }

    private val commonmarkRenderer: HtmlRenderer by lazy {
        HtmlRenderer.builder().build()
    }

    // ── GFM 解析器（含 GFM 扩展） ──
    private val gfmExtensions = listOf(
        TablesExtension.create(),
        StrikethroughExtension.create(),
        TaskListItemsExtension.create(),
        AutolinkExtension.create(),
    )

    private val gfmParser: Parser by lazy {
        Parser.builder()
            .extensions(gfmExtensions)
            .build()
    }

    private val gfmRenderer: HtmlRenderer by lazy {
        HtmlRenderer.builder()
            .extensions(gfmExtensions + HeadingAnchorExtension.builder().build())
            .build()
    }

    // ── JetBrains Markdown 解析器 ──
    private val jetbrainsGfmFlavour = GFMFlavourDescriptor()
    private val jetbrainsCommonMarkFlavour = CommonMarkFlavourDescriptor()

    fun parse(markdown: String, dialect: MarkdownDialect = MarkdownDialect.GFM): Node {
        return when (dialect) {
            MarkdownDialect.COMMONMARK -> commonmarkParser.parse(markdown)
            MarkdownDialect.GFM -> gfmParser.parse(markdown)
            MarkdownDialect.JETBRAINS -> gfmParser.parse(markdown) // AST 提取仍用 commonmark
        }
    }

    fun renderToHtml(markdown: String, dialect: MarkdownDialect = MarkdownDialect.GFM): String {
        val html = when (dialect) {
            MarkdownDialect.COMMONMARK -> {
                val document = commonmarkParser.parse(markdown)
                commonmarkRenderer.render(document)
            }
            MarkdownDialect.GFM -> {
                val document = gfmParser.parse(markdown)
                gfmRenderer.render(document)
            }
            MarkdownDialect.JETBRAINS -> {
                renderWithJetbrains(markdown, useGfm = true)
            }
        }
        // 注入基于行号的锚点 ID，用于目录点击定位
        return injectHeadingLineIds(markdown, html)
    }

    /**
     * 为 HTML 中的标题标签注入基于行号的 id 属性
     * 格式: id="heading-line-N"，N 为 Markdown 源文件中的行号（0-based）
     *
     * 按顺序在 HTML 中查找所有 <h1>~<h6> 标签，与 headings 列表一一对应，
     * 避免同级标题匹配错乱。
     */
    private fun injectHeadingLineIds(markdown: String, html: String): String {
        val headings = extractHeadings(markdown)
        if (headings.isEmpty()) return html

        // 按顺序查找 HTML 中所有标题标签
        val headingTagRegex = Regex("""<h([1-6])(\s[^>]*)?>""")
        val matches = headingTagRegex.findAll(html).toList()

        if (matches.size < headings.size) return html

        val sb = StringBuilder()
        var lastEnd = 0
        for (i in headings.indices) {
            val match = matches[i]
            val heading = headings[i]
            val anchorId = "heading-line-${heading.lineIndex}"

            // 追加匹配之前的内容
            sb.append(html, lastEnd, match.range.first)

            // 构建新的标签
            val existingAttrs = match.groupValues[2]
            if (existingAttrs.contains("id=")) {
                // 替换已有 id
                val newAttrs = existingAttrs.replace(Regex("""id="[^"]*""""), "id=\"$anchorId\"")
                sb.append("<h${heading.level}$newAttrs>")
            } else {
                // 添加 id
                sb.append("<h${heading.level} id=\"$anchorId\"$existingAttrs>")
            }

            lastEnd = match.range.last + 1
        }
        // 追加剩余内容
        sb.append(html, lastEnd, html.length)

        return sb.toString()
    }

    private fun renderWithJetbrains(markdown: String, useGfm: Boolean = true): String {
        val flavour = if (useGfm) jetbrainsGfmFlavour else jetbrainsCommonMarkFlavour
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdown)
        return HtmlGenerator(markdown, parsedTree, flavour).generateHtml()
    }

    fun extractHeadings(markdown: String): List<MarkdownHeading> {
        val headings = mutableListOf<MarkdownHeading>()

        markdown.lines().forEachIndexed { index, line ->
            val trimmed = line.trimStart()
            val level = when {
                trimmed.startsWith("######") -> 6
                trimmed.startsWith("#####") -> 5
                trimmed.startsWith("####") -> 4
                trimmed.startsWith("###") -> 3
                trimmed.startsWith("##") -> 2
                trimmed.startsWith("#") -> 1
                else -> 0
            }
            if (level > 0) {
                val title = trimmed.removePrefix("#".repeat(level)).trimStart()
                headings.add(MarkdownHeading(level, title, index))
            }
        }

        return headings
    }

    fun wrapHtml(
        bodyHtml: String,
        isDark: Boolean = false,
        codeFontFace: String? = null,
        backgroundColor: String? = null,
        surfaceColor: String? = null,
        textColor: String? = null,
        onTextColor: String? = null,
        dialect: MarkdownDialect = MarkdownDialect.GFM,
    ): String {
        val cssStyle = if (isDark) buildDarkCss(backgroundColor, surfaceColor, textColor, onTextColor, dialect)
        else buildLightCss(backgroundColor, surfaceColor, textColor, onTextColor, dialect)
        val codeFontStyle = codeFontFace ?: ""
        // highlight.js 主题：暗色用 Atom One Dark，亮色用 Atom One Light
        val hljsTheme = if (isDark) "atom-one-dark" else "atom-one-light"
        // 内联 highlight.js 主题 CSS，避免依赖网络加载
        val hljsCss = if (isDark) HIGHLIGHT_JS_DARK_CSS else HIGHLIGHT_JS_LIGHT_CSS
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.css">
                <style>$hljsCss</style>
                <style>$cssStyle</style>
                <style>$codeFontStyle</style>
            </head>
            <body>
                <article class="markdown-body">$bodyHtml</article>
                <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>
                <script src="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.js"></script>
                <script src="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/contrib/auto-render.min.js"></script>
                <script>
                    // 为代码块添加语言标签
                    document.querySelectorAll('pre code[class*="language-"]').forEach(function(block) {
                        var lang = block.className.match(/language-(\w+)/);
                        if (lang) block.setAttribute('data-language', lang[1]);
                    });
                    if (typeof hljs !== 'undefined') {
                        hljs.highlightAll();
                    }
                    // 删除线兜底：将未被解析的 ~~text~~ 转为 <del> 标签
                    var body = document.querySelector('.markdown-body');
                    if (body) {
                        body.innerHTML = body.innerHTML.replace(/~~([^~]+)~~/g, '<del>$1</del>');
                    }
                    // 表格包裹：将 <table> 包裹在 <div class="table-wrapper"> 中，实现横向滚动
                    document.querySelectorAll('.markdown-body table').forEach(function(table) {
                        if (table.parentElement && table.parentElement.classList.contains('table-wrapper')) return;
                        var wrapper = document.createElement('div');
                        wrapper.className = 'table-wrapper';
                        table.parentNode.insertBefore(wrapper, table);
                        wrapper.appendChild(table);
                    });
                    // KaTeX 数学公式渲染
                    (function() {
                        if (typeof katex === 'undefined') return;
                        // 渲染 JetBrains 方言生成的 .math-block 和 .math-inline
                        document.querySelectorAll('.math-block, .math-inline').forEach(function(el) {
                            var tex = el.textContent || el.innerText;
                            var displayMode = el.classList.contains('math-block');
                            try {
                                katex.render(tex.trim(), el, { displayMode: displayMode, throwOnError: false });
                            } catch(e) {
                                el.textContent = tex;
                            }
                        });
                        // 渲染 GFM/CommonMark 中的 $...$ 和 $$...$$（未被解析器处理的纯文本）
                        var container = document.querySelector('.markdown-body');
                        if (container) {
                            renderMathInElement(container, {
                                delimiters: [
                                    {left: '$$', right: '$$', display: true},
                                    {left: '$', right: '$', display: false},
                                ],
                                throwOnError: false
                            });
                        }
                    })();
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildLightCss(
        backgroundColor: String?,
        surfaceColor: String?,
        textColor: String?,
        onTextColor: String?,
        dialect: MarkdownDialect = MarkdownDialect.GFM,
    ): String {
        val bg = backgroundColor ?: "#ffffff"
        val surface = surfaceColor ?: "#f6f8fa"
        val codeBg = surfaceColor ?: "#eff1f3"
        val text = textColor ?: "#1f2328"
        val onText = onTextColor ?: "#656d76"
        val border = surfaceColor ?: "#d1d9e0"
        val link = "#0969da"
        val dialectSpecific = buildDialectSpecificLightCss(dialect, border, onText)
        return """
        .markdown-body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Noto Sans', Helvetica, Arial, sans-serif;
            font-size: 16px;
            line-height: 1.7;
            color: $text;
            padding: 0 16px;
            margin: 0;
            word-wrap: break-word;
            background: $bg;
        }
        .markdown-body h1, .markdown-body h2, .markdown-body h3,
        .markdown-body h4, .markdown-body h5, .markdown-body h6 {
            margin-top: 24px;
            margin-bottom: 16px;
            font-weight: 600;
            line-height: 1.25;
        }
        .markdown-body h1 { font-size: 2em; border-bottom: 1px solid $border; padding-bottom: .3em; }
        .markdown-body h2 { font-size: 1.5em; border-bottom: 1px solid $border; padding-bottom: .3em; }
        .markdown-body h3 { font-size: 1.25em; }
        .markdown-body h4 { font-size: 1em; }
        .markdown-body h5 { font-size: .875em; }
        .markdown-body h6 { font-size: .85em; color: $onText; }
        .markdown-body p { margin-top: 0; margin-bottom: 16px; }
        .markdown-body a { color: $link; text-decoration: none; }
        .markdown-body a:hover { text-decoration: underline; }
        .markdown-body strong, .markdown-body b { font-weight: 700; }
        .markdown-body em, .markdown-body i { font-style: italic; }
        .markdown-body del, .markdown-body s { text-decoration: line-through !important; color: $onText; }
        .markdown-body a del, .markdown-body a s { text-decoration: line-through !important; }
        .markdown-body strong del, .markdown-body b del, .markdown-body strong s, .markdown-body b s { text-decoration: line-through !important; }
        .markdown-body em del, .markdown-body i del, .markdown-body em s, .markdown-body i s { text-decoration: line-through !important; }
        .markdown-body del code, .markdown-body s code { text-decoration: line-through !important; }
        .markdown-body strong em, .markdown-body strong i, .markdown-body b em, .markdown-body b i { font-weight: 700; font-style: italic; }
        .markdown-body em strong, .markdown-body em b, .markdown-body i strong, .markdown-body i b { font-weight: 700; font-style: italic; }
        .markdown-body code {
            background: $codeBg;
            padding: .2em .4em;
            border-radius: 6px;
            font-family: 'JetBrains Mono', 'Consolas', 'SFMono-Regular', monospace;
            font-size: 85%;
        }
        .markdown-body pre {
            background: #fafafa;
            padding: 16px;
            border-radius: 8px;
            overflow-x: auto;
            margin-top: 0;
            margin-bottom: 16px;
            line-height: 1.5;
            border: 1px solid $border;
            position: relative;
        }
        .markdown-body pre code {
            background: transparent;
            padding: 0;
            border-radius: 0;
            font-size: 85%;
            /* 不覆盖 color，让 highlight.js 主题控制代码颜色 */
        }
        /* 代码块语言标签 */
        .markdown-body pre code[class*="language-"]::before {
            content: attr(data-language);
            position: absolute;
            top: 4px;
            right: 8px;
            font-size: 11px;
            color: $onText;
            opacity: 0.6;
            text-transform: uppercase;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
        }
        .markdown-body pre code.hljs {
            padding: 0;
            background: transparent;
        }
        .markdown-body blockquote {
            border-left: 4px solid $border;
            margin: 0 0 16px 0;
            padding: 4px 16px;
            color: $onText;
        }
        .markdown-body ul, .markdown-body ol {
            margin-top: 0;
            margin-bottom: 16px;
            padding-left: 2em;
        }
        .markdown-body li { margin-bottom: 4px; }
        .markdown-body li + li { margin-top: 4px; }
        .markdown-body table {
            border-collapse: collapse;
            width: 100%;
            margin-top: 0;
            margin-bottom: 16px;
        }
        .markdown-body th, .markdown-body td {
            border: 1px solid $border;
            padding: 6px 13px;
            overflow-wrap: break-word;
            word-break: break-word;
            hyphens: auto;
        }
        .markdown-body th { font-weight: 600; }
        .markdown-body td[align="left"], .markdown-body th[align="left"] { text-align: left; }
        .markdown-body td[align="center"], .markdown-body th[align="center"] { text-align: center; }
        .markdown-body td[align="right"], .markdown-body th[align="right"] { text-align: right; }
        .markdown-body .table-wrapper {
            overflow-x: auto;
            -webkit-overflow-scrolling: touch;
            margin-bottom: 16px;
        }
        .markdown-body hr {
            border: 0;
            border-top: 2px solid $border;
            margin: 24px 0;
            padding: 0;
        }
        .markdown-body img {
            max-width: 100%;
            border-radius: 8px;
            box-sizing: border-box;
        }
        .markdown-body input[type="checkbox"] {
            margin-right: 8px;
            accent-color: $link;
        }
        .markdown-body .task-list-item {
            list-style-type: none;
        }
        .markdown-body .task-list-item + .task-list-item {
            margin-top: 4px;
        }
        /* KaTeX 数学公式 */
        .markdown-body .katex-display {
            margin: 16px 0;
            overflow-x: auto;
            overflow-y: hidden;
        }
        .markdown-body .katex { font-size: 1.1em; }
        $dialectSpecific
    """
    }

    private fun buildDarkCss(
        backgroundColor: String?,
        surfaceColor: String?,
        textColor: String?,
        onTextColor: String?,
        dialect: MarkdownDialect = MarkdownDialect.GFM,
    ): String {
        val bg = backgroundColor ?: "#0d1117"
        val surface = surfaceColor ?: "#161b22"
        val codeBg = surfaceColor ?: "#1c2128"
        val text = textColor ?: "#e6edf3"
        val headingText = textColor ?: "#f0f6fc"
        val onText = onTextColor ?: "#8b949e"
        val border = surfaceColor ?: "#3d444d"
        val link = "#58a6ff"
        val dialectSpecific = buildDialectSpecificDarkCss(dialect, border, onText)
        return """
        .markdown-body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Noto Sans', Helvetica, Arial, sans-serif;
            font-size: 16px;
            line-height: 1.7;
            color: $text;
            padding: 0 16px;
            margin: 0;
            word-wrap: break-word;
            background: $bg;
        }
        .markdown-body h1, .markdown-body h2, .markdown-body h3,
        .markdown-body h4, .markdown-body h5, .markdown-body h6 {
            margin-top: 24px;
            margin-bottom: 16px;
            font-weight: 600;
            line-height: 1.25;
            color: $headingText;
        }
        .markdown-body h1 { font-size: 2em; border-bottom: 1px solid $border; padding-bottom: .3em; }
        .markdown-body h2 { font-size: 1.5em; border-bottom: 1px solid $border; padding-bottom: .3em; }
        .markdown-body h3 { font-size: 1.25em; }
        .markdown-body h4 { font-size: 1em; }
        .markdown-body h5 { font-size: .875em; }
        .markdown-body h6 { font-size: .85em; color: $onText; }
        .markdown-body p { margin-top: 0; margin-bottom: 16px; }
        .markdown-body a { color: $link; text-decoration: none; }
        .markdown-body a:hover { text-decoration: underline; }
        .markdown-body strong, .markdown-body b { font-weight: 700; color: $headingText; }
        .markdown-body em, .markdown-body i { font-style: italic; }
        .markdown-body del, .markdown-body s { text-decoration: line-through !important; color: $onText; }
        .markdown-body a del, .markdown-body a s { text-decoration: line-through !important; }
        .markdown-body strong del, .markdown-body b del, .markdown-body strong s, .markdown-body b s { text-decoration: line-through !important; }
        .markdown-body em del, .markdown-body i del, .markdown-body em s, .markdown-body i s { text-decoration: line-through !important; }
        .markdown-body del code, .markdown-body s code { text-decoration: line-through !important; }
        .markdown-body strong em, .markdown-body strong i, .markdown-body b em, .markdown-body b i { font-weight: 700; font-style: italic; color: $headingText; }
        .markdown-body em strong, .markdown-body em b, .markdown-body i strong, .markdown-body i b { font-weight: 700; font-style: italic; color: $headingText; }
        .markdown-body code {
            background: $codeBg;
            padding: .2em .4em;
            border-radius: 6px;
            font-family: 'JetBrains Mono', 'Consolas', 'SFMono-Regular', monospace;
            font-size: 85%;
        }
        .markdown-body pre {
            background: #1a1b26;
            padding: 16px;
            border-radius: 8px;
            overflow-x: auto;
            margin-top: 0;
            margin-bottom: 16px;
            border: 1px solid $border;
            line-height: 1.5;
            position: relative;
        }
        .markdown-body pre code {
            background: transparent;
            padding: 0;
            border-radius: 0;
            font-size: 85%;
            /* 不覆盖 color，让 highlight.js 主题控制代码颜色 */
        }
        /* 代码块语言标签 */
        .markdown-body pre code[class*="language-"]::before {
            content: attr(data-language);
            position: absolute;
            top: 4px;
            right: 8px;
            font-size: 11px;
            color: $onText;
            opacity: 0.6;
            text-transform: uppercase;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
        }
        .markdown-body pre code.hljs {
            padding: 0;
            background: transparent;
        }
        .markdown-body blockquote {
            border-left: 4px solid $border;
            margin: 0 0 16px 0;
            padding: 4px 16px;
            color: $onText;
        }
        .markdown-body ul, .markdown-body ol {
            margin-top: 0;
            margin-bottom: 16px;
            padding-left: 2em;
        }
        .markdown-body li { margin-bottom: 4px; }
        .markdown-body li + li { margin-top: 4px; }
        .markdown-body table {
            border-collapse: collapse;
            width: 100%;
            margin-top: 0;
            margin-bottom: 16px;
        }
        .markdown-body th, .markdown-body td {
            border: 1px solid $border;
            padding: 6px 13px;
            overflow-wrap: break-word;
            word-break: break-word;
            hyphens: auto;
        }
        .markdown-body th { font-weight: 600; color: $headingText; }
        .markdown-body td[align="left"], .markdown-body th[align="left"] { text-align: left; }
        .markdown-body td[align="center"], .markdown-body th[align="center"] { text-align: center; }
        .markdown-body td[align="right"], .markdown-body th[align="right"] { text-align: right; }
        .markdown-body .table-wrapper {
            overflow-x: auto;
            -webkit-overflow-scrolling: touch;
            margin-bottom: 16px;
        }
        .markdown-body hr {
            border: 0;
            border-top: 2px solid $border;
            margin: 24px 0;
            padding: 0;
        }
        .markdown-body img {
            max-width: 100%;
            border-radius: 8px;
            box-sizing: border-box;
        }
        .markdown-body input[type="checkbox"] {
            margin-right: 8px;
            accent-color: $link;
        }
        .markdown-body .task-list-item {
            list-style-type: none;
        }
        .markdown-body .task-list-item + .task-list-item {
            margin-top: 4px;
        }
        /* KaTeX 数学公式 */
        .markdown-body .katex-display {
            margin: 16px 0;
            overflow-x: auto;
            overflow-y: hidden;
        }
        .markdown-body .katex { font-size: 1.1em; }
        $dialectSpecific
    """
    }

    /**
     * CommonMark 方言特有样式：不显示表格、删除线、任务列表等 GFM 扩展样式
     * 实际上 CommonMark 解析器不会生成这些 HTML，所以 CSS 无需特殊处理
     */
    private fun buildDialectSpecificLightCss(dialect: MarkdownDialect, border: String, onText: String): String {
        return when (dialect) {
            MarkdownDialect.COMMONMARK -> """
                /* CommonMark: 严格规范，无额外扩展样式 */
            """
            MarkdownDialect.GFM -> ""
            MarkdownDialect.JETBRAINS -> """
                /* JetBrains Markdown: LaTeX 数学公式样式（KaTeX 渲染后覆盖） */
                .markdown-body .math-block {
                    margin: 16px 0;
                    padding: 12px;
                    text-align: center;
                    overflow-x: auto;
                }
                .markdown-body .math-inline {
                }
            """
        }
    }

    private fun buildDialectSpecificDarkCss(dialect: MarkdownDialect, border: String, onText: String): String {
        return when (dialect) {
            MarkdownDialect.COMMONMARK -> """
                /* CommonMark: 严格规范，无额外扩展样式 */
            """
            MarkdownDialect.GFM -> ""
            MarkdownDialect.JETBRAINS -> """
                /* JetBrains Markdown: LaTeX 数学公式样式（KaTeX 渲染后覆盖） */
                .markdown-body .math-block {
                    margin: 16px 0;
                    padding: 12px;
                    text-align: center;
                    overflow-x: auto;
                }
                .markdown-body .math-inline {
                }
            """
        }
    }
}

data class MarkdownHeading(
    val level: Int,
    val title: String,
    val lineIndex: Int,
    val anchorId: String = "heading-line-$lineIndex",
)

// ── 内联 highlight.js 主题 CSS（Atom One Dark / Atom One Light） ──
// 避免依赖 CDN 网络加载，确保离线也能正常渲染代码块颜色

private const val HIGHLIGHT_JS_DARK_CSS = """
pre code.hljs{display:block;overflow-x:auto;padding:1em}code.hljs{padding:3px 5px}.hljs{color:#abb2bf;background:#282c34}.hljs-comment,.hljs-quote{color:#5c6370;font-style:italic}.hljs-doctag,.hljs-formula,.hljs-keyword{color:#c678dd}.hljs-deletion,.hljs-name,.hljs-section,.hljs-selector-tag,.hljs-subst{color:#e06c75}.hljs-literal{color:#56b6c2}.hljs-addition,.hljs-attribute,.hljs-meta .hljs-string,.hljs-regexp,.hljs-string{color:#98c379}.hljs-attr,.hljs-number,.hljs-selector-attr,.hljs-selector-class,.hljs-selector-pseudo,.hljs-template-variable,.hljs-type,.hljs-variable{color:#d19a66}.hljs-bullet,.hljs-link,.hljs-meta,.hljs-selector-id,.hljs-symbol,.hljs-title{color:#61aeee}.hljs-built_in,.hljs-class .hljs-title,.hljs-title.class_{color:#e6c07b}.hljs-emphasis{font-style:italic}.hljs-strong{font-weight:700}.hljs-link{text-decoration:underline}
"""

private const val HIGHLIGHT_JS_LIGHT_CSS = """
pre code.hljs{display:block;overflow-x:auto;padding:1em}code.hljs{padding:3px 5px}.hljs{color:#383a42;background:#fafafa}.hljs-comment,.hljs-quote{color:#a0a1a7;font-style:italic}.hljs-doctag,.hljs-formula,.hljs-keyword{color:#a626a4}.hljs-deletion,.hljs-name,.hljs-section,.hljs-selector-tag,.hljs-subst{color:#e45649}.hljs-literal{color:#0184bb}.hljs-addition,.hljs-attribute,.hljs-meta .hljs-string,.hljs-regexp,.hljs-string{color:#50a14f}.hljs-attr,.hljs-number,.hljs-selector-attr,.hljs-selector-class,.hljs-selector-pseudo,.hljs-template-variable,.hljs-type,.hljs-variable{color:#986801}.hljs-bullet,.hljs-link,.hljs-meta,.hljs-selector-id,.hljs-symbol,.hljs-title{color:#4078f2}.hljs-built_in,.hljs-class .hljs-title,.hljs-title.class_{color:#c18401}.hljs-emphasis{font-style:italic}.hljs-strong{font-weight:700}.hljs-link{text-decoration:underline}
"""
