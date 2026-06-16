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
                    val info = updateChecker.checkFromSource(source, currentVersion)
                    DualCheckResult(
                        gitee = if (source == UpdateSource.GITEE) info else null,
                        github = if (source == UpdateSource.GITHUB) info else null
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

                settingsRepository.setLastUpdateCheckTime(System.currentTimeMillis())
            } catch (e: Exception) {
                _checkState.value = CheckState.Error(e.message ?: "检查更新失败")
                Timber.tag("Update").e(e, "检查更新失败")
            }
        }
    }

    /**
     * 应用启动时自动检查更新（静默模式，并行查询双平台）
     */
    fun autoCheckIfEnabled() {
        viewModelScope.launch {
            val autoCheck = settingsRepository.autoCheckUpdate.first()
            if (!autoCheck) return@launch

            val lastCheckTime = settingsRepository.lastUpdateCheckTime.first()
            val eightHours = 8 * 60 * 60 * 1000L
            if (System.currentTimeMillis() - lastCheckTime < eightHours) {
                Timber.tag("Update").d("距上次检查不足 8 小时，跳过自动检查")
                return@launch
            }

            try {
                wasAutoCheck = true  // 标记为自动检查
                val result = updateChecker.checkAllSources(BuildConfig.VERSION_NAME)
                if (result.gitee != null || result.github != null) {
                    currentResult = result
                    _checkState.value = CheckState.Available(result)
                    Timber.tag("Update").i("自动检查发现新版本 - Gitee:${result.gitee?.versionName} GitHub:${result.github?.versionName}")
                }
                settingsRepository.setLastUpdateCheckTime(System.currentTimeMillis())
            } catch (e: Exception) {
                Timber.tag("Update").w(e, "自动检查更新失败（静默）")
            }
        }
    }

    /**
     * 开始下载更新包
     */
    fun startDownload(mirrorUrl: String? = null) {
        val updateInfo = selectedUpdateInfo ?: return
        val url = mirrorUrl ?: updateInfo.downloadUrl
        viewModelScope.launch {
            apkDownloader.resetState()
            // 删除旧的部分文件，确保从头开始新下载
            val dir = File(application.externalCacheDir ?: application.cacheDir, "updates")
            dir.listFiles()?.forEach { it.delete() }
            apkDownloader.downloadApk(url, updateInfo.fileName)
        }
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
