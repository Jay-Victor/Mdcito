package com.mdcito.app.data.sync

import com.mdcito.app.R

/**
 * 云同步服务类型
 */
enum class CloudSyncServiceType(val displayName: String) {
    WEBDAV("WebDAV"),
    FTP("FTP"),
    FTPS("FTPS"),
    SFTP("SFTP"),
    ONEDRIVE("OneDrive"),
    GOOGLE_DRIVE("Google Drive"),
}

/**
 * 同步模式
 */
enum class SyncMode(val displayName: String, val displayNameResId: Int) {
    MANUAL("手动同步", R.string.cs_mode_manual_label),
    AUTO("自动同步", R.string.cs_mode_auto_label),
}

/**
 * 冲突解决策略
 */
enum class ConflictResolution(val displayName: String, val displayNameResId: Int) {
    NEWER_WINS("保留较新版本", R.string.cs_conflict_newer_label),
    LOCAL_WINS("保留本地版本", R.string.cs_conflict_local_label),
    REMOTE_WINS("保留远程版本", R.string.cs_conflict_remote_label),
    MANUAL("手动解决", R.string.cs_conflict_manual_label),
}

/**
 * 同步状态
 */
enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR,
}

/**
 * 云同步配置
 */
data class CloudSyncConfig(
    val serviceType: CloudSyncServiceType = CloudSyncServiceType.WEBDAV,
    val serverUrl: String = "",
    val port: Int = 0,
    val username: String = "",
    val password: String = "",
    val remotePath: String = "/Mdcito",
    val syncMode: SyncMode = SyncMode.MANUAL,
    val autoSyncIntervalMinutes: Int = 30,
    val syncOnWifiOnly: Boolean = true,
    val syncOnCharging: Boolean = false,
    val fileFilterRules: String = "",
    val lastSyncTime: Long = 0L,
    val lastSyncStatus: String = "",
    val isEnabled: Boolean = false,
    val accessToken: String = "",
    val refreshToken: String = "",
    val tokenExpiryTime: Long = 0L,
    val conflictResolution: ConflictResolution = ConflictResolution.NEWER_WINS,
    val oauthClientId: String = "",
    val oauthState: String = "",
    val useCustomAuthEndpoint: Boolean = false,
    val customAuthEndpoint: String = "",
    val customTokenEndpoint: String = "",
    val syncDeletions: Boolean = true,
    val lastSyncedFiles: String = "",
)

/**
 * 远程文件信息
 */
data class RemoteFileInfo(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val etag: String? = null,
)

/**
 * 同步操作结果
 */
data class SyncResult(
    val success: Boolean,
    val uploadedCount: Int = 0,
    val downloadedCount: Int = 0,
    val skippedCount: Int = 0,
    val conflictCount: Int = 0,
    val errorMessage: String? = null,
    val conflictFiles: List<ConflictFile> = emptyList(),
)

/**
 * 冲突文件信息
 */
data class ConflictFile(
    val relativePath: String,
    val localLastModified: Long,
    val remoteLastModified: Long,
    val localSize: Long,
    val remoteSize: Long,
)

/**
 * 连接测试结果
 */
data class ConnectionTestResult(
    val success: Boolean,
    val message: String,
    val serverInfo: String? = null,
)

/**
 * 同步进度信息
 */
data class SyncProgress(
    val currentFile: String = "",
    val processedCount: Int = 0,
    val totalCount: Int = 0,
    val uploadedCount: Int = 0,
    val downloadedCount: Int = 0,
) {
    val percentage: Int get() = if (totalCount > 0) (processedCount * 100 / totalCount) else 0
}

/**
 * 同步历史记录条目
 */
data class SyncHistoryEntry(
    val timestamp: Long,
    val success: Boolean,
    val uploadedCount: Int = 0,
    val downloadedCount: Int = 0,
    val skippedCount: Int = 0,
    val conflictCount: Int = 0,
    val errorMessage: String? = null,
)
