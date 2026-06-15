package com.mdcito.app.data.log

import android.util.Log
import timber.log.Timber

/**
 * Timber Tree 桥接：将 Timber 日志同时写入 AppLogger 内存存储。
 * 这样所有通过 Timber 记录的日志都会出现在用户可见的日志设置页面中。
 */
class AppLoggerTree(
    private val appLogger: AppLogger,
) : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val level = when (priority) {
            Log.VERBOSE -> LogLevel.DEBUG
            Log.DEBUG -> LogLevel.DEBUG
            Log.INFO -> LogLevel.INFO
            Log.WARN -> LogLevel.WARN
            Log.ERROR -> LogLevel.ERROR
            Log.ASSERT -> LogLevel.ERROR
            else -> LogLevel.DEBUG
        }

        val effectiveTag = tag ?: "App"
        appLogger.log(level, effectiveTag, message, t)
    }
}
