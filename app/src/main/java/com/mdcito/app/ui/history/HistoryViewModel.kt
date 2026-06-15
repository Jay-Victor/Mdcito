package com.mdcito.app.ui.history

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdcito.app.R
import com.mdcito.app.data.db.entity.FileEntity
import com.mdcito.app.data.db.entity.HistoryEntity
import com.mdcito.app.data.files.FileOperations
import com.mdcito.app.data.repository.FileRepository
import com.mdcito.app.data.repository.HistoryRepository
import com.mdcito.app.ui.files.FileTypeFilter
import com.mdcito.app.ui.files.SortOrder
import com.mdcito.app.ui.files.TimeFilter
import com.mdcito.app.ui.files.PinFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.BufferOverflow
import javax.inject.Inject
import timber.log.Timber

enum class HistoryTab { FILES, FOLDERS }

data class HistoryBreadcrumbItem(val id: Long?, val name: String)

data class HistoryFilterState(
    val timeFilter: TimeFilter = TimeFilter.ALL,
    val fileTypeFilter: FileTypeFilter = FileTypeFilter.ALL,
    val pinFilter: PinFilter = PinFilter.ALL,
    val selectedTags: Set<String> = emptySet(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val application: Application,
    private val historyRepository: HistoryRepository,
    private val fileRepository: FileRepository,
    private val fileOperations: FileOperations,
) : ViewModel() {

    sealed class DeleteEvent {
        data class Success(val count: Int) : DeleteEvent()
        data class Error(val message: String) : DeleteEvent()
    }

    sealed class ImportEvent {
        data class Success(val fileName: String) : ImportEvent()
        data class Error(val message: String) : ImportEvent()
    }

    private val _deleteEvent = MutableSharedFlow<DeleteEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val deleteEvent: SharedFlow<DeleteEvent> = _deleteEvent

    private val _importEvent = MutableSharedFlow<ImportEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val importEvent: SharedFlow<ImportEvent> = _importEvent

    private val _selectedTab = MutableStateFlow(HistoryTab.FILES)
    val selectedTab: StateFlow<HistoryTab> = _selectedTab.asStateFlow()

    // 文件夹内导航状态
    private val _currentFolderId = MutableStateFlow<Long?>(null)
    val currentFolderId: StateFlow<Long?> = _currentFolderId.asStateFlow()

    private val _breadcrumbs = MutableStateFlow(listOf(HistoryBreadcrumbItem(null, application.getString(R.string.filter_folders))))
    val breadcrumbs: StateFlow<List<HistoryBreadcrumbItem>> = _breadcrumbs.asStateFlow()

    // 文件夹视图：当前文件夹内的子文件夹和文件
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val folderSubFolders: StateFlow<List<FileEntity>> = _currentFolderId
        .flatMapLatest { folderId ->
            if (folderId == null) {
                // 顶层：显示历史记录中的文件夹
                combine(historyRepository.getFolderHistoryLatest(), _searchQuery, _filterState) { histories, query, filter ->
                    var result = histories
                    if (query.isNotBlank()) {
                        result = result.filter { history ->
                            val name = _fileNameMap.value[history.fileId] ?: ""
                            name.contains(query, ignoreCase = true)
                        }
                    }
                    result = applyFilters(result, filter)
                    result.sortedByDescending { it.accessedAt }
                        .mapNotNull { _fileEntityMap.value[it.fileId] }
                }
            } else {
                // 进入某文件夹内部：显示该文件夹的子内容
                combine(
                    fileRepository.getFoldersByParent(folderId),
                    _searchQuery,
                    _sortOrder,
                    _filterState,
                ) { folders, query, order, filter ->
                    var result = folders
                    if (query.isNotBlank()) {
                        result = result.filter { it.name.contains(query, ignoreCase = true) }
                    }
                    result = applyFolderContentFilters(result, filter)
                    result.sortedWith(sortComparator(order))
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val folderSubFiles: StateFlow<List<FileEntity>> = _currentFolderId
        .flatMapLatest { folderId ->
            if (folderId == null) {
                // 顶层无文件
                MutableStateFlow(emptyList<FileEntity>())
            } else {
                combine(
                    fileRepository.getFilesByParent(folderId),
                    _searchQuery,
                    _sortOrder,
                    _filterState,
                ) { files, query, order, filter ->
                    var result = files
                    if (query.isNotBlank()) {
                        result = result.filter { it.name.contains(query, ignoreCase = true) }
                    }
                    result = applyFolderContentFilters(result, filter)
                    result.sortedWith(sortComparator(order))
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.TIME_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _filterState = MutableStateFlow(HistoryFilterState())
    val filterState: StateFlow<HistoryFilterState> = _filterState.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _isSelectMode = MutableStateFlow(false)
    val isSelectMode: StateFlow<Boolean> = _isSelectMode.asStateFlow()

    private val _fileNameMap = MutableStateFlow<Map<Long, String>>(emptyMap())
    val fileNameMap: StateFlow<Map<Long, String>> = _fileNameMap.asStateFlow()

    private val _fileEntityMap = MutableStateFlow<Map<Long, FileEntity>>(emptyMap())
    val fileEntityMap: StateFlow<Map<Long, FileEntity>> = _fileEntityMap.asStateFlow()

    val allTags: StateFlow<Set<String>> = fileRepository.getAll()
        .map { files -> files.flatMap { parseTags(it.tags) }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        viewModelScope.launch {
            fileRepository.getAll().collect { files ->
                _fileNameMap.value = files.associate { it.id to it.name }
                _fileEntityMap.value = files.associate { it.id to it }
            }
        }
    }

    val fileHistory: StateFlow<List<HistoryEntity>> = combine(
        historyRepository.getFileHistoryLatest(),
        _searchQuery,
        _filterState,
    ) { histories, query, filter ->
        var result = histories
        // 根级只显示不属于任何文件夹的文件（parentId 为 null）
        result = result.filter { history ->
            _fileEntityMap.value[history.fileId]?.parentId == null
        }
        if (query.isNotBlank()) {
            result = result.filter { history ->
                val name = _fileNameMap.value[history.fileId] ?: ""
                name.contains(query, ignoreCase = true)
            }
        }
        result = applyFilters(result, filter)
        result.sortedByDescending { it.accessedAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folderHistory: StateFlow<List<HistoryEntity>> = combine(
        historyRepository.getFolderHistoryLatest(),
        _searchQuery,
        _filterState,
    ) { histories, query, filter ->
        var result = histories
        // 根级只显示不属于任何文件夹的文件夹（parentId 为 null）
        result = result.filter { history ->
            _fileEntityMap.value[history.fileId]?.parentId == null
        }
        if (query.isNotBlank()) {
            result = result.filter { history ->
                val name = _fileNameMap.value[history.fileId] ?: ""
                name.contains(query, ignoreCase = true)
            }
        }
        result = applyFilters(result, filter)
        result.sortedByDescending { it.accessedAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getFileEntity(fileId: Long): FileEntity? = _fileEntityMap.value[fileId]

    fun selectTab(tab: HistoryTab) {
        _selectedTab.value = tab
        Timber.tag("History").d("切换标签页：$tab")
    }

    fun navigateToFolder(folderId: Long, folderName: String) {
        _currentFolderId.value = folderId
        _breadcrumbs.value = _breadcrumbs.value + HistoryBreadcrumbItem(folderId, folderName)
    }

    fun navigateUp(): Boolean {
        if (_breadcrumbs.value.size <= 1) return false
        Timber.tag("History").d("返回上级目录")
        _breadcrumbs.value = _breadcrumbs.value.dropLast(1)
        _currentFolderId.value = _breadcrumbs.value.last().id
        return true
    }

    fun navigateToBreadcrumb(index: Int) {
        if (index < 0 || index >= _breadcrumbs.value.size) return
        _breadcrumbs.value = _breadcrumbs.value.subList(0, index + 1)
        _currentFolderId.value = _breadcrumbs.value.last().id
        Timber.tag("History").d("面包屑导航：${_breadcrumbs.value.map { it.name }.joinToString("/")}")
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        Timber.tag("History").d("搜索历史记录：$query")
    }

    fun updateFilter(filter: HistoryFilterState) {
        _filterState.value = filter
        Timber.tag("History").d("筛选条件：$filter")
    }

    fun updateSortOrder(order: SortOrder) {
        _sortOrder.value = order
        Timber.tag("History").d("排序方式：$order")
    }

    fun resetFilter() {
        _filterState.value = HistoryFilterState()
        _sortOrder.value = SortOrder.TIME_DESC
    }

    fun toggleSelect(id: Long) {
        _selectedIds.value = if (_selectedIds.value.contains(id)) {
            _selectedIds.value - id
        } else {
            _selectedIds.value + id
        }
    }

    fun selectAll(ids: List<Long>) {
        _selectedIds.value = ids.toSet()
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
        _isSelectMode.value = false
    }

    fun enterSelectMode(firstId: Long) {
        _isSelectMode.value = true
        _selectedIds.value = setOf(firstId)
    }

    fun enterBatchSelectMode() {
        _isSelectMode.value = true
        _selectedIds.value = emptySet()
    }

    fun deleteHistory(history: HistoryEntity) {
        viewModelScope.launch {
            try {
                val fileEntity = fileRepository.getFileById(history.fileId)
                // 如果是导入文件，仅删除数据库引用记录，不删除系统文件
                if (fileEntity != null && fileEntity.isImported) {
                    // 同时清理历史记录和文件实体，不删除系统原始文件
                    historyRepository.deleteByFileId(history.fileId)
                    fileRepository.deleteFile(fileEntity)
                } else {
                    historyRepository.deleteByFileId(history.fileId)
                }
                _deleteEvent.emit(DeleteEvent.Success(1))
                Timber.tag("History").i("删除历史记录：fileId=${history.fileId}")
            } catch (e: Exception) {
                Timber.tag("History").e(e, "删除历史记录失败")
                _deleteEvent.emit(DeleteEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun deleteHistoryByIds(ids: List<Long>, fileIdMap: Map<Long, Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                // 清理导入文件的 FileEntity（不删系统文件）
                ids.forEach { id ->
                    val fileId = fileIdMap[id]
                    if (fileId != null) {
                        val fileEntity = fileRepository.getFileById(fileId)
                        if (fileEntity != null && fileEntity.isImported) {
                            fileRepository.deleteFile(fileEntity)
                        }
                    }
                }
                historyRepository.deleteByIds(ids)
                clearSelection()
                _deleteEvent.emit(DeleteEvent.Success(ids.size))
                Timber.tag("History").i("批量删除历史记录：${ids.size} 条")
            } catch (e: Exception) {
                Timber.tag("History").e(e, "批量删除历史记录失败")
                _deleteEvent.emit(DeleteEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            try {
                val isFilesTab = _selectedTab.value == HistoryTab.FILES
                val count = if (isFilesTab) {
                    val items = historyRepository.getFileHistoryLatest().first()
                    // 清理导入文件的 FileEntity（不删系统文件）
                    items.forEach { history ->
                        val fileEntity = fileRepository.getFileById(history.fileId)
                        if (fileEntity != null && fileEntity.isImported) {
                            fileRepository.deleteFile(fileEntity)
                        }
                    }
                    historyRepository.deleteFileHistory()
                    items.size
                } else {
                    val items = historyRepository.getFolderHistoryLatest().first()
                    // 清理导入文件的 FileEntity（不删系统文件）
                    items.forEach { history ->
                        val fileEntity = fileRepository.getFileById(history.fileId)
                        if (fileEntity != null && fileEntity.isImported) {
                            fileRepository.deleteFile(fileEntity)
                        }
                    }
                    historyRepository.deleteFolderHistory()
                    items.size
                }
                clearSelection()
                _deleteEvent.emit(DeleteEvent.Success(count))
                Timber.tag("History").i("清空历史记录：$count 条")
            } catch (e: Exception) {
                Timber.tag("History").e(e, "清空历史记录失败")
                _deleteEvent.emit(DeleteEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun renameFile(fileId: Long, newName: String) {
        viewModelScope.launch {
            val file = fileRepository.getFileById(fileId) ?: return@launch
            Timber.tag("History").i("重命名文件：${file.name} -> $newName")
            val oldPath = file.path
            val newPath = when {
                oldPath.startsWith("content://") -> {
                    try {
                        val newUri = fileOperations.renameDocument(Uri.parse(oldPath), newName)
                        newUri?.toString() ?: oldPath
                    } catch (e: Exception) { Timber.tag("History").w(e, "SAF URI重命名失败，保留旧路径"); oldPath }
                }
                oldPath.startsWith("documents/") -> "documents/$newName"
                else -> {
                    val oldFile = java.io.File(oldPath)
                    val parentDir = oldFile.parentFile
                    if (parentDir != null && oldFile.exists()) {
                        val newFile = java.io.File(parentDir, newName)
                        if (oldFile.renameTo(newFile)) newFile.absolutePath else oldPath
                    } else {
                        val parentPath = oldPath.substringBeforeLast("/", "")
                        if (parentPath.isNotEmpty()) "$parentPath/$newName" else newName
                    }
                }
            }
            fileRepository.updateFile(
                file.copy(name = newName, path = newPath, updatedAt = System.currentTimeMillis())
            )
        }
    }

    fun shareFile(fileId: Long, context: Context) {
        viewModelScope.launch {
            try {
                val file = fileRepository.getFileById(fileId) ?: return@launch
                Timber.tag("History").i("分享文件：${file.name}")
                val content = when {
                    file.path.startsWith("content://") -> fileOperations.readFileContent(Uri.parse(file.path))
                    file.path.startsWith("documents/") -> file.content
                    else -> {
                        val sysFile = java.io.File(file.path)
                        if (sysFile.exists()) sysFile.readText() else file.content
                    }
                }
                // Write content to a temporary file and share as file attachment
                val sharedDir = java.io.File(context.cacheDir, "shared").apply { mkdirs() }
                val tempFile = java.io.File(sharedDir, file.name).apply {
                    writeText(content)
                }
                val contentUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile,
                )
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    type = "*/*"
                }
                context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.share_file, file.name)))
            } catch (e: Exception) { Timber.tag("History").e(e, "分享文件失败") }
        }
    }

    fun addTagToFile(fileId: Long, tag: String) {
        viewModelScope.launch {
            val file = fileRepository.getFileById(fileId) ?: return@launch
            val currentTags = parseTags(file.tags).toMutableList()
            if (tag !in currentTags) {
                currentTags.add(tag)
                fileRepository.updateFile(file.copy(tags = formatTags(currentTags)))
            }
        }
    }

    fun removeTagFromFile(fileId: Long, tag: String) {
        Timber.tag("History").d("移除标签：$tag -> fileId=$fileId")
        viewModelScope.launch {
            val file = fileRepository.getFileById(fileId) ?: return@launch
            val currentTags = parseTags(file.tags).toMutableList()
            currentTags.remove(tag)
            fileRepository.updateFile(file.copy(tags = formatTags(currentTags)))
        }
    }

    fun addTagToSelectedFiles(tag: String, fileIdMap: Map<Long, Long>) {
        viewModelScope.launch {
            for (historyId in _selectedIds.value) {
                val fId = fileIdMap[historyId] ?: continue
                addTagToFile(fId, tag)
            }
        }
    }

    fun copyFile(fileEntity: FileEntity) {
        Timber.tag("History").i("复制文件：${fileEntity.name}")
        viewModelScope.launch {
            val copyName = generateCopyName(fileEntity.name)
            fileRepository.createFile(
                fileEntity.copy(
                    id = 0,
                    name = copyName,
                    path = "documents/$copyName",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isPinned = false,
                    parentId = _currentFolderId.value,
                )
            )
        }
    }

    fun deleteFileFromFolder(file: FileEntity) {
        viewModelScope.launch {
            try {
                if (!file.isImported) {
                    deleteSystemFile(file)
                }
                fileRepository.deleteFile(file)
                _deleteEvent.emit(DeleteEvent.Success(1))
            } catch (e: Exception) {
                _deleteEvent.emit(DeleteEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }

    private suspend fun deleteSystemFile(file: FileEntity) {
        try {
            val path = file.path
            when {
                path.startsWith("content://") -> {
                    try {
                        fileOperations.deleteDocument(Uri.parse(path))
                    } catch (e: Exception) { Timber.tag("History").e(e, "删除SAF文档失败：$path") }
                }
                path.startsWith("documents/") -> {
                    // virtual path, no system file
                }
                else -> {
                    val sysFile = java.io.File(path)
                    if (sysFile.exists()) {
                        sysFile.delete()
                    }
                }
            }
        } catch (e: Exception) { Timber.tag("History").e(e, "删除系统文件失败：${file.path}") }
    }

    private fun generateCopyName(originalName: String): String {
        val dotIndex = originalName.lastIndexOf('.')
        val baseName = if (dotIndex > 0) originalName.substring(0, dotIndex) else originalName
        val extension = if (dotIndex > 0) originalName.substring(dotIndex) else ""
        return "$baseName - ${application.getString(R.string.copy_suffix)}$extension"
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val items = if (_selectedTab.value == HistoryTab.FILES) fileHistory.value else folderHistory.value
            Timber.tag("History").d("刷新历史记录，共 ${items.size} 条")
            kotlinx.coroutines.delay(800)
            _isRefreshing.value = false
        }
    }

    fun importFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val fileName = fileOperations.getFileName(uri)
                if (fileName == null) {
                    _importEvent.emit(ImportEvent.Error(application.getString(R.string.import_file_failed)))
                    return@launch
                }
                val fileSize = fileOperations.getFileSize(uri)
                val content = fileOperations.readFileContent(uri)
                if (content.isEmpty() && fileSize > 0) {
                    _importEvent.emit(ImportEvent.Error(application.getString(R.string.import_file_read_failed)))
                    return@launch
                }
                val entity = FileEntity(
                    name = fileName,
                    path = uri.toString(),
                    type = "file",
                    size = fileSize,
                    tags = "[]",
                    content = content,
                    isImported = true,
                    parentId = _currentFolderId.value,
                )
                val id = fileRepository.createFile(entity)
                fileOperations.takePersistablePermission(uri)
                // 记录访问历史
                historyRepository.recordAccess(id)
                _importEvent.emit(ImportEvent.Success(fileName))
                Timber.tag("History").i("从历史页导入文件：$fileName")
            } catch (e: Exception) {
                Timber.tag("History").e(e, "从历史页导入文件失败")
                _importEvent.emit(ImportEvent.Error(e.message ?: application.getString(R.string.import_file_failed)))
            }
        }
    }

    fun importFolder(uri: Uri) {
        viewModelScope.launch {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                try {
                    application.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: Exception) { Timber.tag("History").w(e, "获取URI持久权限失败") }

                val dirName = try {
                    val docId = androidx.documentfile.provider.DocumentFile.fromTreeUri(application, uri)
                    docId?.name ?: uri.lastPathSegment ?: application.getString(R.string.custom_directory)
                } catch (e: Exception) {
                    Timber.tag("History").w(e, "获取文件夹名称失败")
                    uri.lastPathSegment ?: application.getString(R.string.custom_directory)
                }

                val entity = FileEntity(
                    name = dirName,
                    path = uri.toString(),
                    type = "folder",
                    isImported = true,
                    parentId = _currentFolderId.value,
                )
                val id = fileRepository.createFile(entity)
                historyRepository.recordAccess(id)
                _importEvent.emit(ImportEvent.Success(dirName))
                Timber.tag("History").i("从历史页导入文件夹：$dirName")
            } catch (e: Exception) {
                Timber.tag("History").e(e, "从历史页导入文件夹失败")
                _importEvent.emit(ImportEvent.Error(e.message ?: application.getString(R.string.import_file_failed)))
            }
        }
    }

    private fun applyFilters(histories: List<HistoryEntity>, filter: HistoryFilterState): List<HistoryEntity> {
        var result = histories
        val now = System.currentTimeMillis()
        when (filter.timeFilter) {
            TimeFilter.ALL -> {}
            TimeFilter.TODAY -> {
                val startOfDay = now - (now % 86_400_000L)
                result = result.filter { it.accessedAt >= startOfDay }
            }
            TimeFilter.THIS_WEEK -> {
                val startOfWeek = now - 7 * 86_400_000L
                result = result.filter { it.accessedAt >= startOfWeek }
            }
            TimeFilter.THIS_MONTH -> {
                val startOfMonth = now - 30 * 86_400_000L
                result = result.filter { it.accessedAt >= startOfMonth }
            }
            TimeFilter.THIS_YEAR -> {
                val startOfYear = now - 365 * 86_400_000L
                result = result.filter { it.accessedAt >= startOfYear }
            }
        }
        when (filter.fileTypeFilter) {
            FileTypeFilter.ALL -> {}
            FileTypeFilter.MD -> {
                result = result.filter { history ->
                    val name = _fileNameMap.value[history.fileId] ?: ""
                    name.endsWith(".md", ignoreCase = true)
                }
            }
            FileTypeFilter.TXT -> {
                result = result.filter { history ->
                    val name = _fileNameMap.value[history.fileId] ?: ""
                    name.endsWith(".txt", ignoreCase = true)
                }
            }
            FileTypeFilter.MARKDOWN -> {
                result = result.filter { history ->
                    val name = _fileNameMap.value[history.fileId] ?: ""
                    name.endsWith(".markdown", ignoreCase = true)
                }
            }
        }
        when (filter.pinFilter) {
            PinFilter.ALL -> {}
            PinFilter.PINNED_ONLY -> {
                result = result.filter { history ->
                    _fileEntityMap.value[history.fileId]?.isPinned == true
                }
            }
            PinFilter.UNPINNED_ONLY -> {
                result = result.filter { history ->
                    _fileEntityMap.value[history.fileId]?.isPinned != true
                }
            }
        }
        if (filter.selectedTags.isNotEmpty()) {
            result = result.filter { history ->
                val fileEntity = _fileEntityMap.value[history.fileId]
                val fileTags = fileEntity?.let { parseTags(it.tags) } ?: emptyList()
                filter.selectedTags.any { it in fileTags }
            }
        }
        return result
    }

    private fun applyFolderContentFilters(files: List<FileEntity>, filter: HistoryFilterState): List<FileEntity> {
        var result = files
        when (filter.fileTypeFilter) {
            FileTypeFilter.ALL -> {}
            FileTypeFilter.MD -> result = result.filter { it.name.endsWith(".md", ignoreCase = true) }
            FileTypeFilter.TXT -> result = result.filter { it.name.endsWith(".txt", ignoreCase = true) }
            FileTypeFilter.MARKDOWN -> result = result.filter { it.name.endsWith(".markdown", ignoreCase = true) }
        }
        val now = System.currentTimeMillis()
        when (filter.timeFilter) {
            TimeFilter.ALL -> {}
            TimeFilter.TODAY -> {
                val startOfDay = now - (now % 86_400_000L)
                result = result.filter { it.updatedAt >= startOfDay }
            }
            TimeFilter.THIS_WEEK -> {
                val startOfWeek = now - 7 * 86_400_000L
                result = result.filter { it.updatedAt >= startOfWeek }
            }
            TimeFilter.THIS_MONTH -> {
                val startOfMonth = now - 30 * 86_400_000L
                result = result.filter { it.updatedAt >= startOfMonth }
            }
            TimeFilter.THIS_YEAR -> {
                val startOfYear = now - 365 * 86_400_000L
                result = result.filter { it.updatedAt >= startOfYear }
            }
        }
        if (filter.selectedTags.isNotEmpty()) {
            result = result.filter { file ->
                val fileTags = parseTags(file.tags)
                filter.selectedTags.any { it in fileTags }
            }
        }
        return result
    }

    private fun sortComparator(order: SortOrder): Comparator<FileEntity> {
        val criteriaComparator: Comparator<FileEntity> = when (order) {
            SortOrder.NAME_ASC -> compareBy { it.name.lowercase() }
            SortOrder.NAME_DESC -> compareByDescending { it.name.lowercase() }
            SortOrder.TIME_ASC -> compareBy { it.updatedAt }
            SortOrder.TIME_DESC -> compareByDescending { it.updatedAt }
            SortOrder.SIZE_ASC -> compareBy { it.size }
            SortOrder.SIZE_DESC -> compareByDescending { it.size }
        }
        return criteriaComparator
    }

    companion object {
        fun parseTags(tagsJson: String): List<String> {
            return try {
                val arr = org.json.JSONArray(tagsJson)
                (0 until arr.length()).map { arr.getString(it) }
            } catch (e: Exception) {
                Timber.tag("History").w(e, "解析标签JSON失败")
                emptyList()
            }
        }

        fun formatTags(tags: List<String>): String {
            val arr = org.json.JSONArray()
            tags.forEach { arr.put(it) }
            return arr.toString()
        }
    }
}
