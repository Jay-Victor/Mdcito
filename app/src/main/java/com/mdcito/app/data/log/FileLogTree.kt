package com.mdcito.app.data.log

import android.content.Context
import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Timber Tree：将日志持久化写入文件。
 * 日志文件存储在应用内部存储的 logs 目录下，按日期轮转。
 * 即使应用重启或崩溃，日志仍然可以通过文件查看。
 */
class FileLogTree(
    context: Context,
) : Timber.Tree() {

    companion object {
        private const val LOG_DIR = "logs"
        private const val MAX_LOG_FILE_SIZE = 2 * 1024 * 1024L // 2MB
        private const val MAX_LOG_FILES = 5
    }

    private val logDir = File(context.filesDir, LOG_DIR)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        try {
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            rotateIfNeeded()
            cleanOldFiles()

            val fileName = "mdcito_${dateFormat.format(Date())}.log"
            val logFile = File(logDir, fileName)

            val levelStr = when (priority) {
                Log.VERBOSE -> "V"
                Log.DEBUG -> "D"
                Log.INFO -> "I"
                Log.WARN -> "W"
                Log.ERROR -> "E"
                Log.ASSERT -> "A"
                else -> "?"
            }

            val timestamp = timeFormat.format(Date())
            val logLine = buildString {
                append("[$timestamp] [$levelStr] [${tag ?: "App"}] $message")
                if (t != null) {
                    append("\n")
                    append(t.stackTraceToString())
                }
            }

            FileWriter(logFile, true).use { writer ->
                writer.write(logLine)
                writer.write("\n")
            }
        } catch (_: Exception) {
            // 文件写入失败时静默处理，避免递归日志
        }
    }

    private fun rotateIfNeeded() {
        val fileName = "mdcito_${dateFormat.format(Date())}.log"
        val logFile = File(logDir, fileName)
        if (logFile.exists() && logFile.length() > MAX_LOG_FILE_SIZE) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val rotatedFile = File(logDir, "mdcito_${timestamp}.log")
            logFile.renameTo(rotatedFile)
        }
    }

    private fun cleanOldFiles() {
        val files = logDir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        if (files.size > MAX_LOG_FILES) {
            files.drop(MAX_LOG_FILES).forEach { it.delete() }
        }
    }
}
