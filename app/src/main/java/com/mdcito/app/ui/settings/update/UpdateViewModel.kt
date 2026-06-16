package com.mdcito.app.ui.settings.update

import android.app.Application
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdcito.app.BuildConfig
import com.mdcito.app.data.update.ApkDownloader
import com.mdcito.app.data.update.DownloadState
import com.mdcito.app.data.update.DualCheckResult
import com.mdcito.app.data.update.UpdateChecker
import com.mdcito.app.data.update.UpdateInfo
import com.mdcito.app.data.update.UpdateSource
import com.mdcito.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val application: Application,
    private val updateChecker: UpdateChecker,
    private val apkDownloader: ApkDownloader,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    sealed class CheckState {
        data object Idle : CheckState()
        data object Checking : CheckState()
        data class Available(val result: DualCheckResult) : CheckState()
        data object UpToDate : CheckState()
        data class Error(val message: String) : CheckState()
    }

    sealed class UpdateEvent {
        data class InstallPermissionRequired(val hasPermission: Boolean) : UpdateEvent()
        data class InstallStarted(val filePath: String) : UpdateEvent()
        data class InstallError(val message: String) : UpdateEvent()
    }

    private val _checkState = MutableStateFlow<CheckState>(CheckState.Idle)
    val checkState: StateFlow<CheckState> = _checkState.asStateFlow()

    val downloadState: StateFlow<DownloadState> = apkDownloader.downloadState

    private val _updateEvent = MutableSharedFlow<UpdateEvent>(extraBufferCapacity = 1)
    val updateEvent: SharedFlow<UpdateEvent> = _updateEvent

    val autoCheckUpdate: StateFlow<Boolean> = settingsRepository.autoCheckUpdate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val updateSource: StateFlow<String> = settingsRepository.updateSource
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "AUTO")

    private var currentResult: DualCheckResult? = null
    private var wasAutoCheck = false  // 标记是否来自自动检查
    private var hasRetried = false    // 标记自动检查是否已重试过

    /** 获取当前选中的更新信息，优先版本号最高的源 */
    val selectedUpdateInfo: UpdateInfo?
        get() {
            val r = currentResult ?: return null
            // 双平台都有更新时，优先选择版本号更高的；版本相同时优先 Gitee（国内速度快）
            return when {
                r.gitee != null && r.github != null -> {
                    if (r.gitee.versionCode >= r.github.versionCode) r.gitee else r.github
                }
                r.gitee != null -> r.gitee
                r.github != null -> r.github
                else -> null
            }
        }

    /**
     * 手动检查更新：并行查询两个平台
     */
    fun checkForUpdate(source: UpdateSource? = null) {
        if (_checkState.value is CheckState.Checking) return
        _checkState.value = CheckState.Checking
        wasAutoCheck = false  // 手动检查

        viewModelScope.launch {
            try {
                val currentVersion = BuildConfig.VERSION_NAME
                val result = if (source != null) {
                    // 用户显式选择了单个平台
                    val checkResult = updateChecker.checkFromSource(source, currentVersion)
                    DualCheckResult(
                        gitee = if (source == UpdateSource.GITEE) checkResult.updateInfo else null,
                        github = if (source == UpdateSource.GITHUB) checkResult.updateInfo else null,
                        giteeContacted = source == UpdateSource.GITEE && checkResult.contacted,
                        githubContacted = source == UpdateSource.GITHUB && checkResult.contacted
                    )
                } else {
                    // 默认：并行检查两个平台
                    updateChecker.checkAllSources(currentVersion)
                }

                currentResult = result
                if (result.gitee != null || result.github != null) {
                    _checkState.value = CheckState.Available(result)
                    Timber.tag("Update").i("发现新版本 - Gitee:${result.gitee?.versionName} GitHub:${result.github?.versionName}")
                } else {
                    _checkState.value = CheckState.UpToDate
                    Timber.tag("Update").i("当前已是最新版本")
                }

                // 仅在至少一个平台成功联系时更新检查时间，避免网络失败时阻塞下次检查
                if (result.anyContacted) {
                    settingsRepository.setLastUpdateCheckTime(System.currentTimeMillis())
                }
            } catch (e: Exception) {
                _checkState.value = CheckState.Error(e.message ?: "检查更新失败")
                Timber.tag("Update").e(e, "检查更新失败")
            }
        }
    }

    /**
     * 应用启动时自动检查更新（静默模式，并行查询双平台）
     *
     * 改进点：
     * 1. 节流时间从 8 小时缩短为 1 小时，避免新版本发布后长时间不提示
     * 2. 仅在至少一个平台成功联系时更新检查时间，网络失败时不阻塞下次检查
     * 3. 两个平台均联系失败时自动重试一次（延迟 30 秒），应对启动时网络未就绪
     * 4. 未发现新版本时重置 wasAutoCheck，避免状态残留
     */
    fun autoCheckIfEnabled() {
        viewModelScope.launch {
            val autoCheck = settingsRepository.autoCheckUpdate.first()
            if (!autoCheck) return@launch

            val lastCheckTime = settingsRepository.lastUpdateCheckTime.first()
            val oneHour = 60 * 60 * 1000L
            if (System.currentTimeMillis() - lastCheckTime < oneHour) {
                Timber.tag("Update").d("距上次检查不足 1 小时，跳过自动检查")
                return@launch
            }

            performAutoCheck(isRetry = false)
        }
    }

    /**
     * 执行自动检查的核心逻辑
     * @param isRetry 是否为重试（重试时不检查节流时间）
     */
    private suspend fun performAutoCheck(isRetry: Boolean) {
        try {
            wasAutoCheck = true
            val result = updateChecker.checkAllSources(BuildConfig.VERSION_NAME)

            if (result.gitee != null || result.github != null) {
                currentResult = result
                _checkState.value = CheckState.Available(result)
                Timber.tag("Update").i("自动检查发现新版本 - Gitee:${result.gitee?.versionName} GitHub:${result.github?.versionName}")
            } else {
                // 未发现新版本，重置标记
                wasAutoCheck = false
            }

            // 仅在至少一个平台成功联系时更新检查时间
            if (result.anyContacted) {
                settingsRepository.setLastUpdateCheckTime(System.currentTimeMillis())
            } else if (!isRetry && !hasRetried) {
                // 两个平台均未联系成功，且尚未重试过 → 延迟重试
                hasRetried = true
                Timber.tag("Update").w("自动检查更新失败：两个平台均未响应，30 秒后重试")
                delay(30_000)
                // 重试前检查是否已有结果（避免用户手动检查后重复弹窗）
                if (_checkState.value !is CheckState.Available) {
                    performAutoCheck(isRetry = true)
                }
            } else if (isRetry) {
                // 重试仍失败，记录日志但不阻塞下次启动检查
                Timber.tag("Update").w("自动检查更新重试仍失败，等待下次启动")
            }
        } catch (e: Exception) {
            wasAutoCheck = false
            Timber.tag("Update").w(e, "自动检查更新失败（静默）")
        }
    }

    /**
     * 开始下载更新包
     *
     * 自动回退机制：构建下载 URL 队列（指定的镜像 → 原始地址 → 其他镜像），
     * 依次尝试，某个源失败后自动切换到下一个，直到成功或全部失败。
     *
     * @param mirrorUrl 指定的镜像 URL，null 表示从原始下载地址开始
     */
    fun startDownload(mirrorUrl: String? = null) {
        val updateInfo = selectedUpdateInfo ?: return

        // 构建下载 URL 队列：指定的 URL 优先，然后回退到原始 URL 和其他镜像
        val urlQueue = buildDownloadUrlQueue(updateInfo, mirrorUrl)

        viewModelScope.launch {
            for ((index, url) in urlQueue.withIndex()) {
                // 每次尝试前重置状态并清理旧文件（不同 URL 的文件内容可能不同，不能断点续传）
                apkDownloader.resetState()
                val dir = File(application.externalCacheDir ?: application.cacheDir, "updates")
                dir.listFiles()?.forEach { it.delete() }

                if (index > 0) {
                    Timber.tag("Update").w("上一个下载源失败，自动尝试第 ${index + 1}/${urlQueue.size} 个源: $url")
                }

                val result = apkDownloader.downloadApk(url, updateInfo.fileName)
                if (result != null) {
                    Timber.tag("Update").i("下载成功（第 ${index + 1} 个源）: $url")
                    return@launch
                }
            }

            // 所有 URL 都失败，最终错误状态已由最后一次 downloadApk 设置
            Timber.tag("Update").e("所有 ${urlQueue.size} 个下载源均失败")
        }
    }

    /**
     * 构建下载 URL 队列
     * 指定的镜像 URL 优先，然后是原始下载地址，最后是其他镜像
     */
    private fun buildDownloadUrlQueue(updateInfo: UpdateInfo, mirrorUrl: String?): List<String> {
        val queue = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        fun addUrl(url: String) {
            if (url.isNotBlank() && url !in seen) {
                queue.add(url)
                seen.add(url)
            }
        }

        // 指定的镜像 URL 优先
        if (mirrorUrl != null) {
            addUrl(mirrorUrl)
        }
        // 原始下载地址
        addUrl(updateInfo.downloadUrl)
        // 其他镜像 URL（作为回退）
        updateInfo.mirrorUrls.forEach { addUrl(it.url) }

        return queue
    }

    fun pauseDownload() = apkDownloader.pauseDownload()

    fun resumeDownload() {
        // 仅取消暂停标志，正在运行的下载协程会在 while(isPaused) 循环中自然唤醒继续
        apkDownloader.resumeDownload()
    }

    fun cancelDownload() = apkDownloader.cancelDownload()

    /**
     * 安装已下载的 APK
     */
    fun installApk(filePath: String) {
        viewModelScope.launch {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    _updateEvent.emit(UpdateEvent.InstallError("APK 文件不存在"))
                    return@launch
                }

                val uri = FileProvider.getUriForFile(
                    application,
                    "${application.packageName}.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                // Android 8+ 需要请求安装未知来源应用权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val canInstall = application.packageManager.canRequestPackageInstalls()
                    if (!canInstall) {
                        _updateEvent.emit(UpdateEvent.InstallPermissionRequired(false))
                        return@launch
                    }
                }

                application.startActivity(intent)
                _updateEvent.emit(UpdateEvent.InstallStarted(filePath))
            } catch (e: Exception) {
                _updateEvent.emit(UpdateEvent.InstallError(e.message ?: "安装失败"))
                Timber.tag("Update").e(e, "安装 APK 失败")
            }
        }
    }

    /**
     * 打开安装权限设置页面
     */
    fun openInstallPermissionSettings() {
        viewModelScope.launch {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = android.net.Uri.parse("package:${application.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                application.startActivity(intent)
            } catch (e: Exception) {
                Timber.tag("Update").e(e, "打开安装权限设置失败")
            }
        }
    }

    /**
     * 重置检查状态
     */
    fun resetCheckState() {
        _checkState.value = CheckState.Idle
        wasAutoCheck = false
        hasRetried = false
    }

    /**
     * 检查是否应该自动弹出更新对话框（仅自动检查时触发）
     */
    fun shouldAutoShowDialog(): Boolean {
        return wasAutoCheck && _checkState.value is CheckState.Available
    }

    /**
     * 标记自动弹窗已显示，避免重复弹出
     */
    fun markAutoDialogShown() {
        wasAutoCheck = false
    }

    fun setAutoCheckUpdate(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoCheckUpdate(enabled) }
    }

    fun setUpdateSource(source: String) {
        viewModelScope.launch { settingsRepository.setUpdateSource(source) }
    }

    override fun onCleared() {
        super.onCleared()
        apkDownloader.cancelDownload()
    }
}
