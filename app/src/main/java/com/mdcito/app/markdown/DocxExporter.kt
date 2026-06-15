package com.mdcito.app.markdown

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFRun
import timber.log.Timber
import java.io.OutputStream

/**
 * 将 Markdown 内容导出为 DOCX 格式
 * 使用 Apache POI (Apache License 2.0) 实现
 */
object DocxExporter {

    fun export(markdown: String, outputStream: OutputStream, fileName: String = "Untitled") {
        Timber.tag("DocxExport").i("开始导出DOCX: %s", fileName)
        try {
            XWPFDocument().use { doc ->
                val lines = markdown.lines()
                var i = 0
                while (i < lines.size) {
                    val line = lines[i]

                    when {
                        // 代码块
                        line.trimStart().startsWith("```") -> {
                            val lang = line.trimStart().removePrefix("```").trim()
                            i++
                            val codeLines = mutableListOf<String>()
                            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                                codeLines.add(lines[i])
                                i++
                            }
                            // 跳过结束的 ```
                            i++
                            addCodeBlock(doc, codeLines, lang)
                        }
                        // 标题
                        line.trimStart().startsWith("#") -> {
                            val match = Regex("^(#{1,6})\\s+(.+)$").find(line)
                            if (match != null) {
                                val level = match.groupValues[1].length
                                val text = match.groupValues[2]
                                addHeading(doc, text, level)
                            } else {
                                addParagraph(doc, line)
                            }
                        }
                        // 分隔线
                        line.trim() in listOf("---", "***", "___") -> {
                            addHorizontalRule(doc)
                        }
                        // 引用块
                        line.trimStart().startsWith("> ") -> {
                            val quoteLines = mutableListOf<String>()
                            while (i < lines.size && lines[i].trimStart().startsWith("> ")) {
                                quoteLines.add(lines[i].trimStart().removePrefix("> ").trim())
                                i++
                            }
                            addQuoteBlock(doc, quoteLines)
                            continue
                        }
                        // 无序列表
                        line.trimStart().let { it.startsWith("- ") || it.startsWith("* ") || it.startsWith("+ ") } -> {
                            val listItems = mutableListOf<String>()
                            while (i < lines.size) {
                                val trimmed = lines[i].trimStart()
                                if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) {
                                    listItems.add(trimmed.substring(2).trim())
                                    i++
                                } else break
                            }
                            addBulletList(doc, listItems)
                            continue
                        }
                        // 有序列表
                        Regex("^\\d+\\.\\s+").containsMatchIn(line.trimStart()) -> {
                            val listItems = mutableListOf<String>()
                            while (i < lines.size) {
                                val match = Regex("^(\\d+)\\.\\s+(.+)$").find(lines[i].trimStart())
                                if (match != null) {
                                    listItems.add(match.groupValues[2].trim())
                                    i++
                                } else break
                            }
                            addOrderedList(doc, listItems)
                            continue
                        }
                        // 空行
                        line.isBlank() -> {
                            // 跳过空行，段落间由 POI 自动处理间距
                        }
                        // 普通段落
                        else -> {
                            addRichParagraph(doc, line)
                        }
                    }
                    i++
                }

                doc.write(outputStream)
                Timber.tag("DocxExport").d("DOCX写入成功: %s", fileName)
            }
        } catch (e: Exception) {
            Timber.tag("DocxExport").e(e, "DOCX导出失败: %s", fileName)
            throw e
        }
    }

    private fun addHeading(doc: XWPFDocument, text: String, level: Int) {
        val para = doc.createParagraph()
        para.style = "Heading${level.coerceIn(1, 6)}"
        addFormattedRuns(para, text)
    }

    private fun addParagraph(doc: XWPFDocument, text: String) {
        val para = doc.createParagraph()
        addFormattedRuns(para, text)
    }

    private fun addRichParagraph(doc: XWPFDocument, text: String) {
        val para = doc.createParagraph()
        addFormattedRuns(para, text)
    }

    /**
     * 解析行内格式（粗体、斜体、行内代码、删除线）并添加到段落
     */
    private fun addFormattedRuns(para: XWPFParagraph, text: String) {
        // 使用正则解析行内格式
        val pattern = Regex("""(`[^`]+`|\*\*[^*]+\*\*|\*[^*]+\*|~~[^~]+~~|[^`*~]+)""")
        val matches = pattern.findAll(text)

        for (match in matches) {
            val segment = match.value
            val run = para.createRun()

            when {
                segment.startsWith("`") && segment.endsWith("`") -> {
                    // 行内代码
                    run.setText(segment.removeSurrounding("`"))
                    run.fontFamily = "Consolas"
                    run.fontSize = 9
                }
                segment.startsWith("**") && segment.endsWith("**") -> {
                    // 粗体
                    run.setText(segment.removeSurrounding("**"))
                    run.isBold = true
                }
                segment.startsWith("*") && segment.endsWith("*") -> {
                    // 斜体
                    run.setText(segment.removeSurrounding("*"))
                    run.isItalic = true
                }
                segment.startsWith("~~") && segment.endsWith("~~") -> {
                    // 删除线
                    run.setText(segment.removeSurrounding("~~"))
                    run.isStrikeThrough = true
                }
                else -> {
                    // 处理链接 [text](url)
                    val linkPattern = Regex("""\[([^\]]+)\]\(([^)]+)\)""")
                    val linkMatch = linkPattern.find(segment)
                    if (linkMatch != null) {
                        val linkText = linkMatch.groupValues[1]
                        val linkUrl = linkMatch.groupValues[2]
                        run.setText(linkText)
                        run.setColor("0563C1")
                        run.setUnderline(org.apache.poi.xwpf.usermodel.UnderlinePatterns.SINGLE)
                    } else {
                        run.setText(segment)
                    }
                }
            }
        }
    }

    private fun addCodeBlock(doc: XWPFDocument, codeLines: List<String>, lang: String) {
        for ((index, line) in codeLines.withIndex()) {
            val para = doc.createParagraph()
            val run = para.createRun()
            run.setText(line)
            run.fontFamily = "Consolas"
            run.fontSize = 9
            // 代码块背景色效果通过段落底纹实现
            setParagraphShading(para, "F5F5F5")
            if (index < codeLines.size - 1) {
                run.addBreak()
            }
        }
    }

    private fun addQuoteBlock(doc: XWPFDocument, lines: List<String>) {
        for (line in lines) {
            val para = doc.createParagraph()
            para.indentationLeft = 720 // 缩进
            addFormattedRuns(para, line)
            // 引用块样式
            setParagraphShading(para, "F0F0F0")
        }
    }

    private fun addBulletList(doc: XWPFDocument, items: List<String>) {
        for (item in items) {
            val para = doc.createParagraph()
            para.style = "ListBullet"
            addFormattedRuns(para, item)
        }
    }

    private fun addOrderedList(doc: XWPFDocument, items: List<String>) {
        for (item in items) {
            val para = doc.createParagraph()
            para.style = "ListNumber"
            addFormattedRuns(para, item)
        }
    }

    private fun addHorizontalRule(doc: XWPFDocument) {
        val para = doc.createParagraph()
        val run = para.createRun()
        run.setText("────────────────────────────────")
        run.setColor("CCCCCC")
        run.fontSize = 8
    }

    private fun setParagraphShading(para: XWPFParagraph, color: String) {
        val ctP = para.ctp
        val pPr = ctP.pPr ?: ctP.addNewPPr()
        val shd = pPr.addNewShd()
        shd.fill = color
        shd.`val` = org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd.CLEAR
    }
}
