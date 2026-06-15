package com.mdcito.app.data.sync

import android.content.Context
import timber.log.Timber
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mdcito.app.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 云同步管理器
 * 负责协调各同步提供者，执行同步逻辑
 */
@Singleton
class CloudSyncManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val secureSettingsDataStore: com.mdcito.app.data.datastore.SecureSettingsDataStore,
    private val webDavSyncProvider: WebDavSyncProvider,
    private val ftpSyncProvider: FtpSyncProvider,
    private val oneDriveSyncProvider: OneDriveSyncProvider,
    private val googleDriveSyncProvider: GoogleDriveSyncProvider,
) {

    /** 并发同步保护锁 */
    private val syncMutex = Mutex()

    /** 当前是否正在同步 */
    val isSyncing: Boolean get() = syncMutex.isLocked

    /**
     * 调度自动同步任务
     */
    fun scheduleAutoSync(intervalMinutes: Int, wifiOnly: Boolean = false, chargingOnly: Boolean = false) {
        val workManager = WorkManager.getInstance(context)
        val effectiveInterval = intervalMinutes.coerceAtLeast(15).toLong()

        val constraintsBuilder = androidx.work.Constraints.Builder()
        if (wifiOnly) {
            constraintsBuilder.setRequiredNetworkType(androidx.work.NetworkType.UNMETERED)
        } else {
            constraintsBuilder.setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
        }
        if (chargingOnly) {
            constraintsBuilder.setRequiresCharging(true)
        }

        val periodicWork = PeriodicWorkRequestBuilder<CloudSyncWorker>(
            effectiveInterval,
            TimeUnit.MINUTES,
        )
            .setConstraints(constraintsBuilder.build())
            .build()
        workManager.enqueueUniquePeriodicWork(
            CloudSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWork,
        )
        Timber.tag("CloudSync").i("已调度自动同步，间隔 $effectiveInterval 分钟，仅WiFi=$wifiOnly，仅充电=$chargingOnly")
    }

    /**
     * 取消自动同步任务
     */
    fun cancelAutoSync() {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(CloudSyncWorker.WORK_NAME)
        Timber.tag("CloudSync").i("已取消自动同步")
    }

    /**
     * 获取当前配置对应的同步提供者
     */
    fun getProvider(serviceType: CloudSyncServiceType): CloudSyncProvider {
        return when (serviceType) {
            CloudSyncServiceType.WEBDAV -> webDavSyncProvider
            CloudSyncServiceType.FTP, CloudSyncServiceType.FTPS, CloudSyncServiceType.SFTP -> ftpSyncProvider
            CloudSyncServiceType.ONEDRIVE -> oneDriveSyncProvider
            CloudSyncServiceType.GOOGLE_DRIVE -> googleDriveSyncProvider
        }
    }

    /**
     * 测试连接
     */
    suspend fun testConnection(config: CloudSyncConfig): ConnectionTestResult {
        return try {
            val provider = getProvider(config.serviceType)
            provider.testConnection(config)
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "连接测试失败: ${config.serviceType.displayName} ${config.serverUrl}")
            ConnectionTestResult(false, e.message ?: "连接测试失败")
        }
    }

    /**
     * 获取远程存储空间信息
     */
    suspend fun getStorageInfo(config: CloudSyncConfig): StorageInfo? {
        val provider = getProvider(config.serviceType)
        return provider.getStorageInfo(config)
    }

    /**
     * 执行同步操作（带进度回调）
     * 使用 Mutex 防止并发同步
     */
    suspend fun syncWithProgress(
        config: CloudSyncConfig,
        onProgress: (SyncProgress) -> Unit = {},
    ): SyncResult = syncMutex.withLock {
        doSync(config, onProgress)
    }

    /**
     * 执行同步操作
     */
    suspend fun sync(config: CloudSyncConfig): SyncResult = syncMutex.withLock {
        doSync(config) {}
    }

    /**
     * 内部同步实现（统一逻辑，消除重复代码）
     */
    private suspend fun doSync(
        config: CloudSyncConfig,
        onProgress: (SyncProgress) -> Unit,
    ): SyncResult = withContext(Dispatchers.IO) {
        if (!config.isEnabled) {
            return@withContext SyncResult(false, errorMessage = "云同步未启用")
        }

        try {
            val provider = getProvider(config.serviceType)
            val updatedConfig = provider.ensureValidToken(config)

            if (updatedConfig.accessToken != config.accessToken ||
                updatedConfig.refreshToken != config.refreshToken
            ) {
                saveConfig(updatedConfig)
            }

            val workspacePath = settingsRepository.workspacePath.first()
                ?: return@withContext SyncResult(false, errorMessage = "工作区路径未设置")

            val localDir = File(workspacePath)
            if (!localDir.exists()) {
                return@withContext SyncResult(false, errorMessage = "本地工作区目录不存在")
            }

            provider.createDirectory(updatedConfig, updatedConfig.remotePath)

            val remoteFiles = listRemoteFilesRecursive(provider, updatedConfig, updatedConfig.remotePath)
                .filter { !it.isDirectory }
                .associateBy { it.path }

            val localFiles = collectLocalFiles(localDir)
            val filterRules = parseFilterRules(updatedConfig.fileFilterRules)

            // 解析上次同步的文件列表（用于删除同步判断）
            val lastSyncedSet = parseLastSyncedFiles(updatedConfig.lastSyncedFiles)

            val localRelativePaths = mutableSetOf<String>()
            val remoteRelativePaths = mutableSetOf<String>()

            val totalCount = localFiles.size + remoteFiles.size
            var processedCount = 0
            var uploadedCount = 0
            var downloadedCount = 0
            var skippedCount = 0
            var conflictCount = 0
            var deletedLocalCount = 0
            var deletedRemoteCount = 0
            val conflictFiles = mutableListOf<ConflictFile>()

            // ── 阶段1：处理本地文件（上传/跳过/冲突） ──
            for (localFile in localFiles) {
                val relativePath = localFile.relativeTo(localDir).path
                val remotePath = "${updatedConfig.remotePath.trimEnd('/')}/$relativePath"
                localRelativePaths.add(relativePath)

                if (shouldSkipFile(relativePath, filterRules)) {
                    skippedCount++
                    processedCount++
                    onProgress(SyncProgress(
                        currentFile = relativePath,
                        processedCount = processedCount,
                        totalCount = totalCount,
                        uploadedCount = uploadedCount,
                        downloadedCount = downloadedCount,
                    ))
                    continue
                }

                val remoteFile = remoteFiles.values.find { it.path == remotePath }
                if (remoteFile == null) {
                    // 远程不存在
                    val wasSyncedBefore = lastSyncedSet.contains(relativePath)
                    if (wasSyncedBefore && updatedConfig.syncDeletions) {
                        // 上次同步过但现在远程不存在 → 远程已删除 → 删除本地
                        if (localFile.delete()) {
                            deletedLocalCount++
                            Timber.tag("CloudSync").d("远程已删除，删除本地: $relativePath")
                        }
                    } else {
                        // 新文件或未开启删除同步 → 上传
                        val content = localFile.readBytes()
                        if (provider.uploadFile(updatedConfig, remotePath, content, localFile.lastModified())) {
                            uploadedCount++
                            // 上传成功后同步本地文件时间戳，确保下次同步时两端时间戳一致
                            syncLocalTimestampAfterUpload(provider, localFile)
                        }
                    }
                } else if (localFile.lastModified() > remoteFile.lastModified) {
                    when (updatedConfig.conflictResolution) {
                        ConflictResolution.NEWER_WINS, ConflictResolution.LOCAL_WINS -> {
                            val content = localFile.readBytes()
                            if (provider.uploadFile(updatedConfig, remotePath, content, localFile.lastModified())) {
                                uploadedCount++
                                // 上传成功后同步本地文件时间戳，确保下次同步时两端时间戳一致
                                syncLocalTimestampAfterUpload(provider, localFile)
                            }
                        }
                        ConflictResolution.REMOTE_WINS -> { skippedCount++ }
                        ConflictResolution.MANUAL -> {
                            conflictCount++
                            conflictFiles.add(ConflictFile(
                                relativePath = relativePath,
                                localLastModified = localFile.lastModified(),
                                remoteLastModified = remoteFile.lastModified,
                                localSize = localFile.length(),
                                remoteSize = remoteFile.size,
                            ))
                        }
                    }
                } else {
                    skippedCount++
                }
                processedCount++
                onProgress(SyncProgress(
                    currentFile = relativePath,
                    processedCount = processedCount,
                    totalCount = totalCount,
                    uploadedCount = uploadedCount,
                    downloadedCount = downloadedCount,
                ))
            }

            // ── 阶段2：处理远程文件（下载/跳过/冲突/删除同步） ──
            for ((_, remoteFile) in remoteFiles) {
                val basePath = updatedConfig.remotePath.trimEnd('/')
                val relativePath = if (remoteFile.path.startsWith(basePath)) {
                    remoteFile.path.substring(basePath.length).trimStart('/')
                } else {
                    remoteFile.name
                }
                remoteRelativePaths.add(relativePath)

                val localFile = File(localDir, relativePath)
                if (!localFile.exists()) {
                    // 本地不存在
                    val wasSyncedBefore = lastSyncedSet.contains(relativePath)
                    if (wasSyncedBefore && updatedConfig.syncDeletions) {
                        // 上次同步过但现在本地不存在 → 本地已删除 → 删除远程
                        if (provider.deleteFile(updatedConfig, remoteFile.path)) {
                            deletedRemoteCount++
                            Timber.tag("CloudSync").d("本地已删除，删除远程: $relativePath")
                        }
                    } else {
                        // 新文件或未开启删除同步 → 下载
                        val content = provider.downloadFile(updatedConfig, remoteFile.path)
                        if (content != null) {
                            localFile.parentFile?.mkdirs()
                            localFile.writeBytes(content)
                            // 保留远程文件的修改时间，避免下次同步时因时间戳不一致导致重复上传
                            localFile.setLastModified(remoteFile.lastModified)
                            downloadedCount++
                        }
                    }
                } else if (remoteFile.lastModified > localFile.lastModified()) {
                    when (updatedConfig.conflictResolution) {
                        ConflictResolution.NEWER_WINS, ConflictResolution.REMOTE_WINS -> {
                            val content = provider.downloadFile(updatedConfig, remoteFile.path)
                            if (content != null) {
                                localFile.writeBytes(content)
                                // 保留远程文件的修改时间，避免下次同步时因时间戳不一致导致重复上传
                                localFile.setLastModified(remoteFile.lastModified)
                                downloadedCount++
                            }
                        }
                        ConflictResolution.LOCAL_WINS -> { skippedCount++ }
                        ConflictResolution.MANUAL -> {
                            conflictCount++
                            conflictFiles.add(ConflictFile(
                                relativePath = relativePath,
                                localLastModified = localFile.lastModified(),
                                remoteLastModified = remoteFile.lastModified,
                                localSize = localFile.length(),
                                remoteSize = remoteFile.size,
                            ))
                        }
                    }
                }
                processedCount++
                onProgress(SyncProgress(
                    currentFile = relativePath,
                    processedCount = processedCount,
                    totalCount = totalCount,
                    uploadedCount = uploadedCount,
                    downloadedCount = downloadedCount,
                ))
            }

            // ── 阶段3：保存当前同步的文件列表（供下次删除同步使用） ──
            val currentSyncedFiles = (localRelativePaths + remoteRelativePaths).filter { relativePath ->
                !shouldSkipFile(relativePath, filterRules)
            }.sorted()
            saveLastSyncedFiles(currentSyncedFiles)

            Timber.tag("CloudSync").i("同步完成(${config.serviceType.displayName})：上传 $uploadedCount，下载 $downloadedCount，跳过 $skippedCount，冲突 $conflictCount，本地删除 $deletedLocalCount，远程删除 $deletedRemoteCount")
            SyncResult(
                success = true,
                uploadedCount = uploadedCount,
                downloadedCount = downloadedCount,
                skippedCount = skippedCount,
                conflictCount = conflictCount,
                conflictFiles = conflictFiles,
            )
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "同步失败(${config.serviceType.displayName})")
            val errorMessage = when {
                e.message?.contains("401") == true || e.message?.contains("403") == true ||
                    e.message?.contains("Auth", ignoreCase = true) == true ||
                    e.message?.contains("authentication", ignoreCase = true) == true ||
                    e.message?.contains("credential", ignoreCase = true) == true ||
                    e.message?.contains("登录", ignoreCase = true) == true ->
                    "认证失败，请检查认证信息"
                e.message?.contains("timeout", ignoreCase = true) == true ||
                    e.message?.contains("连接", ignoreCase = true) == true ->
                    "网络连接失败，请检查网络设置"
                else -> e.message ?: "同步失败"
            }
            SyncResult(false, errorMessage = errorMessage)
        }
    }

    /**
     * 上传成功后同步本地文件时间戳。
     * 对于 OneDrive 等无法设置远程修改时间的服务，上传后远程修改时间为上传时间，
     * 需要将本地文件时间戳同步为远程实际时间，避免下次同步时因时间戳不一致导致重复操作。
     */
    private fun syncLocalTimestampAfterUpload(provider: CloudSyncProvider, localFile: File) {
        if (provider is OneDriveSyncProvider) {
            val remoteModified = provider.lastUploadRemoteModified
            if (remoteModified > 0) {
                localFile.setLastModified(remoteModified)
                Timber.tag("CloudSync").d("OneDrive 上传后同步本地时间戳: ${localFile.name} -> $remoteModified")
            }
        }
        // Google Drive 和 WebDAV 支持设置远程修改时间，无需额外同步
    }

    /**
     * 递归收集本地文件
     */
    private fun collectLocalFiles(dir: File): List<File> {
        val files = mutableListOf<File>()
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                files.addAll(collectLocalFiles(file))
            } else {
                files.add(file)
            }
        }
        Timber.tag("CloudSync").d("收集本地文件: ${dir.absolutePath}, 共 ${files.size} 个")
        return files
    }

    /**
     * 递归列出远程目录中的文件（包含子目录）
     */
    private suspend fun listRemoteFilesRecursive(
        provider: CloudSyncProvider,
        config: CloudSyncConfig,
        remotePath: String,
    ): List<RemoteFileInfo> {
        val allFiles = mutableListOf<RemoteFileInfo>()
        val dirsToProcess = mutableListOf(remotePath)

        while (dirsToProcess.isNotEmpty()) {
            val currentDir = dirsToProcess.removeAt(0)
            val files = provider.listFiles(config, currentDir)
            for (file in files) {
                if (file.isDirectory) {
                    dirsToProcess.add(file.path)
                } else {
                    allFiles.add(file)
                }
            }
        }

        Timber.tag("CloudSync").d("递归列出远程文件: $remotePath, 共 ${allFiles.size} 个")
        return allFiles
    }

    /**
     * 解析文件过滤规则
     * 格式为 JSON 数组，每个元素是一个 glob 模式
     */
    private fun parseFilterRules(rulesJson: String): List<String> {
        if (rulesJson.isBlank()) return emptyList()
        return try {
            val array = org.json.JSONArray(rulesJson)
            (0 until array.length()).map { array.getString(it) }
        } catch (e: Exception) {
            Timber.tag("CloudSync").w(e, "解析过滤规则失败: $rulesJson")
            emptyList()
        }
    }

    /**
     * 解析上次同步的文件列表
     */
    private fun parseLastSyncedFiles(json: String): Set<String> {
        if (json.isBlank()) return emptySet()
        return try {
            val array = org.json.JSONArray(json)
            (0 until array.length()).mapTo(mutableSetOf()) { array.getString(it) }
        } catch (e: Exception) {
            Timber.tag("CloudSync").w(e, "解析上次同步文件列表失败")
            emptySet()
        }
    }

    /**
     * 保存当前同步的文件列表到 DataStore
     */
    private suspend fun saveLastSyncedFiles(files: List<String>) {
        val array = org.json.JSONArray()
        files.forEach { array.put(it) }
        settingsRepository.setCloudSyncLastSyncedFiles(array.toString())
    }

    /**
     * 判断文件是否应被跳过
     */
    private fun shouldSkipFile(relativePath: String, filterRules: List<String>): Boolean {
        if (filterRules.isEmpty()) return false
        val fileName = relativePath.substringAfterLast('/')
        for (rule in filterRules) {
            if (matchesGlob(fileName, rule) || matchesGlob(relativePath, rule)) {
                return true
            }
        }
        return false
    }

    /**
     * 简单的 glob 匹配
     */
    private fun matchesGlob(text: String, pattern: String): Boolean {
        if (pattern == "*") return true

        val regexStr = buildString {
            append('^')
            for (ch in pattern) {
                when (ch) {
                    '*' -> append(".*")
                    '?' -> append('.')
                    in ".()+|^$@%[]{}\\<>" -> {
                        append('\\')
                        append(ch)
                    }
                    else -> append(ch)
                }
            }
            append('$')
        }

        return try {
            val regex = regexStr.toRegex()
            regex.matches(text) || regex.matches(text.substringAfterLast('/'))
        } catch (e: Exception) {
            Timber.tag("CloudSync").w(e, "Glob 匹配失败")
            text == pattern
        }
    }

    /**
     * 保存配置到 DataStore
     */
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
            settingsRepository.setCloudSyncDeletions(config.syncDeletions)
            settingsRepository.setCloudSyncLastSyncedFiles(config.lastSyncedFiles)
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "保存配置失败: ${config.serviceType.displayName}")
        }
    }

    /**
     * 从 DataStore 读取配置
     */
    suspend fun loadConfig(): CloudSyncConfig = withContext(Dispatchers.IO) {
        try {
            CloudSyncConfig(
                serviceType = runCatching { CloudSyncServiceType.valueOf(settingsRepository.cloudSyncServiceType.first()) }
                    .getOrDefault(CloudSyncServiceType.WEBDAV),
                serverUrl = settingsRepository.cloudSyncServerUrl.first(),
                port = settingsRepository.cloudSyncPort.first(),
                username = settingsRepository.cloudSyncUsername.first(),
                password = settingsRepository.cloudSyncPassword.value,
                remotePath = settingsRepository.cloudSyncRemotePath.first(),
                syncMode = runCatching { SyncMode.valueOf(settingsRepository.cloudSyncMode.first()) }
                    .getOrDefault(SyncMode.MANUAL),
                autoSyncIntervalMinutes = settingsRepository.cloudSyncAutoSyncInterval.first(),
                syncOnWifiOnly = settingsRepository.cloudSyncWifiOnly.first(),
                syncOnCharging = settingsRepository.cloudSyncChargingOnly.first(),
                fileFilterRules = settingsRepository.cloudSyncFileFilterRules.first(),
                lastSyncTime = settingsRepository.cloudSyncLastSyncTime.first(),
                lastSyncStatus = settingsRepository.cloudSyncLastSyncStatus.first(),
                isEnabled = settingsRepository.cloudSyncEnabled.first(),
                accessToken = settingsRepository.cloudSyncAccessToken.value,
                refreshToken = settingsRepository.cloudSyncRefreshToken.value,
                tokenExpiryTime = settingsRepository.cloudSyncTokenExpiryTime.first(),
                conflictResolution = runCatching { ConflictResolution.valueOf(settingsRepository.cloudSyncConflictResolution.first()) }
                    .getOrDefault(ConflictResolution.NEWER_WINS),
                oauthClientId = settingsRepository.cloudSyncOAuthClientId.value,
                oauthState = settingsRepository.getCloudSyncOAuthState(),
                useCustomAuthEndpoint = settingsRepository.cloudSyncUseCustomAuthEndpoint.first(),
                customAuthEndpoint = settingsRepository.cloudSyncCustomAuthEndpoint.first(),
                customTokenEndpoint = settingsRepository.cloudSyncCustomTokenEndpoint.first(),
                syncDeletions = settingsRepository.cloudSyncDeletions.first(),
                lastSyncedFiles = settingsRepository.cloudSyncLastSyncedFiles.first(),
            )
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "加载配置失败")
            CloudSyncConfig()
        }
    }

    /**
     * 更新同步状态（供 Worker 调用）
     */
    suspend fun updateSyncStatus(success: Boolean, errorMessage: String?) {
        val now = System.currentTimeMillis()
        val status = if (success) "success" else "error: $errorMessage"
        settingsRepository.setCloudSyncLastSyncTime(now)
        settingsRepository.setCloudSyncLastSyncStatus(status)
    }

    /**
     * 格式化上次同步时间
     */
    fun formatLastSyncTime(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
