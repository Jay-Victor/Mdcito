package com.mdcito.app

import android.app.Application
import android.content.Context
import com.google.android.material.color.DynamicColors
import com.mdcito.app.data.locale.LanguageHelper
import com.mdcito.app.data.log.AppLogger
import com.mdcito.app.data.log.AppLoggerTree
import com.mdcito.app.data.log.FileLogTree
import com.mdcito.app.data.log.LogLevel
import com.mdcito.app.data.repository.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MdcitoApplication : Application() {

    @Inject lateinit var appLogger: AppLogger
    @Inject lateinit var settingsRepository: SettingsRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun attachBaseContext(base: Context) {
        LanguageHelper.init(base)
        super.attachBaseContext(LanguageHelper.wrapContext(base))
    }

    override fun onCreate() {
        super.onCreate()
        // 启用 Material You 动态颜色支持
        DynamicColors.applyToActivitiesIfAvailable(this)

        // 初始化 Timber 日志系统
        initTimber()

        // 同步初始化日志配置，确保启动日志不丢失
        runBlocking {
            appLogger.debugMode = settingsRepository.debugMode.first()
            appLogger.minLogLevel = LogLevel.fromName(settingsRepository.logLevel.first())
        }

        // 监听日志级别配置变化
        observeLogLevel()

        // 监听调试模式变化
        observeDebugMode()

        // 设置全局未捕获异常处理器
        setupUncaughtExceptionHandler()

        Timber.tag("App").i("Mdcito 应用启动完成")
    }

    private fun initTimber() {
        // 种植 AppLogger 桥接 Tree（写入内存，用户可在 UI 查看）
        Timber.plant(AppLoggerTree(appLogger))
        // 种植文件持久化 Tree（写入文件，重启后可查）
        Timber.plant(FileLogTree(this))
        // 种植 Debug Tree（输出到 Logcat，仅调试构建）
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun observeLogLevel() {
        appScope.launch {
            settingsRepository.logLevel
                .distinctUntilChanged()
                .collect { levelName ->
                    appLogger.minLogLevel = LogLevel.fromName(levelName)
                }
        }
    }

    private fun observeDebugMode() {
        appScope.launch {
            settingsRepository.debugMode
                .distinctUntilChanged()
                .collect { enabled ->
                    appLogger.debugMode = enabled
                }
        }
    }

    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.e(throwable, "未捕获异常 [${thread.name}]")
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
