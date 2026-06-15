package com.mdcito.app.ui.home

import android.net.Uri
import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdcito.app.R
import com.mdcito.app.data.db.entity.FileEntity
import com.mdcito.app.data.db.entity.HistoryEntity
import com.mdcito.app.data.files.FileOperations
import com.mdcito.app.data.repository.FileRepository
import com.mdcito.app.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.BufferOverflow
import javax.inject.Inject
import timber.log.Timber

enum class HomeFilterTab(@param:StringRes val labelRes: Int) {
    ALL(R.string.filter_all), FILES(R.string.filter_files), FOLDERS(R.string.filter_folders)
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val fileRepository: FileRepository,
    private val historyRepository: HistoryRepository,
    private val fileOperations: FileOperations,
) : ViewModel() {

    sealed class ImportEvent {
        data class Success(val fileName: String) : ImportEvent()
        data class Error(val message: String) : ImportEvent()
    }

    private val _importEvent = MutableSharedFlow<ImportEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val importEvent: SharedFlow<ImportEvent> = _importEvent

    private val _pinnedFilterTab = MutableStateFlow(HomeFilterTab.ALL)
    val pinnedFilterTab: StateFlow<HomeFilterTab> = _pinnedFilterTab.asStateFlow()

    private val _recentFilterTab = MutableStateFlow(HomeFilterTab.ALL)
    val recentFilterTab: StateFlow<HomeFilterTab> = _recentFilterTab.asStateFlow()

    private val _fileEntityMap = MutableStateFlow<Map<Long, FileEntity>>(emptyMap())
    val fileEntityMap: StateFlow<Map<Long, FileEntity>> = _fileEntityMap.asStateFlow()

    private val _allPinnedFiles = fileRepository.getPinned()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val hasPinnedFiles: StateFlow<Boolean> = _allPinnedFiles.map { it.any { file -> !file.isImported } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val pinnedFiles: StateFlow<List<FileEntity>> = combine(
        _allPinnedFiles,
        _pinnedFilterTab,
    ) { files, tab ->
        val nonImported = files.filter { !it.isImported }
        when (tab) {
            HomeFilterTab.ALL -> nonImported
            HomeFilterTab.FILES -> nonImported.filter { it.type == "file" }
            HomeFilterTab.FOLDERS -> nonImported.filter { it.type == "folder" }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val deduplicatedHistory = combine(
        historyRepository.getAll(),
        _fileEntityMap,
    ) { histories, entityMap ->
        histories
            .groupBy { it.fileId }
            .mapValues { (_, entries) -> entries.maxByOrNull { it.accessedAt }!! }
            .values
            .toList()
            .sortedByDescending { it.accessedAt }
            .take(10)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasRecentHistory: StateFlow<Boolean> = deduplicatedHistory.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val recentHistory: StateFlow<List<HistoryEntity>> = combine(
        deduplicatedHistory,
        _recentFilterTab,
        _fileEntityMap,
    ) { deduplicated, tab, entityMap ->
        when (tab) {
            HomeFilterTab.ALL -> deduplicated
            HomeFilterTab.FILES -> deduplicated.filter {
                val entity = entityMap[it.fileId]
                entity != null && entity.type == "file"
            }
            HomeFilterTab.FOLDERS -> deduplicated.filter {
                val entity = entityMap[it.fileId]
                entity != null && entity.type == "folder"
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _fileNameMap = MutableStateFlow<Map<Long, String>>(emptyMap())
    val fileNameMap: StateFlow<Map<Long, String>> = _fileNameMap.asStateFlow()

    init {
        viewModelScope.launch {
            fileRepository.getAll().collect { files ->
                _fileNameMap.value = files.associate { it.id to it.name }
                _fileEntityMap.value = files.associate { it.id to it }
                Timber.tag("Home").d("刷新首页数据，最近文件：${files.size} 个")
            }
        }
        Timber.tag("Home").d("首页初始化完成")
    }

    fun selectPinnedTab(tab: HomeFilterTab) {
        _pinnedFilterTab.value = tab
        Timber.tag("Home").d("切换到置顶标签页：$tab")
    }

    fun selectRecentTab(tab: HomeFilterTab) {
        _recentFilterTab.value = tab
        Timber.tag("Home").d("切换到最近标签页：$tab")
    }

    val stats: StateFlow<HomeStats> = combine(
        fileRepository.getNonImportedFileCount(),
        fileRepository.getNonImportedFolderCount(),
        fileRepository.getNonImportedTotalSize(),
    ) { fileCount, folderCount, totalSize ->
        HomeStats(
            fileCount = fileCount,
            folderCount = folderCount,
            totalSizeBytes = totalSize ?: 0L,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeStats())

    fun recordAccess(fileId: Long) {
        viewModelScope.launch {
            historyRepository.recordAccess(fileId)
        }
    }

    fun createFile(
        name: String,
        type: String,
        tags: List<String> = emptyList(),
        saveUri: Uri? = null,
    ) {
        viewModelScope.launch {
            Timber.tag("Home").i("首页创建文件：$name, 类型：$type")
            if (saveUri != null) {
                val resultUri = if (type == "folder") {
                    fileOperations.createFolderInDirectory(saveUri, name)
                } else {
                    val fileName = if (name.endsWith(".md")) name else "$name.md"
                    fileOperations.createFileInDirectory(saveUri, fileName)
                }
                if (resultUri == null) {
                    fileRepository.createFile(
                        FileEntity(
                            name = name,
                            path = "documents/$name",
                            type = type,
                            tags = formatTags(tags),
                        )
                    )
                } else {
                    fileOperations.takePersistablePermission(resultUri)
                    fileRepository.createFile(
                        FileEntity(
                            name = if (type == "folder") name else (if (name.endsWith(".md")) name else "$name.md"),
                            path = resultUri.toString(),
                            type = type,
                            tags = formatTags(tags),
                        )
                    )
                }
            } else {
                fileRepository.createFile(
                    FileEntity(
                        name = if (type == "folder") name else (if (name.endsWith(".md")) name else "$name.md"),
                        path = "documents/$name",
                        type = type,
                        tags = formatTags(tags),
                    )
                )
            }
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
                )
                val id = fileRepository.createFile(entity)
                fileOperations.takePersistablePermission(uri)
                historyRepository.recordAccess(id)
                _importEvent.emit(ImportEvent.Success(fileName))
                Timber.tag("Home").i("导入文件：$fileName")
            } catch (e: Exception) {
                Timber.tag("Home").e(e, "导入文件失败")
                _importEvent.emit(ImportEvent.Error(e.message ?: application.getString(R.string.import_file_failed)))
            }
        }
    }

    fun importFolder(uri: Uri, dirName: String) {
        viewModelScope.launch {
            try {
                val entity = FileEntity(
                    name = dirName,
                    path = uri.toString(),
                    type = "folder",
                    isImported = true,
                )
                val id = fileRepository.createFile(entity)
                fileOperations.takePersistablePermission(uri)
                historyRepository.recordAccess(id)
                _importEvent.emit(ImportEvent.Success(dirName))
                Timber.tag("Home").i("导入文件夹：$dirName")
            } catch (e: Exception) {
                Timber.tag("Home").e(e, "导入文件夹失败")
                _importEvent.emit(ImportEvent.Error(e.message ?: application.getString(R.string.import_file_failed)))
            }
        }
    }

    private fun formatTags(tags: List<String>): String {
        return try {
            val arr = org.json.JSONArray()
            tags.forEach { arr.put(it) }
            arr.toString()
        } catch (e: Exception) {
            Timber.tag("Home").w(e, "格式化标签JSON失败")
            "[]"
        }
    }
}

data class HomeStats(
    val fileCount: Int = 0,
    val folderCount: Int = 0,
    val totalSizeBytes: Long = 0L,
) {
    fun formatTotalSize(): String {
        return when {
            totalSizeBytes >= 1_073_741_824 -> "%.1f GB".format(totalSizeBytes / 1_073_741_824.0)
            totalSizeBytes >= 1_048_576 -> "%.1f MB".format(totalSizeBytes / 1_048_576.0)
            totalSizeBytes >= 1_024 -> "%.1f KB".format(totalSizeBytes / 1_024.0)
            else -> "$totalSizeBytes B"
        }
    }
}
