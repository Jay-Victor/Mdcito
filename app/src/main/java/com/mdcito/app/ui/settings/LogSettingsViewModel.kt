package com.mdcito.app.ui.settings

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdcito.app.R
import com.mdcito.app.data.log.AppLogger
import com.mdcito.app.data.log.LogEntry
import com.mdcito.app.data.log.LogLevel
import com.mdcito.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LogSettingsViewModel @Inject constructor(
    private val application: Application,
    private val appLogger: AppLogger,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    sealed class LogEvent {
        data class CopySuccess(val count: Int) : LogEvent()
        data class CopyFailed(val message: String) : LogEvent()
        data class ClearSuccess(val level: LogLevel) : LogEvent()
        data class ClearAllSuccess(val count: Int) : LogEvent()
        data class ExportSuccess(val filePath: String) : LogEvent()
        data class ExportFailed(val message: String) : LogEvent()
        data class ExportProgress(val percent: Int) : LogEvent()
    }

    private val _logEvent = MutableSharedFlow<LogEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
    )
    val logEvent: SharedFlow<LogEvent> = _logEvent

    val debugMode: StateFlow<Boolean> = settingsRepository.debugMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val logCount: StateFlow<Map<LogLevel, Int>> = appLogger.logCount

    private val _expandedLevels = MutableStateFlow<Set<LogLevel>>(emptySet())
    val expandedLevels: StateFlow<Set<LogLevel>> = _expandedLevels.asStateFlow()

    private val _levelLogs = MutableStateFlow<Map<LogLevel, List<LogEntry>>>(emptyMap())
    val levelLogs: StateFlow<Map<LogLevel, List<LogEntry>>> = _levelLogs.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow(0)
    val exportProgress: StateFlow<Int> = _exportProgress.asStateFlow()

    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()

    private val _selectedExportLevels = MutableStateFlow<Set<LogLevel>>(LogLevel.entries.toSet())
    val selectedExportLevels: StateFlow<Set<LogLevel>> = _selectedExportLevels.asStateFlow()

    private val _showClearConfirmDialog = MutableStateFlow<LogLevel?>(null)
    val showClearConfirmDialog: StateFlow<LogLevel?> = _showClearConfirmDialog.asStateFlow()

    private val _showClearAllConfirmDialog = MutableStateFlow(false)
    val showClearAllConfirmDialog: StateFlow<Boolean> = _showClearAllConfirmDialog.asStateFlow()

    init {
        refreshLogs()
        // 监听日志计数变化，自动刷新日志列表
        viewModelScope.launch {
            appLogger.logCount.collect {
                refreshLogs()
            }
        }
    }

    fun setDebugMode(enabled: Boolean) {
        Timber.tag("LogSettings").d("调试模式：${if (enabled) "开启" else "关闭"}")
        viewModelScope.launch { settingsRepository.setDebugMode(enabled) }
    }

    fun toggleLevelExpanded(level: LogLevel) {
        _expandedLevels.value = _expandedLevels.value.toMutableSet().apply {
            if (contains(level)) remove(level) else add(level)
        }
        if (level in _expandedLevels.value) {
            refreshLevelLogs(level)
        }
    }

    fun refreshLogs() {
        _levelLogs.value = LogLevel.entries.associateWith { appLogger.getLogs(it) }
    }

    private fun refreshLevelLogs(level: LogLevel) {
        _levelLogs.value = _levelLogs.value.toMutableMap().apply {
            this[level] = appLogger.getLogs(level)
        }
    }

    fun copyLogs(level: LogLevel) {
        Timber.tag("LogSettings").d("复制${level.label}级别日志")
        viewModelScope.launch {
            try {
                val text = appLogger.copyLogsAsText(level)
                val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Mdcito Logs ($level)", text)
                clipboard.setPrimaryClip(clip)
                val count = appLogger.getLogCount(level)
                _logEvent.emit(LogEvent.CopySuccess(count))
            } catch (e: Exception) {
                _logEvent.emit(LogEvent.CopyFailed(e.message ?: application.getString(R.string.unknown_error)))
            }
        }
    }

    fun clearLogs(level: LogLevel) {
        Timber.tag("LogSettings").i("清除${level.label}级别日志")
        viewModelScope.launch {
            appLogger.clearLogs(level)
            refreshLevelLogs(level)
            _logEvent.emit(LogEvent.ClearSuccess(level))
        }
    }

    fun showClearConfirm(level: LogLevel) {
        _showClearConfirmDialog.value = level
    }

    fun dismissClearConfirm() {
        _showClearConfirmDialog.value = null
    }

    fun showClearAllConfirm() {
        _showClearAllConfirmDialog.value = true
    }

    fun dismissClearAllConfirm() {
        _showClearAllConfirmDialog.value = false
    }

    fun clearAllLogs() {
        Timber.tag("LogSettings").i("清除所有日志")
        viewModelScope.launch {
            val count = appLogger.getTotalLogCount()
            appLogger.clearLogs()
            refreshLogs()
            _showClearAllConfirmDialog.value = false
            _logEvent.emit(LogEvent.ClearAllSuccess(count))
        }
    }

    fun showExportDialog() {
        _showExportDialog.value = true
    }

    fun dismissExportDialog() {
        _showExportDialog.value = false
    }

    fun toggleExportLevel(level: LogLevel) {
        _selectedExportLevels.value = _selectedExportLevels.value.toMutableSet().apply {
            if (contains(level)) remove(level) else add(level)
        }
    }

    fun selectAllExportLevels() {
        _selectedExportLevels.value = LogLevel.entries.toSet()
    }

    fun exportLogs() {
        Timber.tag("LogSettings").i("导出日志")
        val levels = _selectedExportLevels.value
        if (levels.isEmpty()) return

        viewModelScope.launch {
            _isExporting.value = true
            _exportProgress.value = 0

            try {
                _logEvent.emit(LogEvent.ExportProgress(10))

                val content = withContext(Dispatchers.IO) {
                    _exportProgress.value = 30
                    appLogger.exportLogsAsText(levels)
                }

                _exportProgress.value = 60

                val fileName = withContext(Dispatchers.IO) {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    "mdcito_log_$timestamp.txt"
                }

                _exportProgress.value = 80

                val filePath = withContext(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android 10+：使用 MediaStore API 写入，确保文件管理器可见
                        val values = ContentValues().apply {
                            put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
                            put(MediaStore.Files.FileColumns.MIME_TYPE, "text/plain")
                            put(MediaStore.Files.FileColumns.TITLE, fileName)
                            put(MediaStore.Files.FileColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOCUMENTS + "/Mdcito/Logs")
                            put(MediaStore.Files.FileColumns.IS_PENDING, 1)
                        }
                        val uri = application.contentResolver.insert(
                            MediaStore.Files.getContentUri("external"), values
                        ) ?: throw Exception(application.getString(R.string.error_cannot_create_mediastore))

                        // 通过 MediaStore 的 OutputStream 写入文件内容
                        application.contentResolver.openOutputStream(uri)?.use { os ->
                            os.write(content.toByteArray())
                        } ?: throw Exception(application.getString(R.string.error_cannot_open_output_stream))

                        // 标记文件写入完成，使其对其他应用可见
                        values.clear()
                        values.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
                        application.contentResolver.update(uri, values, null, null)

                        // 主动通知媒体扫描器和 ContentObserver，确保文件管理器可见
                        val actualPath = android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_DOCUMENTS
                        ).absolutePath + "/Mdcito/Logs/$fileName"
                        MediaScannerConnection.scanFile(
                            application, arrayOf(actualPath), arrayOf("text/plain"), null
                        )
                        application.contentResolver.notifyChange(uri, null)

                        // 通过 URI 查询实际文件路径用于显示
                        val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
                        application.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                cursor.getString(0)
                            } else {
                                android.os.Environment.getExternalStoragePublicDirectory(
                                    android.os.Environment.DIRECTORY_DOCUMENTS
                                ).absolutePath + "/Mdcito/Logs/$fileName"
                            }
                        } ?: android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_DOCUMENTS
                        ).absolutePath + "/Mdcito/Logs/$fileName"
                    } else {
                        // Android 9 及以下：直接 File API 写入 + 扫描通知
                        val docsDir = android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_DOCUMENTS
                        )
                        val exportsDir = File(docsDir, "Mdcito/Logs")
                        exportsDir.mkdirs()
                        val file = File(exportsDir, fileName)
                        file.writeText(content)
                        MediaScannerConnection.scanFile(
                            application, arrayOf(file.absolutePath), arrayOf("text/plain"), null
                        )
                        file.absolutePath
                    }
                }

                _exportProgress.value = 100
                _logEvent.emit(LogEvent.ExportSuccess(filePath))
            } catch (e: Exception) {
                _logEvent.emit(LogEvent.ExportFailed(e.message ?: application.getString(R.string.unknown_error)))
            } finally {
                _isExporting.value = false
                _exportProgress.value = 0
                _showExportDialog.value = false
            }
        }
    }

    fun generateTestLogs() {
        Timber.tag("LogSettings").d("生成测试日志")
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                appLogger.error("CloudSync", application.getString(R.string.log_test_sync_timeout))
                appLogger.warn("CloudSync", application.getString(R.string.log_test_wifi_skip))
                appLogger.info("Editor", application.getString(R.string.log_test_file_saved))
                appLogger.debug("Editor", application.getString(R.string.log_test_doc_loaded))
                appLogger.error("ImageBed", application.getString(R.string.log_test_upload_auth_expired))
                appLogger.warn("FileRepo", application.getString(R.string.log_test_file_modified))
                appLogger.info("App", application.getString(R.string.log_test_app_started))
                appLogger.debug("Settings", application.getString(R.string.log_test_log_level_switched))
            }
            refreshLogs()
        }
    }
}
