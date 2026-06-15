package com.mdcito.app.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdcito.app.data.db.entity.FileEntity
import com.mdcito.app.data.db.entity.VersionEntity
import com.mdcito.app.data.files.FileOperations
import com.mdcito.app.data.font.FontService
import com.mdcito.app.data.repository.FileRepository
import com.mdcito.app.data.repository.HistoryRepository
import com.mdcito.app.data.repository.SettingsRepository
import com.mdcito.app.data.repository.VersionRepository
import com.mdcito.app.data.image.ImageUploadService
import com.mdcito.app.data.image.UploadResult
import com.mdcito.app.markdown.MarkdownDialect
import com.mdcito.app.markdown.MarkdownRenderer
import com.mdcito.app.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import timber.log.Timber

enum class EditorMode { PLAIN, RENDER }

enum class SaveState { IDLE, SAVING, SAVED, ERROR }

enum class SaveConfirmState { NONE, SAVED, ERROR }

enum class ExportFormat { MARKDOWN, PLAIN_TEXT, HTML, PDF, DOCX, IMAGE_PNG, ZIP }

enum class MarkdownToolbarAction {
    UNDO, REDO,
    BOLD, ITALIC, BOLD_ITALIC, STRIKETHROUGH,
    HEADING1, HEADING2, HEADING3, HEADING4, HEADING5, HEADING6,
    BULLET_LIST, ORDERED_LIST, TASK_LIST,
    BLOCKQUOTE, INLINE_CODE, CODE_BLOCK,
    LINK, IMAGE, HORIZONTAL_RULE, TABLE,
    SEARCH,
}

data class CursorInfo(
    val line: Int = 1,
    val column: Int = 1,
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0,
    val hasSelection: Boolean = false,
    val selectedText: String = "",
)

data class MatchedLine(
    val lineNumber: Int,
    val lineContent: String,
    val matchRangesInLine: List<IntRange>,
    val matchIndices: List<Int>,
)

data class SearchState(
    val query: String = "",
    val replaceQuery: String = "",
    val isCaseSensitive: Boolean = false,
    val isRegex: Boolean = false,
    val isWholeWord: Boolean = false,
    val matches: List<IntRange> = emptyList(),
    val currentMatchIndex: Int = -1,
    val searchHistory: List<String> = emptyList(),
    val matchedLines: List<MatchedLine> = emptyList(),
) {
    val matchCount: Int get() = matches.size
    val currentMatchDisplay: Int get() = if (currentMatchIndex >= 0) currentMatchIndex + 1 else 0
    val hasMatches: Boolean get() = matches.isNotEmpty()
    val currentMatch: IntRange? get() = matches.getOrNull(currentMatchIndex)
}



@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context,
    private val fileRepository: FileRepository,
    private val fileOperations: FileOperations,
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val versionRepository: VersionRepository,
    val fontService: FontService,
    private val imageUploadService: ImageUploadService,
) : ViewModel() {

    val fileId: Long? = savedStateHandle.get<Long>("fileId")

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _fileName = MutableStateFlow("")
    val fileName: StateFlow<String> = _fileName.asStateFlow()

    private val _editorMode = MutableStateFlow(EditorMode.PLAIN)
    val editorMode: StateFlow<EditorMode> = _editorMode.asStateFlow()

    private val _isModified = MutableStateFlow(false)
    val isModified: StateFlow<Boolean> = _isModified.asStateFlow()

    private val _saveState = MutableStateFlow(SaveState.IDLE)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val _lastSavedAt = MutableStateFlow(0L)
    val lastSavedAt: StateFlow<Long> = _lastSavedAt.asStateFlow()

    private val _saveConfirmState = MutableStateFlow(SaveConfirmState.NONE)
    val saveConfirmState: StateFlow<SaveConfirmState> = _saveConfirmState.asStateFlow()

    private val _isEditingInRender = MutableStateFlow(false)
    val isEditingInRender: StateFlow<Boolean> = _isEditingInRender.asStateFlow()

    private val _editingBlockIndex = MutableStateFlow(-1)
    val editingBlockIndex: StateFlow<Int> = _editingBlockIndex.asStateFlow()

    private val _editingBlockContent = MutableStateFlow("")
    val editingBlockContent: StateFlow<String> = _editingBlockContent.asStateFlow()

    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _cursorInfo = MutableStateFlow(CursorInfo())
    val cursorInfo: StateFlow<CursorInfo> = _cursorInfo.asStateFlow()

    private val _isFormatPainterActive = MutableStateFlow(false)
    val isFormatPainterActive: StateFlow<Boolean> = _isFormatPainterActive.asStateFlow()

    private val _copiedFormat = MutableStateFlow<String?>(null)

    private val _scrollToLine = MutableStateFlow(-1)
    val scrollToLine: StateFlow<Int> = _scrollToLine.asStateFlow()

    private val _scrollToAnchor = MutableStateFlow<String?>(null)
    val scrollToAnchor: StateFlow<String?> = _scrollToAnchor.asStateFlow()

    private val _actionFeedback = MutableStateFlow<String?>(null)
    val actionFeedback: StateFlow<String?> = _actionFeedback.asStateFlow()

    private val _highlightedLine = MutableStateFlow<Int?>(null)
    val highlightedLine: StateFlow<Int?> = _highlightedLine.asStateFlow()

    private val _desiredCursor = MutableStateFlow<Int?>(null)
    val desiredCursor: StateFlow<Int?> = _desiredCursor.asStateFlow()

    private val _desiredSelectionEnd = MutableStateFlow<Int?>(null)
    val desiredSelectionEnd: StateFlow<Int?> = _desiredSelectionEnd.asStateFlow()

    fun consumeDesiredCursor(): Int? {
        val pos = _desiredCursor.value
        _desiredCursor.value = null
        return pos
    }

    fun requestDesiredCursor(position: Int) {
        _desiredCursor.value = position
    }

    fun consumeDesiredSelectionEnd(): Int? {
        val pos = _desiredSelectionEnd.value
        _desiredSelectionEnd.value = null
        return pos
    }

    val charCount: StateFlow<Int> = _content.map { it.length }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val autoSave = settingsRepository.autoSave
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val autoSaveInterval = settingsRepository.autoSaveInterval
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val autoSaveIntervalUnit = settingsRepository.autoSaveIntervalUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "seconds")

    val showLineNumbers = settingsRepository.showLineNumbers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val highlightCurrentLine = settingsRepository.highlightCurrentLine
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val lineWrap = settingsRepository.lineWrap
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "soft")

    val fontSize = settingsRepository.fontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 14)

    val editorFont = settingsRepository.editorFont
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val codeFont = settingsRepository.codeFont
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "jetbrains_mono")

    val versionSnapshot = settingsRepository.versionSnapshot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoIndent = settingsRepository.autoIndent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val bracketMatching = settingsRepository.bracketMatching
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val highlightLineColor = settingsRepository.highlightLineColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#4C6EF5")

    val indentType = settingsRepository.indentType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "spaces")

    val indentSize = settingsRepository.indentSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)

    val spellCheck = settingsRepository.spellCheck
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val syncScroll = settingsRepository.syncScroll
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val lineHeight = settingsRepository.lineHeight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.7f)

    val letterSpacing = settingsRepository.letterSpacing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0f)

    val paragraphSpacing = settingsRepository.paragraphSpacing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val markdownDialect = settingsRepository.markdownDialect
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "gfm")

    private var autoSaveJob: Job? = null
    private var lastSavedContent = ""

    private val _autoSaveRemainingSeconds = MutableStateFlow(0)
    val autoSaveRemainingSeconds: StateFlow<Int> = _autoSaveRemainingSeconds.asStateFlow()

    private val _restoreChannel = Channel<String>()
    val restoreEvent = _restoreChannel.receiveAsFlow()

    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()
    private val maxUndoSize = 50
    private var lastUndoSnapshot = ""

    init {
        val loadStartTime = System.currentTimeMillis()

        fileId?.let { id ->
            viewModelScope.launch {
                fileRepository.getFileById(id)?.let { file ->
                    _fileName.value = file.name
                    Timber.tag("Editor").d("打开文件：${file.name} (id=${file.id})")
                    val content = if (file.content.isNotEmpty()) {
                        file.content
                    } else {
                        readContentFromPath(file)
                    }
                    _content.value = content

                    // 动态骨架屏时长：跟随文件大小和实际加载耗时
                    // - 最小显示 300ms（防闪烁，KakaoPay 最佳实践）
                    // - 根据内容大小增加渲染缓冲（大文件编辑器渲染耗时更长）
                    // - 实际加载耗时自然覆盖，不截断
                    val minDisplayTime = 300L
                    val renderBuffer = when {
                        content.length > 500_000 -> 400L   // 超大文件
                        content.length > 100_000 -> 250L   // 大文件
                        content.length > 50_000 -> 150L    // 中大文件
                        content.length > 10_000 -> 80L     // 中等文件
                        else -> 0L                          // 小文件
                    }
                    val totalWait = minDisplayTime + renderBuffer
                    val elapsed = System.currentTimeMillis() - loadStartTime
                    if (elapsed < totalWait) {
                        delay(totalWait - elapsed)
                    }

                    _isLoading.value = false
                    Timber.tag("Editor").d("文件内容加载完成：${_fileName.value}，大小：${content.length}字符，耗时：${elapsed}ms")
                    lastSavedContent = content
                    lastUndoSnapshot = content
                    _isModified.value = false
                    recordAccess(file.id)
                } ?: run {
                    val elapsed = System.currentTimeMillis() - loadStartTime
                    if (elapsed < 300L) delay(300L - elapsed)
                    _isLoading.value = false
                }
            }
        } ?: run {
            // 新建文件，无需骨架屏
            _isLoading.value = false
        }

        viewModelScope.launch {
            autoSave.collect { enabled ->
                autoSaveJob?.cancel()
                if (enabled) startAutoSaveTimer()
            }
        }

        // 间隔或单位变化时重启定时器
        viewModelScope.launch {
            combine(autoSaveInterval, autoSaveIntervalUnit) { interval, unit ->
                interval to unit
            }.collect {
                if (autoSave.value) {
                    startAutoSaveTimer()
                }
            }
        }
    }

    fun updateContent(newContent: String) {
        pushUndo()
        _content.value = newContent
        _isModified.value = true
        refreshSearchMatches()
    }

    fun updateCursorInfo(selectionStart: Int, selectionEnd: Int) {
        val text = _content.value
        val start = selectionStart.coerceIn(0, text.length)
        val end = selectionEnd.coerceIn(0, text.length)
        val textBeforeCursor = text.substring(0, start)
        val line = textBeforeCursor.count { it == '\n' } + 1
        val lastNewline = textBeforeCursor.lastIndexOf('\n')
        val column = start - lastNewline
        val hasSelection = start != end
        val selectedText = if (hasSelection) text.substring(start, end) else ""

        _cursorInfo.value = CursorInfo(
            line = line,
            column = column,
            selectionStart = start,
            selectionEnd = end,
            hasSelection = hasSelection,
            selectedText = selectedText,
        )
    }

    fun requestScrollToLine(line: Int) {
        _scrollToLine.value = line
    }

    fun requestScrollToAnchor(anchor: String) {
        _scrollToAnchor.value = anchor
    }

    fun consumeScrollToAnchor() {
        _scrollToAnchor.value = null
    }

    fun consumeScrollToLine(): Int {
        val line = _scrollToLine.value
        _scrollToLine.value = -1
        return line
    }

    fun toggleEditorMode() {
        _editorMode.value = when (_editorMode.value) {
            EditorMode.PLAIN -> EditorMode.RENDER
            EditorMode.RENDER -> EditorMode.PLAIN
        }
        if (_editorMode.value == EditorMode.RENDER) {
            Timber.tag("Editor").d("切换到渲染模式：${_fileName.value}")
        }
        _isEditingInRender.value = false
    }

    fun setEditorMode(mode: EditorMode) {
        _editorMode.value = mode
        _isEditingInRender.value = false
    }

    fun startEditingInRender() {
        _isEditingInRender.value = true
    }

    fun stopEditingInRender() {
        _isEditingInRender.value = false
    }

    fun startEditingBlock(blockIndex: Int, blockContent: String) {
        _editingBlockIndex.value = blockIndex
        _editingBlockContent.value = blockContent
        _isEditingInRender.value = true
    }

    fun updateEditingBlockContent(newContent: String) {
        _editingBlockContent.value = newContent
    }

    fun finishEditingBlock() {
        val blockIndex = _editingBlockIndex.value
        val blockContent = _editingBlockContent.value
        if (blockIndex >= 0) {
            val lines = _content.value.lines()
            if (blockIndex < lines.size) {
                val newLines = lines.toMutableList()
                newLines[blockIndex] = blockContent
                _content.value = newLines.joinToString("\n")
                _isModified.value = true
            }
        }
        _isEditingInRender.value = false
        _editingBlockIndex.value = -1
        _editingBlockContent.value = ""
    }

    fun cancelEditingBlock() {
        _isEditingInRender.value = false
        _editingBlockIndex.value = -1
        _editingBlockContent.value = ""
    }

    fun save() {
        viewModelScope.launch {
            _saveState.value = SaveState.SAVING
            if (_content.value.length > 10000) {
                Timber.tag("Editor").w("文件较大（超过10000字符）：${_fileName.value}，${_content.value.length}字符")
            }
            try {
                fileId?.let { id ->
                    fileRepository.getFileById(id)?.let { file ->
                        val updatedFile = file.copy(
                            updatedAt = System.currentTimeMillis(),
                            size = _content.value.toByteArray().size.toLong(),
                            content = _content.value,
                        )
                        fileRepository.updateFile(updatedFile)

                        // 同步内容到磁盘文件
                        syncContentToDiskFile(updatedFile)
                    }
                }
                lastSavedContent = _content.value
                _isModified.value = false
                _saveState.value = SaveState.SAVED
                _lastSavedAt.value = System.currentTimeMillis()
                _saveConfirmState.value = SaveConfirmState.SAVED
                Timber.tag("Editor").i("文件已保存：${_fileName.value}")
                createVersionSnapshot()
                delay(200)
                _saveConfirmState.value = SaveConfirmState.NONE
                delay(1800)
                if (_saveState.value == SaveState.SAVED) {
                    _saveState.value = SaveState.IDLE
                }
            } catch (e: Exception) {
                Timber.tag("Editor").e(e, "文件保存失败：${_fileName.value}")
                _saveState.value = SaveState.ERROR
                _saveConfirmState.value = SaveConfirmState.ERROR
                viewModelScope.launch {
                    delay(200)
                    _saveConfirmState.value = SaveConfirmState.NONE
                }
            }
        }
    }

    private suspend fun syncContentToDiskFile(file: FileEntity) {
        val path = file.path
        if (path.isBlank()) return

        when {
            // SAF URI 路径 (content://...)
            path.startsWith("content://") -> {
                try {
                    val uri = Uri.parse(path)
                    fileOperations.writeFileContent(uri, file.content)
                } catch (e: Exception) { Timber.tag("Editor").e(e, "同步内容到SAF URI失败：$path") }
            }
            // 绝对路径 (工作区路径，如 /storage/emulated/0/...)
            path.startsWith("/") -> {
                try {
                    val diskFile = java.io.File(path)
                    if (diskFile.exists() && diskFile.canWrite()) {
                        diskFile.writeText(file.content)
                    }
                } catch (e: Exception) { Timber.tag("Editor").e(e, "同步内容到磁盘文件失败：$path") }
            }
            // 虚拟路径 (documents/xxx.md) - 仅存在于数据库中，无需写入磁盘
        }
    }

    private suspend fun readContentFromPath(file: FileEntity): String {
        val path = file.path
        if (path.isBlank()) return ""
        return try {
            when {
                path.startsWith("content://") -> fileOperations.readFileContent(Uri.parse(path))
                path.startsWith("/") -> {
                    val diskFile = java.io.File(path)
                    if (diskFile.exists() && diskFile.canRead()) diskFile.readText() else ""
                }
                else -> ""
            }
        } catch (e: Exception) {
            Timber.tag("Editor").e(e, "读取文件内容失败：$path")
            ""
        }
    }

    fun toggleFormatPainter() {
        _isFormatPainterActive.value = !_isFormatPainterActive.value
        if (!_isFormatPainterActive.value) {
            _copiedFormat.value = null
        }
    }

    fun copyFormat(formatText: String) {
        _copiedFormat.value = formatText
        _isFormatPainterActive.value = true
    }

    fun applyFormatPainter() {
        val format = _copiedFormat.value ?: return
        insertFormatting(format, format)
        _isFormatPainterActive.value = false
        _copiedFormat.value = null
    }

    fun insertFormatting(prefix: String, suffix: String, selStart: Int? = null, selEnd: Int? = null) {
        pushUndo()
        val current = _content.value
        val start = selStart ?: _cursorInfo.value.selectionStart
        val end = selEnd ?: _cursorInfo.value.selectionEnd
        val hasSelection = start != end

        if (hasSelection) {
            val before = current.substring(0, start)
            val selected = current.substring(start, end)
            val after = current.substring(end)
            _content.value = "$before$prefix$selected$suffix$after"
            _desiredCursor.value = start + prefix.length + selected.length
        } else {
            val placeholder = context.getString(R.string.placeholder_text)
            val before = current.substring(0, start)
            val after = current.substring(start)
            _content.value = "$before$prefix$placeholder$suffix$after"
            _desiredCursor.value = start + prefix.length
            _desiredSelectionEnd.value = start + prefix.length + placeholder.length
        }
        _isModified.value = true
        refreshSearchMatches()
    }

    fun insertLinePrefix(prefix: String, selStart: Int? = null, selEnd: Int? = null) {
        pushUndo()
        val current = _content.value
        val cursorPos = selStart ?: _cursorInfo.value.selectionStart

        val lineStart = current.lastIndexOf('\n', cursorPos - 1).let { if (it == -1) 0 else it + 1 }
        val lineEnd = current.indexOf('\n', cursorPos).let { if (it == -1) current.length else it }
        val lineContent = current.substring(lineStart, lineEnd)

        val headingPattern = Regex("""^#{1,6}\s""")
        val listPattern = Regex("""^[-*+]\s""")
        val orderedListPattern = Regex("""^\d+\.\s""")
        val taskListPattern = Regex("""^-\s\[[ x]\]\s""")
        val quotePattern = Regex("""^>\s""")

        val existingPrefix = when {
            taskListPattern.containsMatchIn(lineContent) -> taskListPattern.find(lineContent)?.value
            orderedListPattern.containsMatchIn(lineContent) -> orderedListPattern.find(lineContent)?.value
            listPattern.containsMatchIn(lineContent) -> listPattern.find(lineContent)?.value
            headingPattern.containsMatchIn(lineContent) -> headingPattern.find(lineContent)?.value
            quotePattern.containsMatchIn(lineContent) -> quotePattern.find(lineContent)?.value
            else -> null
        }

        if (existingPrefix != null) {
            val newLine = lineContent.removePrefix(existingPrefix)
            if (existingPrefix == prefix) {
                _content.value = current.substring(0, lineStart) + newLine + current.substring(lineEnd)
                _desiredCursor.value = (cursorPos - prefix.length).coerceAtLeast(lineStart)
            } else {
                _content.value = current.substring(0, lineStart) + prefix + newLine + current.substring(lineEnd)
                _desiredCursor.value = cursorPos - existingPrefix.length + prefix.length
            }
        } else {
            _content.value = current.substring(0, lineStart) + prefix + current.substring(lineStart)
            _desiredCursor.value = cursorPos + prefix.length
        }
        _isModified.value = true
        refreshSearchMatches()
    }

    fun insertAtCursor(text: String, selStart: Int? = null, selEnd: Int? = null) {
        pushUndo()
        val current = _content.value
        val start = selStart ?: _cursorInfo.value.selectionStart
        val end = selEnd ?: _cursorInfo.value.selectionEnd

        var insertText = text
        if (insertText.startsWith("\n") && start > 0 && current.getOrNull(start - 1) != '\n') {
        } else if (!insertText.startsWith("\n") && start > 0 && current.getOrNull(start - 1) != '\n') {
            insertText = "\n$insertText"
        }

        if (insertText.endsWith("\n") && start < current.length && current.getOrNull(start) != '\n') {
        } else if (!insertText.endsWith("\n") && start < current.length && current.getOrNull(start) != '\n') {
            insertText = "$insertText\n"
        }

        val before = current.substring(0, start)
        val after = current.substring(end.coerceAtLeast(start))
        _content.value = "$before$insertText$after"
        _desiredCursor.value = before.length + insertText.length
        _isModified.value = true
        refreshSearchMatches()
    }

    fun insertLink(selStart: Int? = null, selEnd: Int? = null) {
        pushUndo()
        val current = _content.value
        val start = selStart ?: _cursorInfo.value.selectionStart
        val end = selEnd ?: _cursorInfo.value.selectionEnd
        val hasSelection = start != end
        val defaultUrl = "https://example.com"

        if (hasSelection) {
            val selectedText = current.substring(start, end)
            val linkText = "[$selectedText]($defaultUrl)"
            val before = current.substring(0, start)
            val after = current.substring(end)
            _content.value = "$before$linkText$after"
            // 选中 URL 部分：(url) 中的 url
            val urlStart = start + selectedText.length + 3 // "[text](" 的长度 = text.length + 3
            _desiredCursor.value = urlStart
            _desiredSelectionEnd.value = urlStart + defaultUrl.length
        } else {
            val linkText = "[${context.getString(R.string.placeholder_link_text)}]($defaultUrl)"
            val before = current.substring(0, start)
            val after = current.substring(start)
            _content.value = "$before$linkText$after"
            // 选中链接文字占位符
            _desiredCursor.value = start + 1
            _desiredSelectionEnd.value = start + 1 + context.getString(R.string.placeholder_link_text).length
        }
        _isModified.value = true
        refreshSearchMatches()
    }

    fun insertImage(url: String, description: String, selStart: Int? = null, selEnd: Int? = null) {
        Timber.tag("Editor").d("插入图片：${_fileName.value}，URL长度=${url.length}")
        pushUndo()
        val current = _content.value
        val start = selStart ?: _cursorInfo.value.selectionStart
        val end = selEnd ?: _cursorInfo.value.selectionEnd
        val hasSelection = start != end

        val desc = if (hasSelection) {
            current.substring(start, end)
        } else {
            description.ifEmpty { context.getString(R.string.image_desc_default) }
        }
        val imageText = "![$desc]($url)"
        val before = current.substring(0, start)
        val after = current.substring(end.coerceAtLeast(start))
        _content.value = "$before$imageText$after"
        // 选中 URL 部分
        val urlStart = start + desc.length + 4 // "![" + desc + "](" 的长度
        _desiredCursor.value = urlStart
        _desiredSelectionEnd.value = urlStart + url.length
        _isModified.value = true
        refreshSearchMatches()
    }

    /**
     * 插入本地图片：将图片复制到应用内部存储，确保重启后仍可访问
     */
    // ── 图床相关状态 ──
    val imageHostEnabled: StateFlow<Boolean> = settingsRepository.imageHostEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    /**
     * 上传图片到图床并插入 Markdown 链接
     */
    fun uploadImageToHost(sourceUri: Uri, description: String, selStart: Int? = null, selEnd: Int? = null) {
        viewModelScope.launch {
            _isUploadingImage.value = true
            _uploadError.value = null
            try {
                // 将图片复制到临时文件
                val tempFile = java.io.File(context.cacheDir, "upload_${System.currentTimeMillis()}")
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalStateException(context.getString(R.string.error_cannot_read_image))

                val fileName = sourceUri.lastPathSegment ?: "image_${System.currentTimeMillis()}.png"
                val result = imageUploadService.uploadImage(tempFile, fileName)

                // 清理临时文件
                tempFile.delete()

                if (result.success && result.url.isNotBlank()) {
                    insertImage(result.url, description, selStart, selEnd)
                    Timber.tag("Editor").i("图床上传成功：${_fileName.value}")
                } else {
                    Timber.tag("Editor").e("图床上传失败：${_fileName.value}，原因：${result.message}")
                    _uploadError.value = result.message.ifBlank { context.getString(R.string.upload_failed) }
                }
            } catch (e: Exception) {
                Timber.tag("Editor").e(e, "图床上传失败：${_fileName.value}")
                _uploadError.value = e.message ?: context.getString(R.string.upload_failed)
            } finally {
                _isUploadingImage.value = false
            }
        }
    }

    fun clearUploadError() {
        _uploadError.value = null
    }

    fun insertLocalImage(sourceUri: Uri, description: String, selStart: Int? = null, selEnd: Int? = null) {
        viewModelScope.launch {
            val imagesDir = java.io.File(context.filesDir, "images").apply { mkdirs() }
            // 根据源文件扩展名确定文件名
            val extension = guessImageExtension(sourceUri)
            val fileName = "img_${System.currentTimeMillis()}.$extension"
            val destFile = java.io.File(imagesDir, fileName)

            try {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                // 使用 file:// URI，确保 WebView 预览能正确加载本地图片
                insertImage("file://${destFile.absolutePath}", description, selStart, selEnd)
            } catch (e: Exception) {
                Timber.tag("Editor").w(e, "复制图片到本地存储失败，回退到content:// URI")
                // 复制失败时回退到原始 content:// URI，尝试获取持久权限
                try {
                    context.contentResolver.takePersistableUriPermission(
                        sourceUri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) { Timber.tag("Editor").w(e, "获取图片URI持久权限失败") }
                insertImage(sourceUri.toString(), description, selStart, selEnd)
            }
        }
    }

    private fun guessImageExtension(uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri) ?: ""
        return when {
            mimeType.contains("png", ignoreCase = true) -> "png"
            mimeType.contains("gif", ignoreCase = true) -> "gif"
            mimeType.contains("webp", ignoreCase = true) -> "webp"
            mimeType.contains("bmp", ignoreCase = true) -> "bmp"
            else -> "jpg"
        }
    }

    fun insertTable(rows: Int, cols: Int, selStart: Int? = null, selEnd: Int? = null) {
        pushUndo()
        val current = _content.value
        val start = selStart ?: _cursorInfo.value.selectionStart
        val end = selEnd ?: _cursorInfo.value.selectionEnd

        val header = "| ${List(cols) { i -> context.getString(R.string.column_n, i + 1) }.joinToString(" | ")} |"
        val separator = "| ${List(cols) { "-----" }.joinToString(" | ")} |"
        val dataRows = List(rows - 1) {
            "| ${List(cols) { context.getString(R.string.content) }.joinToString(" | ")} |"
        }.joinToString("\n")

        val before = current.substring(0, start)
        val after = current.substring(end)
        val needNewline = before.isNotEmpty() && !before.endsWith("\n")
        val prefix = if (needNewline) "\n" else ""
        val table = "$prefix$header\n$separator\n$dataRows\n"
        _content.value = "$before$table$after"
        _desiredCursor.value = start + table.length
        _isModified.value = true
        refreshSearchMatches()
    }

    fun insertCodeBlock(selStart: Int? = null, selEnd: Int? = null) {
        pushUndo()
        val current = _content.value
        val start = selStart ?: _cursorInfo.value.selectionStart
        val end = selEnd ?: _cursorInfo.value.selectionEnd
        val hasSelection = start != end

        if (hasSelection) {
            val before = current.substring(0, start)
            val selected = current.substring(start, end)
            val after = current.substring(end)
            val needLeadingNewline = start > 0 && current.getOrNull(start - 1) != '\n'
            val needTrailingNewline = end < current.length && current.getOrNull(end) != '\n'
            val leading = if (needLeadingNewline) "\n" else ""
            val trailing = if (needTrailingNewline) "\n" else ""
            val newText = "$leading```\n$selected\n```$trailing"
            _content.value = "$before$newText$after"
            _desiredCursor.value = before.length + newText.length
        } else {
            val before = current.substring(0, start)
            val after = current.substring(start)
            val needLeadingNewline = start > 0 && current.getOrNull(start - 1) != '\n'
            val needTrailingNewline = start < current.length && current.getOrNull(start) != '\n'
            val leading = if (needLeadingNewline) "\n" else ""
            val trailing = if (needTrailingNewline) "\n" else ""
            val newText = "$leading```\n${context.getString(R.string.placeholder_code)}\n```$trailing"
            _content.value = "$before$newText$after"
            _desiredCursor.value = before.length + leading.length + 5
            _desiredSelectionEnd.value = before.length + leading.length + 7
        }
        _isModified.value = true
        refreshSearchMatches()
    }

    fun clearFormat() {
        pushUndo()
        val current = _content.value
        val info = _cursorInfo.value
        val target = if (info.hasSelection) {
            current.substring(info.selectionStart, info.selectionEnd)
        } else {
            current
        }
        val cleaned = target
            .replace(Regex("""[*_~]{1,2}"""), "")
            .replace(Regex("""=="""), "")
            .replace(Regex("""`{1,3}"""), "")
            .replace(Regex("""^#{1,6}\s""", RegexOption.MULTILINE), "")
            .replace(Regex("""^>\s""", RegexOption.MULTILINE), "")
            .replace(Regex("""^[-*+]\s""", RegexOption.MULTILINE), "")
            .replace(Regex("""^\d+\.\s""", RegexOption.MULTILINE), "")
            .replace(Regex("""^-\s\[[ x]\]\s""", RegexOption.MULTILINE), "")
            .replace(Regex("""<u>"""), "")
            .replace(Regex("""</u>"""), "")
            .replace(Regex("""\[([^\]]+)\]\([^)]+\)"""), "$1")
            .replace(Regex("""!\[([^\]]*)\]\([^)]+\)"""), "$1")

        if (info.hasSelection) {
            val before = current.substring(0, info.selectionStart)
            val after = current.substring(info.selectionEnd)
            _content.value = "$before$cleaned$after"
        } else {
            _content.value = cleaned
        }
        _isModified.value = true
        refreshSearchMatches()
    }

    fun getExportContent(format: ExportFormat): String {
        val dialect = MarkdownDialect.fromKey(markdownDialect.value)
        return when (format) {
            ExportFormat.MARKDOWN -> _content.value
            ExportFormat.PLAIN_TEXT -> stripMarkdownFormatting(_content.value)
            ExportFormat.HTML -> {
                val html = MarkdownRenderer.renderToHtml(_content.value, dialect)
                MarkdownRenderer.wrapHtml(html, dialect = dialect)
            }
            ExportFormat.PDF -> {
                val html = MarkdownRenderer.renderToHtml(_content.value, dialect)
                MarkdownRenderer.wrapHtml(html, dialect = dialect)
            }
            ExportFormat.DOCX -> _content.value
            ExportFormat.IMAGE_PNG -> _content.value
            ExportFormat.ZIP -> _content.value
        }
    }

    /**
     * 将 Markdown 内容导出为 DOCX 格式到指定输出流
     */
    fun exportDocx(outputStream: java.io.OutputStream) {
        Timber.tag("Editor").i("导出 DOCX：${_fileName.value}")
        com.mdcito.app.markdown.DocxExporter.export(_content.value, outputStream, _fileName.value)
    }

    /**
     * 将 HTML 内容导出为 PDF 格式
     * 使用 WebView + Android PrintManager，确保与预览模式所见即所得。
     * 通过系统打印服务生成矢量 PDF，弹出打印对话框供用户保存。
     *
     * @param htmlContent 完整的 HTML 文档（含 CSS 样式和主题颜色）
     * @param activityContext Activity 级别的 Context（PrintManager 要求）
     * @param jobName 打印任务名称
     * @param onReady WebView 渲染完成、打印对话框即将弹出时回调
     * @param onComplete 导出完成回调（包括用户取消时）
     * @param onError 导出失败回调
     */
    fun exportPdf(
        htmlContent: String,
        activityContext: Context,
        jobName: String,
        onReady: () -> Unit = {},
        onComplete: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val exporter = com.mdcito.app.markdown.PdfExporter(activityContext)
        Timber.tag("Editor").i("导出 PDF：${_fileName.value}")
        exporter.export(htmlContent, jobName, onReady, onComplete, onError)
    }

    /**
     * 去除 Markdown 格式标记，返回纯文本
     */
    private fun stripMarkdownFormatting(markdown: String): String {
        var text = markdown
        // 移除代码块
        text = text.replace(Regex("```[\\s\\S]*?```"), "")
        // 移除图片链接
        text = text.replace(Regex("!\\[.*?]\\(.*?\\)"), "")
        // 保留链接文本
        text = text.replace(Regex("\\[([^\\]]*)]\\([^)]*\\)"), "$1")
        // 移除标题标记
        text = text.replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
        // 移除粗体/斜体标记
        text = text.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        text = text.replace(Regex("\\*(.+?)\\*"), "$1")
        // 移除删除线
        text = text.replace(Regex("~~(.+?)~~"), "$1")
        // 移除行内代码
        text = text.replace(Regex("`([^`]+)`"), "$1")
        // 移除引用标记
        text = text.replace(Regex("^>\\s+", RegexOption.MULTILINE), "")
        // 移除列表标记
        text = text.replace(Regex("^[*+-]\\s+", RegexOption.MULTILINE), "")
        text = text.replace(Regex("^\\d+\\.\\s+", RegexOption.MULTILINE), "")
        // 移除水平线
        text = text.replace(Regex("^[-*_]{3,}\\s*$", RegexOption.MULTILINE), "")
        return text.trim()
    }

    fun getWordCount(): Int = _content.value.split(Regex("\\s+")).filter { it.isNotBlank() }.size

    fun getLineCount(): Int = _content.value.lines().size

    fun getParagraphCount(): Int = _content.value.split(Regex("\n\\s*\n")).filter { it.isNotBlank() }.size

    /**
     * 计算预计阅读时间（分钟）
     * - 中文阅读速度：约 350 字/分钟
     * - 英文阅读速度：约 238 词/分钟
     * - 排除代码块、图片链接等非正文内容
     * - 结果四舍五入取整，最少显示 1 分钟
     */
    fun getReadingTimeMinutes(): Int {
        val content = _content.value
        if (content.isBlank()) return 1

        // 移除代码块（```...```）
        var text = content.replace(Regex("```[\\s\\S]*?```"), "")
        // 移除行内代码（`...`）
        text = text.replace(Regex("`[^`]+`"), "")
        // 移除图片链接 ![alt](url)
        text = text.replace(Regex("!\\[.*?]\\(.*?\\)"), "")
        // 移除链接中的 URL 部分，保留文本
        text = text.replace(Regex("\\[.*?]\\(.*?\\)"), "")
        // 移除 HTML 标签
        text = text.replace(Regex("<[^>]+>"), "")
        // 移除 Markdown 标记符号（#、*、-、> 等）
        text = text.replace(Regex("[#*\\->`~_|]"), "")

        // 统计中文字符数
        val chineseChars = text.count { it.code in 0x4E00..0x9FFF || it.code in 0x3400..0x4DBF }
        // 统计英文单词数（移除中文后）
        val textWithoutChinese = text.replace(Regex("[\\u4e00-\\u9fff\\u3400-\\u4dbf]"), " ")
        val englishWords = textWithoutChinese.split(Regex("\\s+")).filter { it.isNotBlank() && it.length > 1 }.size

        // 中文 350 字/分钟，英文 238 词/分钟
        val minutes = (chineseChars / 350.0) + (englishWords / 238.0)

        return maxOf(1, Math.round(minutes).toInt())
    }

    fun updateSearchQuery(query: String) {
        _searchState.value = _searchState.value.copy(query = query)
        refreshSearchMatches()
    }

    fun updateReplaceQuery(query: String) {
        _searchState.value = _searchState.value.copy(replaceQuery = query)
    }

    fun toggleCaseSensitive() {
        _searchState.value = _searchState.value.copy(
            isCaseSensitive = !_searchState.value.isCaseSensitive
        )
        refreshSearchMatches()
    }

    fun toggleRegex() {
        _searchState.value = _searchState.value.copy(
            isRegex = !_searchState.value.isRegex
        )
        refreshSearchMatches()
    }

    fun toggleWholeWord() {
        _searchState.value = _searchState.value.copy(
            isWholeWord = !_searchState.value.isWholeWord
        )
        refreshSearchMatches()
    }

    fun goToNextMatch() {
        val state = _searchState.value
        if (state.matches.isEmpty()) return
        val nextIndex = if (state.currentMatchIndex < state.matches.size - 1)
            state.currentMatchIndex + 1 else 0
        _searchState.value = state.copy(currentMatchIndex = nextIndex)
    }

    fun goToPreviousMatch() {
        val state = _searchState.value
        if (state.matches.isEmpty()) return
        val prevIndex = if (state.currentMatchIndex > 0)
            state.currentMatchIndex - 1 else state.matches.size - 1
        _searchState.value = state.copy(currentMatchIndex = prevIndex)
    }

    fun replaceCurrent() {
        val state = _searchState.value
        val match = state.currentMatch ?: return
        val content = _content.value
        if (match.first <= content.length && match.last <= content.length) {
            pushUndo()
            _content.value = content.substring(0, match.first) +
                    state.replaceQuery +
                    content.substring(match.last + 1)
            _isModified.value = true
            refreshSearchMatches()
        }
    }

    fun replaceAll() {
        val state = _searchState.value
        if (state.matches.isEmpty()) return
        val regex = buildSearchRegex(state) ?: return
        pushUndo()
        // 使用 lambda 重载避免 replacement 中的 $ 被解释为反向引用
        _content.value = regex.replace(_content.value) { state.replaceQuery }
        _isModified.value = true
        refreshSearchMatches()
    }

    fun addToSearchHistory(query: String) {
        if (query.isBlank()) return
        val current = _searchState.value.searchHistory
        val updated = (listOf(query) + current.filter { it != query }).take(5)
        _searchState.value = _searchState.value.copy(searchHistory = updated)
    }

    fun selectHistoryItem(query: String) {
        _searchState.value = _searchState.value.copy(query = query)
        refreshSearchMatches()
    }

    fun clearSearch() {
        _searchState.value = SearchState(searchHistory = _searchState.value.searchHistory)
    }

    fun deleteSearchHistoryItem(query: String) {
        val updated = _searchState.value.searchHistory.filter { it != query }
        _searchState.value = _searchState.value.copy(searchHistory = updated)
    }

    fun clearSearchHistory() {
        _searchState.value = _searchState.value.copy(searchHistory = emptyList())
    }

    fun goToMatchAtIndex(matchIndex: Int) {
        val state = _searchState.value
        if (matchIndex < 0 || matchIndex >= state.matches.size) return
        _searchState.value = state.copy(currentMatchIndex = matchIndex)
    }

    fun undo() {
        Timber.tag("Editor").d("撤销操作：${_fileName.value}，undo栈大小=${undoStack.size}")
        if (undoStack.isEmpty()) return
        val current = _content.value
        redoStack.add(current)
        val previous = undoStack.removeAt(undoStack.size - 1)
        _content.value = previous
        lastUndoSnapshot = previous
        _isModified.value = true
        refreshSearchMatches()
        showActionFeedback(context.getString(R.string.undo_done))
    }

    fun redo() {
        Timber.tag("Editor").d("重做操作：${_fileName.value}，redo栈大小=${redoStack.size}")
        if (redoStack.isEmpty()) return
        val current = _content.value
        undoStack.add(current)
        val next = redoStack.removeAt(redoStack.size - 1)
        _content.value = next
        lastUndoSnapshot = next
        _isModified.value = true
        refreshSearchMatches()
        showActionFeedback(context.getString(R.string.redo_done))
    }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun showActionFeedback(message: String) {
        _actionFeedback.value = message
        viewModelScope.launch {
            delay(1500)
            _actionFeedback.value = null
        }
    }

    fun flashLineHighlight(line: Int) {
        _highlightedLine.value = line
        viewModelScope.launch {
            delay(600)
            _highlightedLine.value = null
        }
    }

    fun findCodeBlocks(text: String): List<IntRange> {
        val lines = text.lines()
        val blocks = mutableListOf<IntRange>()
        var blockStart = -1
        for ((index, line) in lines.withIndex()) {
            if (line.trimStart().startsWith("```")) {
                if (blockStart < 0) {
                    blockStart = index
                } else {
                    blocks.add(blockStart..index)
                    blockStart = -1
                }
            }
        }
        if (blockStart >= 0) {
            blocks.add(blockStart..lines.lastIndex)
        }
        return blocks
    }

    /**
     * 根据当前行内容计算自动缩进
     * @param content 当前文本内容
     * @param cursorPosition 光标位置
     * @param indentType 缩进类型 ("spaces" 或 "tabs")
     * @param indentSize 缩进大小（空格数）
     * @return 需要插入的缩进字符串
     */
    fun calculateAutoIndent(content: String, cursorPosition: Int, indentType: String, indentSize: Int): String {
        val textBeforeCursor = content.substring(0, cursorPosition)
        val currentLine = textBeforeCursor.substringAfterLast('\n')

        // 提取当前行的前导空白
        val leadingWhitespace = currentLine.takeWhile { it == ' ' || it == '\t' }

        // 检查当前行是否需要额外缩进（如以 { 或 : 结尾）
        val trimmedLine = currentLine.trimEnd()
        val needsExtraIndent = trimmedLine.endsWith("{") ||
            trimmedLine.endsWith(":") ||
            trimmedLine.endsWith("(") ||
            trimmedLine.endsWith("[")

        val baseIndent = if (indentType == "tabs") "\t" else " ".repeat(indentSize)
        val extraIndent = if (needsExtraIndent) baseIndent else ""

        return leadingWhitespace + extraIndent
    }

    /**
     * 处理自动缩进的换行
     */
    fun handleAutoIndentEnter(cursorPosition: Int, indentType: String, indentSize: Int): String {
        val content = _content.value
        return calculateAutoIndent(content, cursorPosition, indentType, indentSize)
    }

    /**
     * 查找匹配的括号位置
     * @param content 文本内容
     * @param position 当前位置
     * @return 匹配括号的位置，如果没有匹配则返回 null
     */
    fun findMatchingBracket(content: String, position: Int): Int? {
        if (position < 0 || position >= content.length) return null

        val char = content[position]
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
        val range = if (searchForward) {
            position..content.lastIndex
        } else {
            position downTo 0
        }

        for (i in range) {
            when (content[i]) {
                openBracket -> depth++
                closeBracket -> depth--
            }
            if (depth == 0 && i != position) return i
        }

        return null
    }

    /**
     * 处理括号自动补全
     * @param char 输入的字符
     * @param cursorPosition 光标位置
     * @return 如果需要自动补全，返回补全后的文本和新的光标位置；否则返回 null
     */
    fun handleBracketAutoComplete(char: Char, cursorPosition: Int): Pair<String, Int>? {
        val closingBracket = when (char) {
            '(' -> ')'
            '[' -> ']'
            '{' -> '}'
            else -> return null
        }

        val content = _content.value
        val before = content.substring(0, cursorPosition)
        val after = content.substring(cursorPosition)
        val newContent = "$before$closingBracket$after"
        val newCursorPos = cursorPosition

        return Pair(newContent, newCursorPos)
    }

    private fun pushUndo() {
        val current = _content.value
        if (current != lastUndoSnapshot) {
            if (undoStack.size >= maxUndoSize) undoStack.removeAt(0)
            undoStack.add(lastUndoSnapshot)
            lastUndoSnapshot = current
            redoStack.clear()
        }
    }

    private fun startAutoSaveTimer() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            while (true) {
                val interval = autoSaveInterval.value
                val unit = autoSaveIntervalUnit.value
                val totalSeconds = when (unit) {
                    "minutes" -> interval * 60L
                    else -> interval.toLong()
                }
                _autoSaveRemainingSeconds.value = totalSeconds.toInt()
                for (i in totalSeconds downTo 1) {
                    _autoSaveRemainingSeconds.value = i.toInt()
                    delay(1000L)
                }
                if (_isModified.value && _content.value != lastSavedContent) {
                    Timber.tag("Editor").i("触发自动保存：${_fileName.value}")
                    save()
                }
            }
        }
    }

    fun createManualSnapshot() {
        viewModelScope.launch {
            createVersionSnapshot()
            Timber.tag("Editor").i("手动创建版本快照：${_fileName.value}")
        }
    }

    fun restoreFromSnapshot(snapshotContent: String) {
        Timber.tag("Editor").i("从版本快照恢复：${_fileName.value}")
        pushUndo()
        _content.value = snapshotContent
        _isModified.value = true
        refreshSearchMatches()
    }

    fun requestRestore(content: String) {
        viewModelScope.launch {
            _restoreChannel.send(content)
        }
    }

    private suspend fun recordAccess(fileId: Long) {
        historyRepository.recordAccess(fileId)
    }

    /**
     * 创建版本快照。使用 insertAndTrim 插入新快照并自动修剪旧快照，
     * 每文件默认保留最新 50 个快照（VersionRepository.DEFAULT_MAX_VERSIONS_PER_FILE），
     * 超出限制的旧快照会被自动删除。
     */
    private suspend fun createVersionSnapshot() {
        fileId?.let { id ->
            val snapshotEnabled = versionSnapshot.value
            if (snapshotEnabled) {
                versionRepository.insertAndTrim(
                    VersionEntity(
                        fileId = id,
                        filePath = "documents/${_fileName.value}",
                        size = _content.value.toByteArray().size.toLong(),
                        content = _content.value,
                    )
                )
            }
        }
    }

    private fun refreshSearchMatches() {
        val state = _searchState.value
        if (state.query.isEmpty()) {
            _searchState.value = state.copy(matches = emptyList(), currentMatchIndex = -1, matchedLines = emptyList())
            return
        }
        val regex = buildSearchRegex(state)
        if (regex == null) {
            _searchState.value = state.copy(matches = emptyList(), currentMatchIndex = -1, matchedLines = emptyList())
            return
        }
        val content = _content.value
        val matches = regex.findAll(content).map { it.range }.toList()
        val newIndex = if (matches.isNotEmpty()) 0 else -1

        // 计算匹配行信息
        val matchedLines = buildMatchedLines(content, matches)

        // 有匹配时自动记录搜索历史
        val updatedHistory = if (matches.isNotEmpty()) {
            (listOf(state.query) + state.searchHistory.filter { it != state.query }).take(10)
        } else {
            state.searchHistory
        }

        _searchState.value = state.copy(
            matches = matches,
            currentMatchIndex = newIndex,
            matchedLines = matchedLines,
            searchHistory = updatedHistory,
        )
    }

    private fun buildMatchedLines(content: String, matches: List<IntRange>): List<MatchedLine> {
        if (matches.isEmpty()) return emptyList()
        val lines = content.lines()
        val matchedLines = mutableListOf<MatchedLine>()
        var charOffset = 0

        for ((lineIndex, line) in lines.withIndex()) {
            val lineStart = charOffset
            val lineEnd = charOffset + line.length

            val matchIndicesInLine = mutableListOf<Int>()
            val matchRangesInLine = mutableListOf<IntRange>()

            matches.forEachIndexed { matchIndex, matchRange ->
                // 检查匹配是否与当前行有交集
                if (matchRange.first < lineEnd && matchRange.last >= lineStart) {
                    matchIndicesInLine.add(matchIndex)
                    // 计算在行内的相对范围
                    val relativeStart = (matchRange.first - lineStart).coerceAtLeast(0)
                    val relativeEnd = (matchRange.last - lineStart).coerceAtMost(line.length - 1)
                    if (relativeStart <= relativeEnd) {
                        matchRangesInLine.add(relativeStart..relativeEnd)
                    }
                }
            }

            if (matchIndicesInLine.isNotEmpty()) {
                matchedLines.add(MatchedLine(
                    lineNumber = lineIndex + 1,
                    lineContent = line,
                    matchRangesInLine = matchRangesInLine,
                    matchIndices = matchIndicesInLine,
                ))
            }

            charOffset = lineEnd + 1 // +1 for the newline character
        }

        return matchedLines
    }

    private fun buildSearchRegex(state: SearchState): Regex? {
        val query = state.query
        if (query.isEmpty()) return null

        return try {
            val pattern = if (state.isRegex) {
                query
            } else {
                Regex.escape(query).let { escaped ->
                    if (state.isWholeWord) "\\b$escaped\\b" else escaped
                }
            }
            val options = mutableSetOf<RegexOption>()
            if (!state.isCaseSensitive) options.add(RegexOption.IGNORE_CASE)
            Regex(pattern, options)
        } catch (e: Exception) {
            Timber.tag("Editor").w(e, "正则表达式编译失败：${state.query}")
            null
        }
    }
}
