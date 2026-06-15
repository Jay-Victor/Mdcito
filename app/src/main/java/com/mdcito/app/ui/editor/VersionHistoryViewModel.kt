package com.mdcito.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdcito.app.data.db.entity.VersionEntity
import com.mdcito.app.data.repository.VersionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.BufferOverflow
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class VersionHistoryViewModel @Inject constructor(
    private val versionRepository: VersionRepository,
) : ViewModel() {

    private val versionCache = mutableMapOf<Long, StateFlow<List<VersionEntity>>>()
    private var allVersionsCache: StateFlow<List<VersionEntity>>? = null

    sealed class DeleteEvent {
        data class Success(val count: Int) : DeleteEvent()
        data class Error(val message: String) : DeleteEvent()
    }

    private val _deleteEvent = MutableSharedFlow<DeleteEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val deleteEvent: SharedFlow<DeleteEvent> = _deleteEvent

    fun getVersionsForFile(fileId: Long): StateFlow<List<VersionEntity>> {
        return versionCache.getOrPut(fileId) {
            versionRepository.getByFileId(fileId)
                .onEach { versions ->
                    Timber.tag("Version").d("加载版本历史：fileId=$fileId，共 ${versions.size} 个快照")
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }
    }

    fun getAllVersions(): StateFlow<List<VersionEntity>> {
        return allVersionsCache ?: versionRepository.getAllVersions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
            .also { allVersionsCache = it }
    }

    fun deleteVersion(version: VersionEntity) {
        viewModelScope.launch {
            try {
                versionRepository.delete(version)
                _deleteEvent.emit(DeleteEvent.Success(1))
                Timber.tag("Version").i("删除版本快照：${version.id}")
            } catch (e: Exception) {
                _deleteEvent.emit(DeleteEvent.Error(e.message ?: "Unknown error"))
                Timber.tag("Version").e(e, "删除版本快照失败")
            }
        }
    }

    fun deleteVersionsByIds(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                versionRepository.deleteByIds(ids)
                _deleteEvent.emit(DeleteEvent.Success(ids.size))
                Timber.tag("Version").i("批量删除版本快照：${ids.size} 个")
            } catch (e: Exception) {
                _deleteEvent.emit(DeleteEvent.Error(e.message ?: "Unknown error"))
                Timber.tag("Version").e(e, "批量删除版本快照失败")
            }
        }
    }

    fun deleteAllVersionsForFile(fileId: Long) {
        viewModelScope.launch {
            try {
                val count = versionRepository.deleteByFileId(fileId)
                if (count >= 20) {
                    runCatching { versionRepository.incrementalVacuum() }
                }
                _deleteEvent.emit(DeleteEvent.Success(count))
                Timber.tag("Version").i("删除文件全部版本快照：fileId=$fileId，共 $count 个")
            } catch (e: Exception) {
                _deleteEvent.emit(DeleteEvent.Error(e.message ?: "Unknown error"))
                Timber.tag("Version").e(e, "删除文件全部版本快照失败：fileId=$fileId")
            }
        }
    }

    /**
     * 手动创建快照。使用 insertAndTrim 自动修剪旧快照，
     * 每文件默认保留最新 50 个快照。
     */
    fun createSnapshot(fileId: Long, filePath: String, content: String, summary: String = "") {
        viewModelScope.launch {
            versionRepository.insertAndTrim(
                VersionEntity(
                    fileId = fileId,
                    filePath = filePath,
                    size = content.toByteArray().size.toLong(),
                    summary = summary,
                    content = content,
                )
            )
            Timber.tag("Version").i("创建版本快照：$filePath")
        }
    }
}
