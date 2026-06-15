package com.mdcito.app.data.log

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

import com.mdcito.app.R

enum class LogLevel(val label: String, val priority: Int, val descResId: Int, val labelResId: Int) {
    ERROR("错误", 3, R.string.log_level_error_desc, R.string.log_error),
    WARN("警告", 2, R.string.log_level_warn_desc, R.string.log_warning),
    INFO("信息", 1, R.string.log_level_info_desc, R.string.log_info),
    DEBUG("调试", 0, R.string.log_level_debug_desc, R.string.log_debug);


    companion object {
        fun fromName(name: String): LogLevel =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: WARN
    }
}

data class LogEntry(
    val id: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val stackTrace: String? = null,
)

@Singleton
class AppLogger @Inject constructor() {

    companion object {
        const val MAX_LOGS_PER_LEVEL = 200
        const val MAX_TOTAL_LOGS = 800
    }

    private val _allLogs = CopyOnWriteArrayList<LogEntry>()
    private val _logCount = MutableStateFlow<Map<LogLevel, Int>>(emptyMap())
    private var _nextId = 0L

    /**
     * 最低日志级别，低于此级别的日志将被忽略。
     * 由 SettingsRepository.logLevel 驱动更新。
     */
    var minLogLevel: LogLevel = LogLevel.DEBUG

    /**
     * 调试模式开关，控制是否记录日志到内存。
     * 关闭时 log() 直接返回，并清除已有日志；开启时开始记录。
     * 由 SettingsRepository.debugMode 驱动更新。
     */
    var debugMode: Boolean = false
        set(value) {
            field = value
            if (!value) {
                // 调试模式关闭时，清除所有已记录的日志
                clearLogs()
            }
        }

    val logCount: StateFlow<Map<LogLevel, Int>> = _logCount.asStateFlow()

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    @Synchronized
    private fun nextId(): Long = _nextId++

    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        // 调试模式关闭时，不记录日志到内存
        if (!debugMode) return
        if (level.priority < minLogLevel.priority) return

        val entry = LogEntry(
            id = nextId(),
            level = level,
            tag = tag,
            message = message,
            timestamp = System.currentTimeMillis(),
            stackTrace = throwable?.stackTraceToString(),
        )
        _allLogs.add(entry)

        trimIfNeeded()

        updateCounts()
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) =
        log(LogLevel.ERROR, tag, message, throwable)

    fun warn(tag: String, message: String) =
        log(LogLevel.WARN, tag, message)

    fun info(tag: String, message: String) =
        log(LogLevel.INFO, tag, message)

    fun debug(tag: String, message: String) =
        log(LogLevel.DEBUG, tag, message)

    fun getLogs(level: LogLevel? = null): List<LogEntry> {
        return if (level != null) {
            _allLogs.filter { it.level == level }
        } else {
            _allLogs.toList()
        }
    }

    fun getLogCount(level: LogLevel): Int = _allLogs.count { it.level == level }

    fun getTotalLogCount(): Int = _allLogs.size

    fun clearLogs(level: LogLevel? = null) {
        if (level != null) {
            _allLogs.removeAll { it.level == level }
        } else {
            _allLogs.clear()
        }
        updateCounts()
    }

    fun copyLogsAsText(level: LogLevel? = null): String {
        val logs = getLogs(level)
        if (logs.isEmpty()) return ""
        return buildString {
            appendLine("=== Mdcito Log Export ===")
            appendLine("Export Time: ${formatTimestamp(System.currentTimeMillis())}")
            if (level != null) {
                appendLine("Log Level: ${level.label}")
            } else {
                appendLine("Log Level: ALL")
            }
            appendLine("========================")
            appendLine()
            for (entry in logs) {
                val time = formatTimestamp(entry.timestamp)
                appendLine("[$time] [${entry.level.label}] [${entry.tag}] ${entry.message}")
                if (entry.stackTrace != null) {
                    appendLine(entry.stackTrace)
                }
            }
        }
    }

    fun exportLogsAsText(levels: Set<LogLevel>): String {
        if (levels.isEmpty()) return ""
        val logs = _allLogs.filter { it.level in levels }.sortedBy { it.timestamp }
        if (logs.isEmpty()) return ""
        return buildString {
            appendLine("=== Mdcito Log Export ===")
            appendLine("Export Time: ${formatTimestamp(System.currentTimeMillis())}")
            appendLine("Log Levels: ${levels.joinToString(", ") { it.label }}")
            appendLine("Total Entries: ${logs.size}")
            appendLine("========================")
            appendLine()
            for (entry in logs) {
                val time = formatTimestamp(entry.timestamp)
                appendLine("[$time] [${entry.level.label}] [${entry.tag}] ${entry.message}")
                if (entry.stackTrace != null) {
                    appendLine(entry.stackTrace)
                }
            }
        }
    }

    private fun trimIfNeeded() {
        if (_allLogs.size > MAX_TOTAL_LOGS) {
            val excess = _allLogs.size - MAX_TOTAL_LOGS
            val toRemove = _allLogs.take(excess).map { it.id }.toSet()
            _allLogs.removeAll { it.id in toRemove }
        }
        for (level in LogLevel.entries) {
            val levelLogs = _allLogs.filter { it.level == level }
            if (levelLogs.size > MAX_LOGS_PER_LEVEL) {
                val toRemove = levelLogs
                    .sortedBy { it.id }
                    .take(levelLogs.size - MAX_LOGS_PER_LEVEL)
                    .map { it.id }
                    .toSet()
                _allLogs.removeAll { it.level == level && it.id in toRemove }
            }
        }
    }

    private fun updateCounts() {
        val counts = mutableMapOf<LogLevel, Int>()
        for (level in LogLevel.entries) {
            counts[level] = 0
        }
        for (entry in _allLogs) {
            counts[entry.level] = (counts[entry.level] ?: 0) + 1
        }
        _logCount.value = counts
    }
}

val LogLevel.colorLight: Color
    get() = when (this) {
        LogLevel.ERROR -> Color(0xFFDC2626)
        LogLevel.WARN -> Color(0xFFD97706)
        LogLevel.INFO -> Color(0xFF2563EB)
        LogLevel.DEBUG -> Color(0xFF6B7280)
    }

val LogLevel.colorDark: Color
    get() = when (this) {
        LogLevel.ERROR -> Color(0xFFF87171)
        LogLevel.WARN -> Color(0xFFFBBF24)
        LogLevel.INFO -> Color(0xFF60A5FA)
        LogLevel.DEBUG -> Color(0xFF9CA3AF)
    }
