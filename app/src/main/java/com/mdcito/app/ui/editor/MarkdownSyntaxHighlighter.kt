package com.mdcito.app.ui.editor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

data class MarkdownHighlightColors(
    val heading: Color,
    val bold: Color,
    val italic: Color,
    val code: Color,
    val codeBlock: Color,
    val link: Color,
    val quote: Color,
    val list: Color,
    val strikethrough: Color,
    val hr: Color,
    val image: Color,
    val highlightBg: Color,
    val codeBg: Color,
    val indent: Color,
    val table: Color,
    val math: Color,
)

@Composable
fun rememberMarkdownHighlightColors(): MarkdownHighlightColors {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme) {
        MarkdownHighlightColors(
            heading = scheme.primary,
            bold = scheme.tertiary,
            italic = scheme.secondary,
            code = scheme.error,
            codeBlock = scheme.onSurfaceVariant,
            link = scheme.primary,
            quote = scheme.secondary,
            list = scheme.tertiary,
            strikethrough = scheme.onSurfaceVariant,
            hr = scheme.outline,
            image = scheme.secondary,
            highlightBg = scheme.tertiaryContainer,
            codeBg = scheme.surfaceVariant,
            indent = scheme.onSurfaceVariant,
            table = scheme.outline,
            math = scheme.tertiary,
        )
    }
}

@Composable
fun highlightMarkdown(
    markdown: String,
    highlightedLine: Int? = null,
    highlightLineColor: String = "#4C6EF5",
    spellErrors: List<SpellError> = emptyList(),
    bracketMatchingEnabled: Boolean = false,
    cursorPosition: Int = -1,
): AnnotatedString {
    val colors = rememberMarkdownHighlightColors()
    return remember(markdown, highlightedLine, highlightLineColor, spellErrors, bracketMatchingEnabled, cursorPosition, colors) {
        MarkdownSyntaxHighlighter.highlight(markdown, colors, highlightedLine, highlightLineColor, spellErrors, bracketMatchingEnabled, cursorPosition)
    }
}

object MarkdownSyntaxHighlighter {

    fun highlight(
        markdown: String,
        colors: MarkdownHighlightColors = MarkdownHighlightColors(
            heading = Color(0xFF7C9A8E),
            bold = Color(0xFF5A7A6E),
            italic = Color(0xFF6B8E9E),
            code = Color(0xFFC8796E),
            codeBlock = Color(0xFF8A847C),
            link = Color(0xFF6B9E7E),
            quote = Color(0xFF8B7EC8),
            list = Color(0xFFC8B47E),
            strikethrough = Color(0xFFB87E7E),
            hr = Color(0xFFB0A99F),
            image = Color(0xFFA47EB8),
            highlightBg = Color(0xFFFFF3CD),
            codeBg = Color(0xFFF0EDE8),
            indent = Color(0xFF8A847C),
            table = Color(0xFFB0A99F),
            math = Color(0xFF7E9EC8),
        ),
        highlightedLine: Int? = null,
        highlightLineColor: String = "#4C6EF5",
        spellErrors: List<SpellError> = emptyList(),
        bracketMatchingEnabled: Boolean = false,
        cursorPosition: Int = -1,
    ): AnnotatedString = buildAnnotatedString {
        val lineHighlightBg = try {
            android.graphics.Color.parseColor(highlightLineColor).let {
                Color(it).copy(alpha = 0.15f)
            }
        } catch (_: Exception) {
            Color(0xFF4C6EF5).copy(alpha = 0.15f)
        }
        val lines = markdown.lines()
        var inCodeBlock = false
        var codeBlockStart = -1

        for ((lineIndex, line) in lines.withIndex()) {
            if (lineIndex > 0) append("\n")

            val isHighlighted = highlightedLine != null && lineIndex == highlightedLine

            if (line.trimStart().startsWith("```")) {
                if (!inCodeBlock) {
                    codeBlockStart = lineIndex
                    inCodeBlock = true
                    if (isHighlighted) {
                        withStyle(SpanStyle(color = colors.codeBlock, fontFamily = FontFamily.Monospace, background = lineHighlightBg)) {
                            append(line)
                        }
                    } else {
                        withStyle(SpanStyle(color = colors.codeBlock, fontFamily = FontFamily.Monospace)) {
                            append(line)
                        }
                    }
                    continue
                } else {
                    inCodeBlock = false
                    if (isHighlighted) {
                        withStyle(SpanStyle(color = colors.codeBlock, fontFamily = FontFamily.Monospace, background = lineHighlightBg)) {
                            append(line)
                        }
                    } else {
                        withStyle(SpanStyle(color = colors.codeBlock, fontFamily = FontFamily.Monospace)) {
                            append(line)
                        }
                    }
                    continue
                }
            }

            if (inCodeBlock) {
                if (isHighlighted) {
                    withStyle(SpanStyle(color = colors.codeBlock, fontFamily = FontFamily.Monospace, background = lineHighlightBg)) {
                        append(line)
                    }
                } else {
                    withStyle(SpanStyle(color = colors.codeBlock, fontFamily = FontFamily.Monospace)) {
                        append(line)
                    }
                }
                continue
            }

            // 表格行检测：包含 | 的行（必须在水平分割线检测之前）
            val trimmedLine = line.trimStart()
            if (trimmedLine.contains('|')) {
                // 表格分隔行：| --- | :---: | ---: |
                val isTableSeparator = Regex("""^\|?[:\-]+(\|[:\-]+)*\|?$""").matches(trimmedLine.replace("\\s".toRegex(), ""))
                if (isTableSeparator) {
                    if (isHighlighted) {
                        withStyle(SpanStyle(color = colors.table, background = lineHighlightBg)) { append(line) }
                    } else {
                        withStyle(SpanStyle(color = colors.table)) { append(line) }
                    }
                    continue
                }
                // 普通表格行：高亮管道符 | 和对齐冒号
                if (isHighlighted) {
                    appendTableLineWithHighlight(line, colors, lineHighlightBg)
                } else {
                    appendTableLine(line, colors)
                }
                continue
            }

            if (line.trimStart().startsWith("---") ||
                line.trimStart().startsWith("***") ||
                line.trimStart().startsWith("___")
            ) {
                if (isHighlighted) {
                    withStyle(SpanStyle(color = colors.hr, background = lineHighlightBg)) { append(line) }
                } else {
                    withStyle(SpanStyle(color = colors.hr)) { append(line) }
                }
                continue
            }

            val trimmed = line.trimStart()
            val indentLen = line.length - trimmed.length

            if (isHighlighted) {
                withStyle(SpanStyle(background = lineHighlightBg)) {
                    if (indentLen > 0) {
                        withStyle(SpanStyle(color = colors.indent)) {
                            append(line.substring(0, indentLen))
                        }
                    }

                    val headingMatch = Regex("^(#{1,6})\\s").find(trimmed)
                    if (headingMatch != null) {
                        val hashLen = headingMatch.groupValues[1].length
                        withStyle(SpanStyle(color = colors.heading, fontWeight = FontWeight.Bold)) {
                            append(trimmed.substring(0, hashLen))
                        }
                        append(" ")
                        withStyle(SpanStyle(color = colors.heading, fontWeight = FontWeight.SemiBold)) {
                            append(trimmed.substring(hashLen + 1))
                        }
                    } else if (trimmed.startsWith("> ")) {
                        withStyle(SpanStyle(color = colors.quote)) { append("> ") }
                        appendStyledContent(trimmed.substring(2), colors)
                    } else {
                        appendLineWithListPrefix(trimmed, colors)
                    }
                }
                continue
            }

            if (indentLen > 0) {
                withStyle(SpanStyle(color = colors.indent)) {
                    append(line.substring(0, indentLen))
                }
            }

            val headingMatch = Regex("^(#{1,6})\\s").find(trimmed)
            if (headingMatch != null) {
                val hashLen = headingMatch.groupValues[1].length
                withStyle(SpanStyle(color = colors.heading, fontWeight = FontWeight.Bold)) {
                    append(trimmed.substring(0, hashLen))
                }
                append(" ")
                withStyle(SpanStyle(color = colors.heading, fontWeight = FontWeight.SemiBold)) {
                    append(trimmed.substring(hashLen + 1))
                }
                continue
            }

            if (trimmed.startsWith("> ")) {
                withStyle(SpanStyle(color = colors.quote)) { append("> ") }
                appendStyledContent(trimmed.substring(2), colors)
                continue
            }

            appendLineWithListPrefix(trimmed, colors)
        }

        // ── 拼写错误高亮（红色波浪下划线 + 半透明红色背景） ──
        if (spellErrors.isNotEmpty()) {
            val spellErrorUnderline = SpanStyle(
                textDecoration = TextDecoration.Underline,
                color = Color(0xFFE53935),
                background = Color(0xFFE53935).copy(alpha = 0.08f),
            )
            for (error in spellErrors) {
                if (error.range.first >= 0 && error.range.last + 1 <= length) {
                    addStyle(spellErrorUnderline, error.range.first, error.range.last + 1)
                }
            }
        }

        // ── 括号匹配高亮 ──
        if (bracketMatchingEnabled && cursorPosition in 0..markdown.lastIndex) {
            val bracketHighlightStyle = SpanStyle(
                background = Color(0xFF4C6EF5).copy(alpha = 0.25f),
                fontWeight = FontWeight.Bold,
            )
            // 检查光标位置及前一个位置的括号
            val positions = buildList {
                if (cursorPosition in 0..markdown.lastIndex) add(cursorPosition)
                if (cursorPosition - 1 in 0..markdown.lastIndex) add(cursorPosition - 1)
            }
            for (pos in positions) {
                val char = markdown[pos]
                val matchPos = findMatchingBracketInText(markdown, pos)
                if (matchPos != null) {
                    val start = minOf(pos, matchPos)
                    val end = maxOf(pos, matchPos) + 1
                    if (start in 0..length && end <= length) {
                        addStyle(bracketHighlightStyle, start, end)
                    }
                }
            }
        }
    }

    private fun AnnotatedString.Builder.appendTableLine(
        line: String,
        colors: MarkdownHighlightColors,
    ) {
        for (char in line) {
            if (char == '|') {
                withStyle(SpanStyle(color = colors.table)) { append(char) }
            } else {
                append(char)
            }
        }
    }

    private fun AnnotatedString.Builder.appendTableLineWithHighlight(
        line: String,
        colors: MarkdownHighlightColors,
        lineHighlightBg: Color,
    ) {
        withStyle(SpanStyle(background = lineHighlightBg)) {
            for (char in line) {
                if (char == '|') {
                    withStyle(SpanStyle(color = colors.table)) { append(char) }
                } else {
                    append(char)
                }
            }
        }
    }

    private fun AnnotatedString.Builder.appendLineWithListPrefix(
        trimmed: String,
        colors: MarkdownHighlightColors,
    ) {
        val taskMatch = Regex("^(- \\[[ x]])\\s").find(trimmed)
        if (taskMatch != null) {
            withStyle(SpanStyle(color = colors.list)) { append(taskMatch.groupValues[1]) }
            append(" ")
            appendStyledContent(trimmed.substring(taskMatch.groupValues[1].length + 1), colors)
            return
        }

        val unorderedMatch = Regex("^([-*+])\\s").find(trimmed)
        if (unorderedMatch != null) {
            withStyle(SpanStyle(color = colors.list)) { append(unorderedMatch.groupValues[1]) }
            append(" ")
            appendStyledContent(trimmed.substring(2), colors)
            return
        }

        val orderedMatch = Regex("^(\\d+\\.)\\s").find(trimmed)
        if (orderedMatch != null) {
            withStyle(SpanStyle(color = colors.list)) { append(orderedMatch.groupValues[1]) }
            append(" ")
            appendStyledContent(trimmed.substring(orderedMatch.groupValues[1].length + 1), colors)
            return
        }

        appendStyledContent(trimmed, colors)
    }

    private fun AnnotatedString.Builder.appendStyledContent(
        text: String,
        colors: MarkdownHighlightColors,
    ) {
        data class PatternDef(
            val regex: Regex,
            val style: SpanStyle,
            val extract: (MatchResult) -> String,
        )

        val patterns = listOf(
            // 粗斜体必须在粗体和斜体之前匹配，避免部分匹配
            // 注意：extract 必须保留完整的 Markdown 标记符，因为 BasicTextField 的
            // onValueChange 中 newValue.text 会用 AnnotatedString 的纯文本覆盖内容
            PatternDef(
                Regex("\\*\\*\\*(.+?)\\*\\*\\*"),
                SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, color = colors.bold),
            ) { m -> m.groupValues[0] },
            PatternDef(
                Regex("___(.+?)___"),
                SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, color = colors.bold),
            ) { m -> m.groupValues[0] },
            PatternDef(
                Regex("\\*\\*(.+?)\\*\\*"),
                SpanStyle(fontWeight = FontWeight.Bold, color = colors.bold),
            ) { m -> m.groupValues[0] },
            PatternDef(
                Regex("__(.+?)__"),
                SpanStyle(fontWeight = FontWeight.Bold, color = colors.bold),
            ) { m -> m.groupValues[0] },
            PatternDef(
                Regex("\\*(.+?)\\*"),
                SpanStyle(fontStyle = FontStyle.Italic, color = colors.italic),
            ) { m -> m.groupValues[0] },
            PatternDef(
                Regex("(?<!\\w)_(.+?)_(?!\\w)"),
                SpanStyle(fontStyle = FontStyle.Italic, color = colors.italic),
            ) { m -> m.groupValues[0] },
            PatternDef(
                Regex("~~(.+?)~~"),
                SpanStyle(color = colors.strikethrough),
            ) { m -> m.groupValues[0] },
            PatternDef(
                Regex("==(.+?)=="),
                SpanStyle(background = colors.highlightBg, fontWeight = FontWeight.SemiBold),
            ) { m -> m.groupValues[0] },
            PatternDef(
                Regex("<u>(.+?)</u>"),
                SpanStyle(textDecoration = TextDecoration.Underline),
            ) { m -> m.groupValues[0] },
            // LaTeX 块级公式 $$...$$（必须在行内公式 $...$ 之前匹配）
            PatternDef(
                Regex("\\$\\$(.+?)\\$\\$"),
                SpanStyle(fontFamily = FontFamily.Monospace, color = colors.math, fontStyle = FontStyle.Italic),
            ) { m -> m.groupValues[0] },
            // LaTeX 行内公式 $...$
            PatternDef(
                Regex("(?<!\\$)\\$(?![\\s$])(.+?)(?<![\\s$])\\$(?!\\$)"),
                SpanStyle(fontFamily = FontFamily.Monospace, color = colors.math, fontStyle = FontStyle.Italic),
            ) { m -> m.groupValues[0] },
            PatternDef(
                Regex("`([^`]+)`"),
                SpanStyle(fontFamily = FontFamily.Monospace, color = colors.code, background = colors.codeBg),
            ) { m -> m.groupValues[0] },
            // 图片必须在链接之前匹配，因为 ![...](...) 包含 [...](...)
            PatternDef(
                Regex("!\\[([^]]*)\\]\\(([^)]+)\\)"),
                SpanStyle(color = colors.image, fontStyle = FontStyle.Italic),
            ) { m -> m.groupValues[0] },
            PatternDef(
                Regex("\\[([^]]+)\\]\\(([^)]+)\\)"),
                SpanStyle(color = colors.link, fontWeight = FontWeight.Medium, textDecoration = TextDecoration.Underline),
            ) { m -> m.groupValues[0] },
        )

        var currentPos = 0
        while (currentPos < text.length) {
            var earliestMatch: MatchResult? = null
            var earliestPattern: PatternDef? = null
            var earliestStart = Int.MAX_VALUE

            for (pattern in patterns) {
                val match = pattern.regex.find(text, currentPos)
                if (match != null && match.range.first < earliestStart) {
                    earliestStart = match.range.first
                    earliestMatch = match
                    earliestPattern = pattern
                }
            }

            if (earliestMatch == null || earliestPattern == null) {
                append(text.substring(currentPos))
                break
            }

            if (earliestStart > currentPos) {
                append(text.substring(currentPos, earliestStart))
            }

            withStyle(earliestPattern.style) {
                append(earliestPattern.extract(earliestMatch))
            }

            currentPos = earliestMatch.range.last + 1
        }
    }

    /**
     * 在文本中查找匹配括号的位置
     */
    private fun findMatchingBracketInText(text: String, position: Int): Int? {
        if (position < 0 || position >= text.length) return null
        val char = text[position]
        val (openBracket, closeBracket, searchForward) = when (char) {
            '(' -> Triple('(', ')', true)
            ')' -> Triple('(', ')', false)
            '[' -> Triple('[', ']', true)
            ']' -> Triple('[', ']', false)
            '{' -> Triple('{', '}', true)
            '}' -> Triple('{', '}', false)
            else -> return null
        }
        var depth = 0
        val range = if (searchForward) position..text.lastIndex else position downTo 0
        for (i in range) {
            when (text[i]) {
                openBracket -> depth++
                closeBracket -> depth--
            }
            if (depth == 0 && i != position) return i
        }
        return null
    }
}
