package com.mdcito.app.ui.settings.cloudsync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdcito.app.data.sync.CloudSyncConfig
import com.mdcito.app.data.sync.CloudSyncManager
import com.mdcito.app.data.sync.CloudSyncServiceType
import com.mdcito.app.data.sync.ConflictFile
import com.mdcito.app.data.sync.ConflictResolution
import com.mdcito.app.data.sync.ConnectionTestResult
import com.mdcito.app.data.sync.StorageInfo
import com.mdcito.app.data.sync.SyncMode
import com.mdcito.app.data.sync.SyncResult
import android.app.Application
import com.mdcito.app.R
import com.mdcito.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class CloudSyncViewModel @Inject constructor(
    private val application: Application,
    private val cloudSyncManager: CloudSyncManager,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    // ── 配置状态 ──
    private val _config = MutableStateFlow(CloudSyncConfig())
    val config: StateFlow<CloudSyncConfig> = _config.asStateFlow()

    // ── 加载状态 ──
    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncProgress = MutableStateFlow(com.mdcito.app.data.sync.SyncProgress())
    val syncProgress: StateFlow<com.mdcito.app.data.sync.SyncProgress> = _syncProgress.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── 凭证校验状态 ──
    private val _credentialValidationError = MutableStateFlow<String?>(null)
    val credentialValidationError: StateFlow<String?> = _credentialValidationError.asStateFlow()

    // ── 存储空间状态 ──
    private val _storageInfo = MutableStateFlow<StorageInfo?>(null)
    val storageInfo: StateFlow<StorageInfo?> = _storageInfo.asStateFlow()

    private val _storageLoading = MutableStateFlow(false)
    val storageLoading: StateFlow<Boolean> = _storageLoading.asStateFlow()

    private val _storageError = MutableStateFlow<String?>(null)
    val storageError: StateFlow<String?> = _storageError.asStateFlow()

    // ── 一次性事件 ──
    private val _connectionTestResult = MutableSharedFlow<ConnectionTestResult>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val connectionTestResult: SharedFlow<ConnectionTestResult> = _connectionTestResult.asSharedFlow()

    private val _syncResult = MutableSharedFlow<SyncResult>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val syncResult: SharedFlow<SyncResult> = _syncResult.asSharedFlow()

    private val _oauthUrl = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val oauthUrl: SharedFlow<String> = _oauthUrl.asSharedFlow()

    private val _oauthResult = MutableSharedFlow<OAuthResult>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val oauthResult: SharedFlow<OAuthResult> = _oauthResult.asSharedFlow()

    // ── Debounce 保存 ──
    private var saveJob: Job? = null

    // ── OAuth 防重复处理 ──
    private var oauthProcessing = false

    // ── 同步历史持久化锁 ──
    private val historyMutex = Mutex()

    // ── 初始化：加载配置 ──
    init {
        loadConfig()
        loadSyncHistory()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _config.value = cloudSyncManager.loadConfig()
                Timber.tag("CloudSync").d("加载云同步状态")
                // 如果已有有效 token，自动查询存储空间
                loadStorageInfo()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── 更新配置（带 debounce 防抖） ──
    fun updateConfig(config: CloudSyncConfig) {
        _config.value = config
        scheduleSave(config)
    }

    /**
     * 立即保存配置（用于关键操作如启用同步、OAuth 回调等）
     */
    private fun updateConfigImmediate(config: CloudSyncConfig) {
        _config.value = config
        saveJob?.cancel()
        viewModelScope.launch {
            saveConfig(config)
        }
    }

    /**
     * 延迟保存配置，500ms 内的多次更新只执行最后一次
     */
    private fun scheduleSave(config: CloudSyncConfig) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            saveConfig(config)
        }
    }

    private suspend fun saveConfig(config: CloudSyncConfig) {
        try {
            settingsRepository.setCloudSyncServiceType(config.serviceType.name)
            settingsRepository.setCloudSyncServerUrl(config.serverUrl)
            settingsRepository.setCloudSyncPort(config.port)
            settingsRepository.setCloudSyncUsername(config.username)
            settingsRepository.setCloudSyncPassword(config.password)
            settingsRepository.setCloudSyncRemotePath(config.remotePath)
            settingsRepository.setCloudSyncMode(config.syncMode.name)
            settingsRepository.setCloudSyncAutoSyncInterval(config.autoSyncIntervalMinutes)
            settingsRepository.setCloudSyncWifiOnly(config.syncOnWifiOnly)
            settingsRepository.setCloudSyncChargingOnly(config.syncOnCharging)
            settingsRepository.setCloudSyncFileFilterRules(config.fileFilterRules)
            settingsRepository.setCloudSyncLastSyncTime(config.lastSyncTime)
            settingsRepository.setCloudSyncLastSyncStatus(config.lastSyncStatus)
            settingsRepository.setCloudSyncEnabled(config.isEnabled)
            settingsRepository.setCloudSyncAccessToken(config.accessToken)
            settingsRepository.setCloudSyncRefreshToken(config.refreshToken)
            settingsRepository.setCloudSyncTokenExpiryTime(config.tokenExpiryTime)
            settingsRepository.setCloudSyncConflictResolution(config.conflictResolution.name)
            settingsRepository.setCloudSyncOAuthClientId(config.oauthClientId)
            settingsRepository.saveCloudSyncOAuthState(config.oauthState)
            settingsRepository.setCloudSyncUseCustomAuthEndpoint(config.useCustomAuthEndpoint)
            settingsRepository.setCloudSyncCustomAuthEndpoint(config.customAuthEndpoint)
            settingsRepository.setCloudSyncCustomTokenEndpoint(config.customTokenEndpoint)
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "ViewModel 保存配置失败")
        }
    }

    // ── 凭证校验 ──
    fun validateCredentials(): Boolean {
        val config = _config.value
        val error = when {
            config.serviceType == CloudSyncServiceType.ONEDRIVE ||
                config.serviceType == CloudSyncServiceType.GOOGLE_DRIVE -> {
                if (config.oauthClientId.isBlank()) application.getString(R.string.cs_validate_oauth_id) else null
            }
            else -> {
                when {
                    config.serverUrl.isBlank() -> application.getString(R.string.cs_validate_server_url)
                    config.username.isBlank() -> application.getString(R.string.cs_validate_username)
                    config.password.isBlank() -> application.getString(R.string.cs_validate_password)
                    else -> null
                }
            }
        }
        _credentialValidationError.value = error
        return error == null
    }

    fun clearCredentialError() {
        _credentialValidationError.value = null
    }

    // ── 测试连接 ──
    fun testConnection() {
        if (!validateCredentials()) return
        viewModelScope.launch {
            _isTesting.value = true
            try {
                val result = cloudSyncManager.testConnection(_config.value)
                _connectionTestResult.emit(result)
                Timber.tag("CloudSync").i("连接测试：${if (result.success) "成功" else "失败 - ${result.message}"}")
                if (result.success) {
                    loadStorageInfo()
                }
            } finally {
                _isTesting.value = false
            }
        }
    }

    // ── 执行同步 ──
    fun performSync() {
        if (!validateCredentials()) return
        Timber.tag("CloudSync").d("检查同步条件：仅WiFi=${_config.value.syncOnWifiOnly}, 仅充电=${_config.value.syncOnCharging}")
        viewModelScope.launch {
            _isSyncing.value = true
            _syncProgress.value = com.mdcito.app.data.sync.SyncProgress()
            try {
                val result = cloudSyncManager.syncWithProgress(_config.value) { progress ->
                    _syncProgress.value = progress
                }
                _syncResult.emit(result)
                Timber.tag("CloudSync").i("手动同步${if (result.success) "成功" else "失败"}：上传${result.uploadedCount}，下载${result.downloadedCount}")
                addSyncHistoryEntry(com.mdcito.app.data.sync.SyncHistoryEntry(
                    timestamp = System.currentTimeMillis(),
                    success = result.success,
                    uploadedCount = result.uploadedCount,
                    downloadedCount = result.downloadedCount,
                    skippedCount = result.skippedCount,
                    conflictCount = result.conflictCount,
                    errorMessage = result.errorMessage,
                ))
                // 同步完成后，直接更新内存中的状态字段，避免 DataStore 异步写入延迟导致读取到旧值
                val now = System.currentTimeMillis()
                val newStatus = if (result.success) "success" else "error: ${result.errorMessage}"
                _config.value = _config.value.copy(
                    lastSyncTime = now,
                    lastSyncStatus = newStatus,
                )
                // 取消可能存在的 debounce 保存任务，防止旧值覆盖
                saveJob?.cancel()
                // 立即保存以确保 lastSyncTime/lastSyncStatus 持久化
                saveConfig(_config.value)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // ── OAuth 授权流程 ──
    fun startOAuthFlow() {
        Timber.tag("CloudSync").i("发起 OAuth 授权")
        val currentConfig = _config.value
        if (currentConfig.oauthClientId.isBlank()) {
            _credentialValidationError.value = application.getString(R.string.cs_validate_oauth_id)
            return
        }
        _credentialValidationError.value = null
        val provider = cloudSyncManager.getProvider(currentConfig.serviceType)
        val authUrl = provider.getAuthorizationUrl(currentConfig)
        if (authUrl != null) {
            viewModelScope.launch {
                _oauthUrl.emit(authUrl)
            }
        }
    }

    fun handleOAuthCallback(code: String, callbackState: String? = null) {
        if (oauthProcessing) {
            Timber.tag("CloudSync").w("OAuth 回调正在处理中，忽略重复请求")
            return
        }
        oauthProcessing = true
        viewModelScope.launch {
            try {
                // 校验 state 参数，防止 CSRF 攻击
                val savedState = settingsRepository.getCloudSyncOAuthState()
                if (callbackState != null) {
                    if (savedState.isBlank() || callbackState.isBlank() || savedState != callbackState) {
                        settingsRepository.saveCloudSyncOAuthState("")
                        _oauthResult.emit(OAuthResult(success = false, errorMessage = application.getString(R.string.cs_oauth_state_mismatch)))
                        return@launch
                    }
                } else if (savedState.isNotBlank()) {
                    // 发起了带 state 的授权请求但回调中缺少 state，可能被篡改
                    settingsRepository.saveCloudSyncOAuthState("")
                    _oauthResult.emit(OAuthResult(success = false, errorMessage = application.getString(R.string.cs_oauth_state_missing)))
                    return@launch
                }
                // 清除已使用的 state
                settingsRepository.saveCloudSyncOAuthState("")

                val currentConfig = _config.value
                val provider = cloudSyncManager.getProvider(currentConfig.serviceType)
                val updatedConfig = provider.exchangeCodeForToken(currentConfig, code)
                updateConfigImmediate(updatedConfig)
                _oauthResult.emit(OAuthResult(success = true))
                Timber.tag("CloudSync").i("OAuth 授权成功")
                loadStorageInfo()
            } catch (e: Exception) {
                Timber.tag("CloudSync").e(e, "OAuth 授权失败")
                _oauthResult.emit(OAuthResult(success = false, errorMessage = e.message ?: application.getString(R.string.cs_oauth_auth_failed)))
            } finally {
                oauthProcessing = false
            }
        }
    }

    // ── 撤销 OAuth 授权 ──
    fun revokeOAuthAuthorization() {
        viewModelScope.launch {
            val provider = cloudSyncManager.getProvider(_config.value.serviceType)
            provider.revokeAuthorization(_config.value)
            val updatedConfig = _config.value.copy(
                accessToken = "",
                refreshToken = "",
                tokenExpiryTime = 0L,
            )
            updateConfigImmediate(updatedConfig)
            Timber.tag("CloudSync").i("撤销 OAuth 授权")
        }
    }

    // ── 存储空间查询 ──
    fun loadStorageInfo() {
        val config = _config.value
        // OAuth 服务需要 access token，WebDAV 需要用户名
        val hasAuth = when (config.serviceType) {
            CloudSyncServiceType.ONEDRIVE, CloudSyncServiceType.GOOGLE_DRIVE -> config.accessToken.isNotBlank()
            CloudSyncServiceType.WEBDAV -> config.username.isNotBlank()
            else -> false
        }
        if (!hasAuth) return
        viewModelScope.launch {
            _storageLoading.value = true
            _storageError.value = null
            try {
                val info = cloudSyncManager.getStorageInfo(config)
                if (info != null) {
                    _storageInfo.value = info
                    Timber.tag("CloudSync").d("存储空间查询成功: 已用 ${info.usedBytes}, 总计 ${info.totalBytes}")
                } else {
                    _storageError.value = "empty"
                    Timber.tag("CloudSync").w("存储空间查询返回空结果")
                }
            } catch (e: Exception) {
                _storageError.value = e.message ?: "unknown"
                Timber.tag("CloudSync").w(e, "存储空间查询失败")
            } finally {
                _storageLoading.value = false
            }
        }
    }

    // ── 便捷方法：更新单个字段 ──
    /**
     * 校验 OAuth 回调中的 state 参数，防止 CSRF 攻击
     * @return true 表示 state 校验通过
     */
    fun validateOAuthState(callbackState: String): Boolean {
        val savedState = settingsRepository.getCloudSyncOAuthState()
        if (savedState.isBlank() || callbackState.isBlank()) return false
        return savedState == callbackState
    }

    fun updateServiceType(serviceType: CloudSyncServiceType) {
        Timber.tag("CloudSync").d("切换同步服务类型：$serviceType")
        // 切换服务类型时清除旧凭据，避免数据混淆
        val currentConfig = _config.value
        val clearedConfig = currentConfig.copy(
            serviceType = serviceType,
            // 清除协议认证凭据
            serverUrl = "",
            port = 0,
            username = "",
            password = "",
            // 清除 OAuth 凭据
            accessToken = "",
            refreshToken = "",
            tokenExpiryTime = 0L,
            oauthClientId = "",
            oauthState = "",
            useCustomAuthEndpoint = false,
            customAuthEndpoint = "",
            customTokenEndpoint = "",
        )
        updateConfigImmediate(clearedConfig)
        _credentialValidationError.value = null
    }

    fun updateServerUrl(url: String) {
        _credentialValidationError.value = null
        updateConfig(_config.value.copy(serverUrl = url))
    }

    fun updatePort(port: Int) {
        _credentialValidationError.value = null
        val clampedPort = if (port in 1..65535) port else 0
        updateConfig(_config.value.copy(port = clampedPort))
    }

    fun updateUsername(username: String) {
        _credentialValidationError.value = null
        updateConfig(_config.value.copy(username = username))
    }

    fun updatePassword(password: String) {
        _credentialValidationError.value = null
        updateConfig(_config.value.copy(password = password))
    }

    fun updateRemotePath(path: String) {
        updateConfig(_config.value.copy(remotePath = path))
    }

    fun updateSyncMode(mode: SyncMode) {
        updateConfigImmediate(_config.value.copy(syncMode = mode))
        Timber.tag("CloudSync").d("同步模式切换为：${mode.name}")
        if (mode == SyncMode.AUTO && _config.value.isEnabled) {
            cloudSyncManager.scheduleAutoSync(
                _config.value.autoSyncIntervalMinutes,
                wifiOnly = _config.value.syncOnWifiOnly,
                chargingOnly = _config.value.syncOnCharging,
            )
        } else {
            cloudSyncManager.cancelAutoSync()
        }
    }

    fun updateAutoSyncInterval(minutes: Int) {
        updateConfig(_config.value.copy(autoSyncIntervalMinutes = minutes))
        if (_config.value.syncMode == SyncMode.AUTO && _config.value.isEnabled) {
            cloudSyncManager.scheduleAutoSync(
                minutes,
                wifiOnly = _config.value.syncOnWifiOnly,
                chargingOnly = _config.value.syncOnCharging,
            )
        }
    }

    fun updateWifiOnly(wifiOnly: Boolean) {
        Timber.tag("CloudSync").d("Wi-Fi 限制：${if (wifiOnly) "开启" else "关闭"}")
        updateConfig(_config.value.copy(syncOnWifiOnly = wifiOnly))
        if (_config.value.syncMode == SyncMode.AUTO && _config.value.isEnabled) {
            cloudSyncManager.scheduleAutoSync(
                _config.value.autoSyncIntervalMinutes,
                wifiOnly = wifiOnly,
                chargingOnly = _config.value.syncOnCharging,
            )
        }
    }

    fun updateChargingOnly(chargingOnly: Boolean) {
        Timber.tag("CloudSync").d("充电限制：${if (chargingOnly) "开启" else "关闭"}")
        updateConfig(_config.value.copy(syncOnCharging = chargingOnly))
        if (_config.value.syncMode == SyncMode.AUTO && _config.value.isEnabled) {
            cloudSyncManager.scheduleAutoSync(
                _config.value.autoSyncIntervalMinutes,
                wifiOnly = _config.value.syncOnWifiOnly,
                chargingOnly = chargingOnly,
            )
        }
    }

    fun updateFileFilterRules(rules: String) {
        Timber.tag("CloudSync").d("更新文件过滤规则：${_config.value.fileFilterRules}")
        updateConfig(_config.value.copy(fileFilterRules = rules))
    }

    fun updateEnabled(enabled: Boolean) {
        if (enabled && !validateCredentials()) return
        if (enabled) {
            // 启用同步前先测试连接，确保凭证有效
            viewModelScope.launch {
                _isTesting.value = true
                try {
                    val result = cloudSyncManager.testConnection(_config.value)
                    // 再次检查当前状态，防止用户在测试期间已禁用同步
                    if (!_config.value.isEnabled && result.success) {
                        updateConfigImmediate(_config.value.copy(isEnabled = true))
                        Timber.tag("CloudSync").i("启用云同步")
                        if (_config.value.syncMode == SyncMode.AUTO) {
                            cloudSyncManager.scheduleAutoSync(
                                _config.value.autoSyncIntervalMinutes,
                                wifiOnly = _config.value.syncOnWifiOnly,
                                chargingOnly = _config.value.syncOnCharging,
                            )
                        }
                    } else if (!result.success) {
                        _credentialValidationError.value = application.getString(R.string.cs_test_failed_check_config, result.message)
                    }
                } catch (e: Exception) {
                    _credentialValidationError.value = application.getString(R.string.cs_test_failed, e.message ?: "")
                } finally {
                    _isTesting.value = false
                }
            }
        } else {
            updateConfigImmediate(_config.value.copy(isEnabled = false))
            Timber.tag("CloudSync").i("禁用云同步")
            cloudSyncManager.cancelAutoSync()
        }
    }

    fun updateConflictResolution(resolution: ConflictResolution) {
        Timber.tag("CloudSync").d("冲突解决策略：$resolution")
        updateConfig(_config.value.copy(conflictResolution = resolution))
    }

    fun updateOAuthClientId(clientId: String) {
        _credentialValidationError.value = null
        updateConfig(_config.value.copy(oauthClientId = clientId))
    }

    fun updateUseCustomAuthEndpoint(use: Boolean) {
        updateConfig(_config.value.copy(useCustomAuthEndpoint = use))
    }

    fun updateCustomAuthEndpoint(endpoint: String) {
        updateConfig(_config.value.copy(customAuthEndpoint = endpoint))
    }

    fun updateCustomTokenEndpoint(endpoint: String) {
        updateConfig(_config.value.copy(customTokenEndpoint = endpoint))
    }

    fun updateSyncDeletions(enabled: Boolean) {
        Timber.tag("CloudSync").d("删除同步：${if (enabled) "开启" else "关闭"}")
        updateConfig(_config.value.copy(syncDeletions = enabled))
    }

    // ── 同步历史 ──
    private val _syncHistory = MutableStateFlow<List<com.mdcito.app.data.sync.SyncHistoryEntry>>(emptyList())
    val syncHistory: StateFlow<List<com.mdcito.app.data.sync.SyncHistoryEntry>> = _syncHistory.asStateFlow()

    private fun loadSyncHistory() {
        viewModelScope.launch {
            try {
                val json = settingsRepository.cloudSyncHistory.first()
                if (json.isNotBlank()) {
                    val array = org.json.JSONArray(json)
                    val entries = (0 until array.length()).mapNotNull { i ->
                        try {
                            val obj = array.getJSONObject(i)
                            com.mdcito.app.data.sync.SyncHistoryEntry(
                                timestamp = obj.optLong("timestamp", 0L),
                                success = obj.optBoolean("success", false),
                                uploadedCount = obj.optInt("uploadedCount", 0),
                                downloadedCount = obj.optInt("downloadedCount", 0),
                                skippedCount = obj.optInt("skippedCount", 0),
                                conflictCount = obj.optInt("conflictCount", 0),
                                errorMessage = obj.optString("errorMessage", ""),
                            )
                        } catch (_: Exception) { null }
                    }
                    _syncHistory.value = entries
                }
            } catch (e: Exception) {
                Timber.tag("CloudSync").w(e, "加载同步历史失败")
            }
        }
    }

    private fun addSyncHistoryEntry(entry: com.mdcito.app.data.sync.SyncHistoryEntry) {
        viewModelScope.launch {
            historyMutex.withLock {
                val current = _syncHistory.value.toMutableList()
                current.add(0, entry)
                val trimmed = current.take(20)
                _syncHistory.value = trimmed
                // 持久化到 DataStore
                val array = org.json.JSONArray()
                trimmed.forEach { e ->
                    val obj = org.json.JSONObject().apply {
                        put("timestamp", e.timestamp)
                        put("success", e.success)
                        put("uploadedCount", e.uploadedCount)
                        put("downloadedCount", e.downloadedCount)
                        put("skippedCount", e.skippedCount)
                        put("conflictCount", e.conflictCount)
                        if (e.errorMessage != null) put("errorMessage", e.errorMessage)
                    }
                    array.put(obj)
                }
                settingsRepository.setCloudSyncHistory(array.toString())
            }
        }
    }

    // ── 冲突解决 ──
    private val _conflictFiles = MutableStateFlow<List<ConflictFile>>(emptyList())
    val conflictFiles: StateFlow<List<ConflictFile>> = _conflictFiles.asStateFlow()

    /**
     * 设置冲突文件列表（同步完成后由 UI 传入）
     */
    fun setConflictFiles(files: List<ConflictFile>) {
        _conflictFiles.value = files
    }

    /**
     * 解决单个冲突：保留本地版本（上传覆盖远程）
     */
    fun resolveConflictKeepLocal(conflictFile: ConflictFile) {
        viewModelScope.launch {
            try {
                val config = _config.value
                val provider = cloudSyncManager.getProvider(config.serviceType)
                val workspacePath = settingsRepository.workspacePath.first() ?: return@launch
                val localFile = java.io.File(workspacePath, conflictFile.relativePath)
                if (localFile.exists()) {
                    val remotePath = "${config.remotePath.trimEnd('/')}/${conflictFile.relativePath}"
                    provider.uploadFile(config, remotePath, localFile.readBytes(), localFile.lastModified())
                }
                _conflictFiles.value = _conflictFiles.value.filter { it.relativePath != conflictFile.relativePath }
                Timber.tag("CloudSync").i("冲突解决-保留本地：${conflictFile.relativePath}")
            } catch (e: Exception) {
                Timber.tag("CloudSync").e(e, "冲突解决失败-保留本地")
                _connectionTestResult.emit(ConnectionTestResult(false, application.getString(R.string.cs_conflict_resolve_failed, e.message ?: "")))
            }
        }
    }

    /**
     * 解决单个冲突：保留远程版本（下载覆盖本地）
     */
    fun resolveConflictKeepRemote(conflictFile: ConflictFile) {
        viewModelScope.launch {
            try {
                val config = _config.value
                val provider = cloudSyncManager.getProvider(config.serviceType)
                val remotePath = "${config.remotePath.trimEnd('/')}/${conflictFile.relativePath}"
                val content = provider.downloadFile(config, remotePath)
                if (content != null) {
                    val workspacePath = settingsRepository.workspacePath.first() ?: return@launch
                    val localFile = java.io.File(workspacePath, conflictFile.relativePath)
                    localFile.parentFile?.mkdirs()
                    localFile.writeBytes(content)
                    // 保留远程文件的修改时间，避免下次同步时因时间戳不一致导致重复上传
                    localFile.setLastModified(conflictFile.remoteLastModified)
                }
                _conflictFiles.value = _conflictFiles.value.filter { it.relativePath != conflictFile.relativePath }
                Timber.tag("CloudSync").i("冲突解决-保留远程：${conflictFile.relativePath}")
            } catch (e: Exception) {
                Timber.tag("CloudSync").e(e, "冲突解决失败-保留远程")
                _connectionTestResult.emit(ConnectionTestResult(false, application.getString(R.string.cs_conflict_resolve_failed, e.message ?: "")))
            }
        }
    }

    /**
     * 批量解决冲突：全部保留本地版本
     */
    fun resolveAllConflictsKeepLocal() {
        val conflicts = _conflictFiles.value.toList()
        conflicts.forEach { conflict ->
            resolveConflictKeepLocal(conflict)
        }
    }

    /**
     * 批量解决冲突：全部保留远程版本
     */
    fun resolveAllConflictsKeepRemote() {
        val conflicts = _conflictFiles.value.toList()
        conflicts.forEach { conflict ->
            resolveConflictKeepRemote(conflict)
        }
    }
}

data class OAuthResult(
    val success: Boolean,
    val errorMessage: String? = null,
)
