package com.mdcito.app.ui.files

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdcito.app.R
import com.mdcito.app.data.db.entity.FileEntity
import com.mdcito.app.data.files.FileOperations
import com.mdcito.app.data.repository.FileRepository
import com.mdcito.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.BufferOverflow
import org.json.JSONArray
import java.io.File
import javax.inject.Inject
import timber.log.Timber

enum class FileViewTab { FILES, FOLDERS }

enum class SortOrder(@param:StringRes val labelRes: Int) {
    NAME_ASC(R.string.sort_name_asc),
    NAME_DESC(R.string.sort_name_desc),
    TIME_ASC(R.string.sort_time_asc),
    TIME_DESC(R.string.sort_time_desc),
    SIZE_ASC(R.string.sort_size_asc),
    SIZE_DESC(R.string.sort_size_desc),
}

enum class FileTypeFilter(@param:StringRes val labelRes: Int) {
    ALL(R.string.filter_all), MD(R.string.file_ext_md), TXT(R.string.file_ext_txt), MARKDOWN(R.string.file_ext_markdown)
}

enum class TimeFilter(@param:StringRes val labelRes: Int) {
    ALL(R.string.filter_all), TODAY(R.string.filter_today), THIS_WEEK(R.string.filter_this_week), THIS_MONTH(R.string.filter_this_month), THIS_YEAR(R.string.filter_this_year)
}

enum class PinFilter(@param:StringRes val labelRes: Int) {
    ALL(R.string.filter_all), PINNED_ONLY(R.string.filter_pinned_only), UNPINNED_ONLY(R.string.filter_unpinned_only)
}

data class FilterState(
    val fileType: FileTypeFilter = FileTypeFilter.ALL,
    val timeFilter: TimeFilter = TimeFilter.ALL,
    val pinFilter: PinFilter = PinFilter.ALL,
    val selectedTags: Set<String> = emptySet(),
)

data class BreadcrumbItem(val id: Long?, val name: String)

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FilesViewModel @Inject constructor(
    private val application: Application,
    private val fileRepository: FileRepository,
    private val fileOperations: FileOperations,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    sealed class DeleteEvent {
        data class Success(val count: Int) : DeleteEvent()
        data class Error(val message: String) : DeleteEvent()
    }

    private val _deleteEvent = MutableSharedFlow<DeleteEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val deleteEvent: SharedFlow<DeleteEvent> = _deleteEvent

    val workspacePath: StateFlow<String?> = settingsRepository.workspacePath
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedTab = MutableStateFlow(FileViewTab.FILES)
    val selectedTab: StateFlow<FileViewTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.TIME_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _isSelectMode = MutableStateFlow(false)
    val isSelectMode: StateFlow<Boolean> = _isSelectMode.asStateFlow()

    val allTags: StateFlow<Set<String>> = fileRepository.getAll()
        .map { files -> files.filter { !it.isImported }.flatMap { parseTags(it.tags) }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _currentFolderId = MutableStateFlow<Long?>(null)
    val currentFolderId: StateFlow<Long?> = _currentFolderId.asStateFlow()

    val currentFolderEntity: StateFlow<FileEntity?> = _currentFolderId
        .flatMapLatest { folderId ->
            if (folderId == null) {
                kotlinx.coroutines.flow.flowOf(null)
            } else {
                fileRepository.getAll().map { files -> files.find { it.id == folderId } }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _breadcrumbs = MutableStateFlow(listOf(BreadcrumbItem(null, application.getString(R.string.all_files))))
    val breadcrumbs: StateFlow<List<BreadcrumbItem>> = _breadcrumbs.asStateFlow()

    val files: StateFlow<List<FileEntity>> = _currentFolderId
        .flatMapLatest { folderId ->
            val source = if (folderId == null) {
                fileRepository.getRootFiles()
            } else {
                fileRepository.getFilesByParent(folderId)
            }
            combine(source, _searchQuery, _sortOrder, _filterState) { files, query, order, filter ->
                var result = files.filter { !it.isImported }
                if (query.isNotBlank()) {
                    result = result.filter { it.name.contains(query, ignoreCase = true) }
                }
                result = applyFilters(result, filter)
                result.sortedWith(sortComparator(order))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<FileEntity>> = _currentFolderId
        .flatMapLatest { folderId ->
            val source = if (folderId == null) {
                fileRepository.getRootFolders()
            } else {
                fileRepository.getFoldersByParent(folderId)
            }
            combine(source, _searchQuery, _sortOrder, _filterState) { folders, query, order, filter ->
                var result = folders.filter { !it.isImported }
                if (query.isNotBlank()) {
                    result = result.filter { it.name.contains(query, ignoreCase = true) }
                }
                result = applyFilters(result, filter)
                result.sortedWith(sortComparator(order))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val itemCount: StateFlow<Int> = combine(files, folders) { f, d ->
        if (_selectedTab.value == FileViewTab.FILES) f.size else d.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun selectTab(tab: FileViewTab) {
        _selectedTab.value = tab
        Timber.tag("FilesVM").d("切换标签页：$tab")
    }

    fun updateSearchQuery(query: String) {
        Timber.tag("FilesVM").d("搜索文件：$query")
        _searchQuery.value = query
    }

    fun updateSortOrder(order: SortOrder) {
        Timber.tag("FilesVM").d("文件排序方式：$order")
        _sortOrder.value = order
    }

    fun updateFilter(filter: FilterState) {
        _filterState.value = filter
    }

    fun resetFilter() {
        _filterState.value = FilterState()
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

    fun exitSelectMode() {
        _isSelectMode.value = false
        _selectedIds.value = emptySet()
    }

    fun navigateToFolder(folderId: Long, folderName: String) {
        Timber.tag("FilesVM").d("进入目录：$folderName (id=$folderId)")
        _currentFolderId.value = folderId
        _breadcrumbs.value = _breadcrumbs.value + BreadcrumbItem(folderId, folderName)
    }

    fun navigateUp(): Boolean {
        if (_breadcrumbs.value.size <= 1) return false
        _breadcrumbs.value = _breadcrumbs.value.dropLast(1)
        _currentFolderId.value = _breadcrumbs.value.last().id
        return true
    }

    fun navigateToBreadcrumb(index: Int) {
        if (index < 0 || index >= _breadcrumbs.value.size) return
        _breadcrumbs.value = _breadcrumbs.value.subList(0, index + 1)
        _currentFolderId.value = _breadcrumbs.value.last().id
        Timber.tag("FilesVM").d("面包屑导航：${_breadcrumbs.value.map { it.name }.joinToString("/")}")
    }

    fun createFile(
        name: String,
        type: String = "file",
        tags: List<String> = emptyList(),
        parentId: Long? = null,
        saveUri: Uri? = null,
    ) {
        viewModelScope.launch {
            Timber.tag("FilesVM").i("创建文件：$name, 类型：$type")
            val effectiveParentId = parentId ?: _currentFolderId.value

            if (saveUri != null) {
                val resultUri = if (type == "folder") {
                    fileOperations.createFolderInDirectory(saveUri, name)
                } else {
                    val fileName = if (name.endsWith(".md")) name else "$name.md"
                    fileOperations.createFileInDirectory(saveUri, fileName)
                }
                if (resultUri == null) {
                    createFileInWorkspace(name, type, tags, effectiveParentId)
                } else {
                    fileOperations.takePersistablePermission(resultUri)
                    fileRepository.createFile(
                        FileEntity(
                            name = if (type == "folder") name else (if (name.endsWith(".md")) name else "$name.md"),
                            path = resultUri.toString(),
                            type = type,
                            tags = formatTags(tags),
                            parentId = effectiveParentId,
                        )
                    )
                }
            } else {
                createFileInWorkspace(name, type, tags, effectiveParentId)
            }
        }
    }

    private suspend fun createFileInWorkspace(
        name: String,
        type: String,
        tags: List<String>,
        parentId: Long?,
    ) {
        var wsPath = workspacePath.value
        val fileName = if (type == "folder") name else (if (name.endsWith(".md")) name else "$name.md")

        // 如果 workspacePath 未设置，自动初始化为默认路径 Documents/Mdcito
        if (wsPath == null) {
            val defaultDir = File(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS),
                "Mdcito"
            )
            if (!defaultDir.exists()) {
                defaultDir.mkdirs()
            }
            if (defaultDir.exists() && defaultDir.canWrite()) {
                settingsRepository.setWorkspacePath(defaultDir.absolutePath)
                wsPath = defaultDir.absolutePath
            }
        }

        if (wsPath != null) {
            if (wsPath.startsWith("content://")) {
                // SAF URI 路径：使用 DocumentFile API 创建文件
                val treeUri = Uri.parse(wsPath)
                try {
                    if (type == "folder") {
                        val folderUri = fileOperations.createFolderInDirectory(treeUri, name)
                        if (folderUri != null) {
                            fileRepository.createFile(
                                FileEntity(
                                    name = name,
                                    path = folderUri.toString(),
                                    type = type,
                                    tags = formatTags(tags),
                                    parentId = parentId,
                                )
                            )
                        } else {
                            createFileInAppStorage(name, type, tags, parentId)
                        }
                    } else {
                        val fileUri = fileOperations.createFileInDirectory(treeUri, fileName)
                        if (fileUri != null) {
                            fileRepository.createFile(
                                FileEntity(
                                    name = fileName,
                                    path = fileUri.toString(),
                                    type = type,
                                    size = 0L,
                                    tags = formatTags(tags),
                                    parentId = parentId,
                                )
                            )
                        } else {
                            createFileInAppStorage(name, type, tags, parentId)
                        }
                    }
                } catch (e: Exception) {
                    Timber.tag("FilesVM").w(e, "SAF创建文件失败，回退到应用存储")
                    createFileInAppStorage(name, type, tags, parentId)
                }
            } else {
                // 传统文件路径
                val wsDir = File(wsPath)
                if (wsDir.exists() && wsDir.canWrite()) {
                    if (type == "folder") {
                        val folder = File(wsDir, name)
                        val created = folder.mkdirs() || folder.exists()
                        fileRepository.createFile(
                            FileEntity(
                                name = name,
                                path = folder.absolutePath,
                                type = type,
                                tags = formatTags(tags),
                                parentId = parentId,
                            )
                        )
                    } else {
                        val file = File(wsDir, fileName)
                        val created = file.createNewFile() || file.exists()
                        fileRepository.createFile(
                            FileEntity(
                                name = fileName,
                                path = file.absolutePath,
                                type = type,
                                size = if (created) file.length() else 0L,
                                tags = formatTags(tags),
                                parentId = parentId,
                            )
                        )
                    }
                } else {
                    createFileInAppStorage(name, type, tags, parentId)
                }
            }
        } else {
            createFileInAppStorage(name, type, tags, parentId)
        }
    }

    private suspend fun createFileInAppStorage(
        name: String,
        type: String,
        tags: List<String>,
        parentId: Long?,
    ) {
        val fileName = if (type == "folder") name else (if (name.endsWith(".md")) name else "$name.md")
        fileRepository.createFile(
            FileEntity(
                name = fileName,
                path = "documents/$fileName",
                type = type,
                tags = formatTags(tags),
                parentId = parentId,
            )
        )
    }

    fun updateFile(file: FileEntity) {
        viewModelScope.launch {
            fileRepository.updateFile(file)
        }
    }

    fun renameFile(file: FileEntity, newName: String) {
        viewModelScope.launch {
            val oldPath = file.path
            val newPath = when {
                oldPath.startsWith("content://") -> {
                    try {
                        val uri = Uri.parse(oldPath)
                        val newUri = fileOperations.renameDocument(uri, newName)
                        newUri?.toString() ?: oldPath
                    } catch (e: Exception) {
                        Timber.tag("FilesVM").w(e, "SAF URI重命名失败，保留旧路径")
                        oldPath
                    }
                }
                oldPath.startsWith("documents/") -> "documents/$newName"
                else -> {
                    val oldFile = File(oldPath)
                    val parentDir = oldFile.parentFile
                    if (parentDir != null && oldFile.exists()) {
                        val newFile = File(parentDir, newName)
                        if (oldFile.renameTo(newFile)) newFile.absolutePath else oldPath
                    } else {
                        val parentPath = oldPath.substringBeforeLast("/", "")
                        if (parentPath.isNotEmpty()) "$parentPath/$newName" else newName
                    }
                }
            }
            fileRepository.updateFile(
                file.copy(
                    name = newName,
                    path = newPath,
                    updatedAt = System.currentTimeMillis(),
                )
            )
            Timber.tag("FilesVM").i("重命名文件：${file.name} -> $newName")
        }
    }

    fun deleteFile(file: FileEntity) {
        viewModelScope.launch {
            try {
                if (!file.isImported) {
                    deleteSystemFile(file)
                }
                fileRepository.deleteFile(file)
                _deleteEvent.emit(DeleteEvent.Success(1))
                Timber.tag("FilesVM").i("删除文件：${file.name}")
            } catch (e: Exception) {
                Timber.tag("FilesVM").e(e, "删除文件失败：${file.name}")
                _deleteEvent.emit(DeleteEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun deleteByIds(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                ids.forEach { id ->
                    val file = fileRepository.getFileById(id)
                    if (file != null && !file.isImported) {
                        deleteSystemFile(file)
                    }
                }
                fileRepository.deleteByIds(ids)
                clearSelection()
                _deleteEvent.emit(DeleteEvent.Success(ids.size))
                Timber.tag("FilesVM").i("批量删除文件：${ids.size} 个")
            } catch (e: Exception) {
                Timber.tag("FilesVM").e(e, "批量删除文件失败：${ids.size} 个")
                _deleteEvent.emit(DeleteEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            try {
                val isFilesTab = _selectedTab.value == FileViewTab.FILES
                val folderId = _currentFolderId.value
                val itemsToDelete = if (isFilesTab) {
                    if (folderId == null) fileRepository.getRootFiles().first()
                    else fileRepository.getFilesByParent(folderId).first()
                } else {
                    if (folderId == null) fileRepository.getRootFolders().first()
                    else fileRepository.getFoldersByParent(folderId).first()
                }
                // 仅删除非导入文件的系统文件，导入文件只删数据库记录
                itemsToDelete.filter { !it.isImported }.forEach { deleteSystemFile(it) }
                if (isFilesTab) {
                    if (folderId == null) fileRepository.deleteRootFiles()
                    else fileRepository.deleteFilesByParent(folderId)
                } else {
                    if (folderId == null) fileRepository.deleteRootFolders()
                    else fileRepository.deleteFoldersByParent(folderId)
                }
                clearSelection()
                _deleteEvent.emit(DeleteEvent.Success(itemsToDelete.size))
                Timber.tag("FilesVM").i("清空当前目录文件：${itemsToDelete.size} 个")
            } catch (e: Exception) {
                Timber.tag("FilesVM").e(e, "清空当前目录文件失败")
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
                    } catch (e: Exception) { Timber.tag("FilesVM").e(e, "删除SAF文档失败：$path") }
                }
                path.startsWith("documents/") -> {
                    // virtual path, no system file
                }
                else -> {
                    val sysFile = File(path)
                    if (sysFile.exists()) {
                        sysFile.delete()
                    }
                }
            }
        } catch (e: Exception) { Timber.tag("FilesVM").e(e, "删除系统文件失败：${file.path}") }
    }

    fun togglePinned(file: FileEntity) {
        viewModelScope.launch {
            fileRepository.togglePinned(file.id, !file.isPinned)
            Timber.tag("FilesVM").d("${if (!file.isPinned) "置顶" else "取消置顶"}：${file.name}")
        }
    }

    fun addTagToFile(file: FileEntity, tag: String) {
        Timber.tag("FilesVM").d("添加标签：$tag -> ${file.name}")
        val currentTags = parseTags(file.tags).toMutableList()
        if (tag !in currentTags) {
            currentTags.add(tag)
            updateFile(file.copy(tags = formatTags(currentTags)))
        }
    }

    fun removeTagFromFile(file: FileEntity, tag: String) {
        Timber.tag("FilesVM").d("移除标签：$tag -> ${file.name}")
        val currentTags = parseTags(file.tags).toMutableList()
        currentTags.remove(tag)
        updateFile(file.copy(tags = formatTags(currentTags)))
    }

    fun addTagToSelectedFiles(tag: String) {
        viewModelScope.launch {
            for (id in _selectedIds.value) {
                val file = fileRepository.getFileById(id) ?: continue
                addTagToFile(file, tag)
            }
        }
    }

    fun copyFile(file: FileEntity) {
        viewModelScope.launch {
            val copyName = generateCopyName(file.name)
            fileRepository.createFile(
                file.copy(
                    id = 0,
                    name = copyName,
                    path = "documents/$copyName",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isPinned = false,
                    parentId = _currentFolderId.value,
                )
            )
            Timber.tag("FilesVM").i("复制文件：${file.name} -> $copyName")
        }
    }

    fun moveFile(file: FileEntity, targetParentId: Long?) {
        viewModelScope.launch {
            fileRepository.updateFile(file.copy(parentId = targetParentId))
            Timber.tag("FilesVM").i("移动文件：${file.name} -> 目标文件夹ID：$targetParentId")
        }
    }

    fun shareFile(file: FileEntity, context: Context) {
        viewModelScope.launch {
            Timber.tag("FilesVM").i("分享文件：${file.name}")
            try {
                val content = when {
                    file.path.startsWith("content://") -> {
                        fileOperations.readFileContent(Uri.parse(file.path))
                    }
                    file.path.startsWith("documents/") -> file.content
                    else -> {
                        val sysFile = File(file.path)
                        if (sysFile.exists()) sysFile.readText() else file.content
                    }
                }
                // Write content to a temporary file and share as file attachment
                val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
                val tempFile = File(sharedDir, file.name).apply {
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
            } catch (e: Exception) { Timber.tag("FilesVM").e(e, "分享文件失败：${file.name}") }
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            Timber.tag("FilesVM").d("刷新文件列表，共 ${files.value.size} 个文件")
            kotlinx.coroutines.delay(800)
            _isRefreshing.value = false
        }
    }

    private fun generateCopyName(originalName: String): String {
        val dotIndex = originalName.lastIndexOf('.')
        val baseName = if (dotIndex > 0) originalName.substring(0, dotIndex) else originalName
        val extension = if (dotIndex > 0) originalName.substring(dotIndex) else ""
        return "$baseName - ${application.getString(R.string.copy_suffix)}$extension"
    }

    private fun applyFilters(files: List<FileEntity>, filter: FilterState): List<FileEntity> {
        var result = files
        when (filter.fileType) {
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
        when (filter.pinFilter) {
            PinFilter.ALL -> {}
            PinFilter.PINNED_ONLY -> result = result.filter { it.isPinned }
            PinFilter.UNPINNED_ONLY -> result = result.filter { !it.isPinned }
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
        val pinnedFirst = compareByDescending<FileEntity> { it.isPinned }
        val criteriaComparator: Comparator<FileEntity> = when (order) {
            SortOrder.NAME_ASC -> compareBy { it.name.lowercase() }
            SortOrder.NAME_DESC -> compareByDescending { it.name.lowercase() }
            SortOrder.TIME_ASC -> compareBy { it.updatedAt }
            SortOrder.TIME_DESC -> compareByDescending { it.updatedAt }
            SortOrder.SIZE_ASC -> compareBy { it.size }
            SortOrder.SIZE_DESC -> compareByDescending { it.size }
        }
        return pinnedFirst.then(criteriaComparator)
    }

    companion object {
        fun parseTags(tagsJson: String): List<String> {
            return try {
                val arr = JSONArray(tagsJson)
                (0 until arr.length()).map { arr.getString(it) }
            } catch (e: Exception) {
                Timber.tag("FilesVM").w(e, "解析标签JSON失败")
                emptyList()
            }
        }

        fun formatTags(tags: List<String>): String {
            val arr = JSONArray()
            tags.forEach { arr.put(it) }
            return arr.toString()
        }
    }
}
