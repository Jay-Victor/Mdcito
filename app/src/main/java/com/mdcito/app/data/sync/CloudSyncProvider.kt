package com.mdcito.app.data.sync

import java.io.InputStream

/**
 * 存储空间信息
 */
data class StorageInfo(
    val usedBytes: Long,
    val totalBytes: Long,
)

/**
 * 云同步提供者接口
 * 所有云存储服务的同步实现都遵循此接口
 */
interface CloudSyncProvider {

    /** 当前配置的服务类型 */
    val serviceType: CloudSyncServiceType

    /**
     * 测试连接是否可用
     * @param config 云同步配置
     * @return 连接测试结果
     */
    suspend fun testConnection(config: CloudSyncConfig): ConnectionTestResult

    /**
     * 列出远程目录下的文件
     * @param config 云同步配置
     * @param remotePath 远程目录路径
     * @return 远程文件信息列表
     */
    suspend fun listFiles(config: CloudSyncConfig, remotePath: String): List<RemoteFileInfo>

    /**
     * 上传文件到远程
     * @param config 云同步配置
     * @param remotePath 远程文件路径
     * @param content 文件内容
     * @param lastModified 最后修改时间（用于冲突检测）
     * @return 是否成功
     */
    suspend fun uploadFile(
        config: CloudSyncConfig,
        remotePath: String,
        content: ByteArray,
        lastModified: Long = System.currentTimeMillis(),
    ): Boolean

    /**
     * 下载远程文件
     * @param config 云同步配置
     * @param remotePath 远程文件路径
     * @return 文件内容，失败返回 null
     */
    suspend fun downloadFile(config: CloudSyncConfig, remotePath: String): ByteArray?

    /**
     * 删除远程文件
     * @param config 云同步配置
     * @param remotePath 远程文件路径
     * @return 是否成功
     */
    suspend fun deleteFile(config: CloudSyncConfig, remotePath: String): Boolean

    /**
     * 创建远程目录
     * @param config 云同步配置
     * @param remotePath 远程目录路径
     * @return 是否成功
     */
    suspend fun createDirectory(config: CloudSyncConfig, remotePath: String): Boolean

    /**
     * 获取远程文件信息
     * @param config 云同步配置
     * @param remotePath 远程文件路径
     * @return 文件信息，不存在返回 null
     */
    suspend fun getFileInfo(config: CloudSyncConfig, remotePath: String): RemoteFileInfo?

    /**
     * 确保 OAuth 令牌有效，必要时刷新
     * 仅适用于 OneDrive / Google Drive
     * @param config 当前配置
     * @return 更新后的配置（可能包含新的令牌）
     */
    suspend fun ensureValidToken(config: CloudSyncConfig): CloudSyncConfig = config

    /**
     * 获取 OAuth 授权 URL
     * 仅适用于 OneDrive / Google Drive
     * @param config 包含 clientId 等信息的配置
     * @return 授权 URL，不支持的返回 null
     */
    fun getAuthorizationUrl(config: CloudSyncConfig): String? = null

    /**
     * 用授权码交换令牌
     * 仅适用于 OneDrive / Google Drive
     * @param config 配置
     * @param authorizationCode 授权码
     * @return 更新后的配置（包含访问令牌和刷新令牌）
     */
    suspend fun exchangeCodeForToken(config: CloudSyncConfig, authorizationCode: String): CloudSyncConfig = config

    /**
     * 撤销 OAuth 授权（服务端撤销）
     * 默认实现为空，各 Provider 可覆盖以实现服务端撤销
     */
    suspend fun revokeAuthorization(config: CloudSyncConfig) {}

    /**
     * 获取远程存储空间信息
     * 默认返回 null（不支持查询）
     */
    suspend fun getStorageInfo(config: CloudSyncConfig): StorageInfo? = null
}
