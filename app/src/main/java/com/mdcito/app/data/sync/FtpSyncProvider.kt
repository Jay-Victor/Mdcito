package com.mdcito.app.data.sync

import android.content.Context
import android.content.SharedPreferences
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import net.schmizz.sshj.common.KeyType
import net.schmizz.sshj.common.SecurityUtils
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPSClient
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.PublicKey
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class FtpSyncProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : CloudSyncProvider {

    companion object {
        private const val FTP_DEFAULT_PORT = 21
        private const val SFTP_DEFAULT_PORT = 22
        private const val KNOWN_HOSTS_PREFS = "sftp_known_hosts"
    }

    /**
     * 已知主机密钥存储，用于 SFTP 主机密钥验证。
     * 首次连接时自动接受并保存主机密钥指纹，后续连接时验证一致性。
     */
    private val knownHostsPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(KNOWN_HOSTS_PREFS, Context.MODE_PRIVATE)
    }

    /**
     * 自定义主机密钥验证器
     * - 首次连接：自动接受主机密钥并保存其指纹
     * - 后续连接：验证主机密钥指纹是否与已保存的一致
     * - 不一致时拒绝连接，防止中间人攻击
     */
    private inner class KnownHostsVerifier : HostKeyVerifier {
        override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
            val fingerprint = SecurityUtils.getFingerprint(key)
            val keyType = KeyType.fromKey(key).name
            val storageKey = "${hostname}:${port}"

            val savedFingerprint = knownHostsPrefs.getString(storageKey, null)

            if (savedFingerprint == null) {
                // 首次连接：保存主机密钥指纹
                Timber.tag("CloudSync").i("首次连接 $storageKey，保存主机密钥指纹 ($keyType)")
                knownHostsPrefs.edit().putString(storageKey, fingerprint).apply()
                return true
            }

            if (savedFingerprint == fingerprint) {
                Timber.tag("CloudSync").d("主机密钥验证通过：$storageKey ($keyType)")
                return true
            }

            Timber.tag("CloudSync").e("主机密钥验证失败：$storageKey！期望：$savedFingerprint，实际：$fingerprint")
            return false
        }

        override fun findExistingAlgorithms(hostname: String, port: Int): MutableList<String> {
            return mutableListOf()
        }
    }

    override val serviceType: CloudSyncServiceType = CloudSyncServiceType.FTP

    override suspend fun testConnection(config: CloudSyncConfig): ConnectionTestResult =
        withContext(Dispatchers.IO) {
            try {
                when (config.serviceType) {
                    CloudSyncServiceType.FTP, CloudSyncServiceType.FTPS -> testFtpConnection(config)
                    CloudSyncServiceType.SFTP -> testSftpConnection(config)
                    else -> ConnectionTestResult(false, "不支持的服务类型: ${config.serviceType.displayName}")
                }
            } catch (e: Exception) {
                Timber.tag("CloudSync").e(e, "连接测试失败: ${config.serviceType.displayName} ${config.serverUrl}:${config.port}")
                ConnectionTestResult(false, "连接失败: ${e.message}")
            }
        }

    override suspend fun listFiles(config: CloudSyncConfig, remotePath: String): List<RemoteFileInfo> =
        withContext(Dispatchers.IO) {
            try {
                when (config.serviceType) {
                    CloudSyncServiceType.FTP, CloudSyncServiceType.FTPS -> listFtpFiles(config, remotePath)
                    CloudSyncServiceType.SFTP -> listSftpFiles(config, remotePath)
                    else -> emptyList()
                }
            } catch (e: Exception) {
                Timber.tag("CloudSync").e(e, "列出远程文件失败: ${config.serviceType.displayName} $remotePath")
                throw RuntimeException("列出远程文件失败: ${e.message}", e)
            }
        }

    override suspend fun uploadFile(
        config: CloudSyncConfig,
        remotePath: String,
        content: ByteArray,
        lastModified: Long,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            when (config.serviceType) {
                CloudSyncServiceType.FTP, CloudSyncServiceType.FTPS -> uploadFtpFile(config, remotePath, content, lastModified)
                CloudSyncServiceType.SFTP -> uploadSftpFile(config, remotePath, content, lastModified)
                else -> false
            }
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "上传文件失败: ${config.serviceType.displayName} $remotePath")
            throw RuntimeException("上传文件失败: ${e.message}", e)
        }
    }

    override suspend fun downloadFile(config: CloudSyncConfig, remotePath: String): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                when (config.serviceType) {
                    CloudSyncServiceType.FTP, CloudSyncServiceType.FTPS -> downloadFtpFile(config, remotePath)
                    CloudSyncServiceType.SFTP -> downloadSftpFile(config, remotePath)
                    else -> null
                }
            } catch (e: Exception) {
                Timber.tag("CloudSync").e(e, "下载文件失败: ${config.serviceType.displayName} $remotePath")
                throw RuntimeException("下载文件失败: ${e.message}", e)
            }
        }

    override suspend fun deleteFile(config: CloudSyncConfig, remotePath: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                when (config.serviceType) {
                    CloudSyncServiceType.FTP, CloudSyncServiceType.FTPS -> deleteFtpFile(config, remotePath)
                    CloudSyncServiceType.SFTP -> deleteSftpFile(config, remotePath)
                    else -> false
                }
            } catch (e: Exception) {
                Timber.tag("CloudSync").e(e, "删除文件失败: ${config.serviceType.displayName} $remotePath")
                throw RuntimeException("删除文件失败: ${e.message}", e)
            }
        }

    override suspend fun createDirectory(config: CloudSyncConfig, remotePath: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                when (config.serviceType) {
                    CloudSyncServiceType.FTP, CloudSyncServiceType.FTPS -> createFtpDirectory(config, remotePath)
                    CloudSyncServiceType.SFTP -> createSftpDirectory(config, remotePath)
                    else -> false
                }
            } catch (e: Exception) {
                Timber.tag("CloudSync").e(e, "创建目录失败: ${config.serviceType.displayName} $remotePath")
                throw RuntimeException("创建目录失败: ${e.message}", e)
            }
        }

    override suspend fun getFileInfo(config: CloudSyncConfig, remotePath: String): RemoteFileInfo? =
        withContext(Dispatchers.IO) {
            try {
                when (config.serviceType) {
                    CloudSyncServiceType.FTP, CloudSyncServiceType.FTPS -> getFtpFileInfo(config, remotePath)
                    CloudSyncServiceType.SFTP -> getSftpFileInfo(config, remotePath)
                    else -> null
                }
            } catch (e: Exception) {
                Timber.tag("CloudSync").e(e, "获取文件信息失败: ${config.serviceType.displayName} $remotePath")
                throw RuntimeException("获取文件信息失败: ${e.message}", e)
            }
        }

    // ==================== FTP / FTPS ====================

    private fun createFtpClient(config: CloudSyncConfig): FTPClient {
        val client = if (config.serviceType == CloudSyncServiceType.FTPS) {
            // 使用显式 TLS (FTPES)：先以明文连接标准端口，再通过 AUTH TLS 升级为加密连接。
            // 这比隐式 TLS (FTPSClient(true), 端口 990) 兼容性更好，是现代 FTPS 服务器的默认模式。
            FTPSClient(false)
        } else {
            // 明文 FTP：凭据和数据均以明文传输，存在被窃听风险
            Timber.tag("CloudSync").w("使用明文 FTP 连接 %s:%d，凭据将以明文传输，建议改用 FTPS 或 SFTP",
                config.serverUrl, if (config.port != 0) config.port else FTP_DEFAULT_PORT)
            FTPClient()
        }
        val port = if (config.port != 0) config.port else FTP_DEFAULT_PORT
        Timber.tag("CloudSync").d("FTP 连接: ${config.serverUrl}:$port (${config.serviceType.displayName})")
        client.connect(config.serverUrl, port)
        if (config.serviceType == CloudSyncServiceType.FTPS) {
            // 显式 TLS 模式下，FTPSClient 会在 connect() 后自动发送 AUTH TLS 命令。
            // 此处配置数据通道保护：PBSZ=0 (无需填充), PROT=P (数据通道加密)
            (client as FTPSClient).execPBSZ(0)
            client.execPROT("P")
            Timber.tag("CloudSync").d("FTPS 安全设置完成: PBSZ=0, PROT=P (显式 TLS)")
        }
        if (!client.login(config.username, config.password)) {
            Timber.tag("CloudSync").e("FTP 登录失败: ${config.serverUrl}:$port, 用户: ${config.username}")
            client.disconnect()
            throw RuntimeException("FTP 登录失败")
        }
        client.enterLocalPassiveMode()
        client.setFileType(FTP.BINARY_FILE_TYPE)
        return client
    }

    private fun disconnectFtp(client: FTPClient) {
        try {
            if (client.isConnected) {
                client.logout()
                client.disconnect()
            }
        } catch (e: Exception) {
            Timber.tag("CloudSync").w(e, "FTP 断开连接时出错")
        }
    }

    private fun testFtpConnection(config: CloudSyncConfig): ConnectionTestResult {
        try {
            val ftp = createFtpClient(config)
            val serverInfo = ftp.systemType ?: "FTP 服务器"
            disconnectFtp(ftp)
            Timber.tag("CloudSync").i("FTP 连接测试成功: ${config.serverUrl}")
            // 明文 FTP 连接成功时，在返回消息中附加安全警告，提示用户凭据以明文传输
            val message = if (config.serviceType == CloudSyncServiceType.FTP) {
                "连接成功（警告：明文 FTP，凭据未加密，建议改用 FTPS/SFTP）"
            } else {
                "连接成功"
            }
            return ConnectionTestResult(true, message, serverInfo)
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "FTP 连接测试失败: ${config.serverUrl}")
            return ConnectionTestResult(false, "FTP 连接失败: ${e.message}")
        }
    }

    private fun listFtpFiles(config: CloudSyncConfig, remotePath: String): List<RemoteFileInfo> {
        val client = createFtpClient(config)
        try {
            val files = client.listFiles(remotePath) ?: return emptyList()
            val result = files.filter { it.name != "." && it.name != ".." }.map { file ->
                RemoteFileInfo(
                    path = if (remotePath.endsWith("/")) "$remotePath${file.name}" else "$remotePath/${file.name}",
                    name = file.name,
                    isDirectory = file.isDirectory,
                    size = file.size,
                    lastModified = file.timestamp.timeInMillis,
                )
            }
            Timber.tag("CloudSync").d("FTP 列出文件: $remotePath, 共 ${result.size} 个")
            return result
        } finally {
            disconnectFtp(client)
        }
    }

    private fun uploadFtpFile(config: CloudSyncConfig, remotePath: String, content: ByteArray, lastModified: Long): Boolean {
        val client = createFtpClient(config)
        try {
            val parentDir = remotePath.substringBeforeLast("/")
            if (parentDir.isNotEmpty()) {
                ensureFtpDirectoryExists(client, parentDir)
            }
            val inputStream = ByteArrayInputStream(content)
            val result = client.storeFile(remotePath, inputStream)
            if (result) {
                // 尝试通过 MFMT 命令设置远程文件的修改时间
                trySetFtpModifiedTime(client, remotePath, lastModified)
                Timber.tag("CloudSync").i("FTP 上传成功: $remotePath (${content.size} bytes)")
            } else {
                Timber.tag("CloudSync").e("FTP 上传失败: $remotePath, reply: ${client.replyString}")
            }
            return result
        } finally {
            disconnectFtp(client)
        }
    }

    private fun downloadFtpFile(config: CloudSyncConfig, remotePath: String): ByteArray? {
        val client = createFtpClient(config)
        try {
            val outputStream = ByteArrayOutputStream()
            val success = client.retrieveFile(remotePath, outputStream)
            if (success) {
                val data = outputStream.toByteArray()
                Timber.tag("CloudSync").i("FTP 下载成功: $remotePath (${data.size} bytes)")
                return data
            } else {
                Timber.tag("CloudSync").e("FTP 下载失败: $remotePath, reply: ${client.replyString}")
                return null
            }
        } finally {
            disconnectFtp(client)
        }
    }

    private fun deleteFtpFile(config: CloudSyncConfig, remotePath: String): Boolean {
        val client = createFtpClient(config)
        try {
            val result = client.deleteFile(remotePath)
            if (result) {
                Timber.tag("CloudSync").i("FTP 删除文件: $remotePath")
            } else {
                Timber.tag("CloudSync").e("FTP 删除文件失败: $remotePath, reply: ${client.replyString}")
            }
            return result
        } finally {
            disconnectFtp(client)
        }
    }

    private fun createFtpDirectory(config: CloudSyncConfig, remotePath: String): Boolean {
        val client = createFtpClient(config)
        try {
            val result = ensureFtpDirectoryExists(client, remotePath)
            if (result) {
                Timber.tag("CloudSync").i("FTP 创建目录成功: $remotePath")
            } else {
                Timber.tag("CloudSync").e("FTP 创建目录失败: $remotePath")
            }
            return result
        } finally {
            disconnectFtp(client)
        }
    }

    private fun ensureFtpDirectoryExists(client: FTPClient, dirPath: String): Boolean {
        val parts = dirPath.split("/").filter { it.isNotEmpty() }
        var currentPath = ""
        for (part in parts) {
            currentPath = "$currentPath/$part"
            try {
                if (!client.changeWorkingDirectory(currentPath)) {
                    if (!client.makeDirectory(currentPath)) {
                        return false
                    }
                }
                client.changeWorkingDirectory("/")
            } catch (e: Exception) {
                Timber.tag("CloudSync").w(e, "FTP 确保目录存在时出错: $currentPath")
                return false
            }
        }
        return true
    }

    /**
     * 尝试通过 MFMT 命令设置远程文件的修改时间。
     * MFMT 是 RFC 3659 定义的命令，并非所有 FTP 服务器都支持。
     * 不支持时静默失败，不影响上传结果。
     */
    private fun trySetFtpModifiedTime(client: FTPClient, remotePath: String, lastModified: Long) {
        try {
            val dateFormat = java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.US)
            dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val timeStr = dateFormat.format(java.util.Date(lastModified))
            // MFMT 命令格式：MFMT YYYYMMDDHHMMSS pathname
            val result = client.sendCommand("MFMT", "$timeStr $remotePath")
            if (result in 200..299) {
                Timber.tag("CloudSync").d("FTP MFMT 设置修改时间成功: $remotePath")
            } else {
                Timber.tag("CloudSync").d("FTP MFMT 不支持或失败: reply=${client.replyString.trim()}")
            }
        } catch (e: Exception) {
            Timber.tag("CloudSync").d("FTP MFMT 设置修改时间失败（服务器可能不支持）: $remotePath")
        }
    }

    private fun getFtpFileInfo(config: CloudSyncConfig, remotePath: String): RemoteFileInfo? {
        val client = createFtpClient(config)
        try {
            val parentDir = remotePath.substringBeforeLast("/")
            val fileName = remotePath.substringAfterLast("/")
            val files = client.listFiles(parentDir)
            val file = files.find { it.name == fileName } ?: return null
            val info = RemoteFileInfo(
                path = remotePath,
                name = file.name,
                isDirectory = file.isDirectory,
                size = file.size,
                lastModified = file.timestamp.timeInMillis,
            )
            Timber.tag("CloudSync").d("FTP 获取文件信息: $remotePath -> isDir=${info.isDirectory}, size=${info.size}")
            return info
        } finally {
            disconnectFtp(client)
        }
    }

    // ==================== SFTP ====================

    private fun <T> withSftp(config: CloudSyncConfig, block: (SSHClient, SFTPClient) -> T): T {
        val ssh = SSHClient()
        ssh.addHostKeyVerifier(KnownHostsVerifier())
        val port = if (config.port != 0) config.port else SFTP_DEFAULT_PORT
        Timber.tag("CloudSync").d("SFTP 连接: ${config.serverUrl}:$port")
        ssh.connect(config.serverUrl, port)
        try {
            try {
                ssh.authPassword(config.username, config.password)
            } catch (e: Exception) {
                Timber.tag("CloudSync").e(e, "SFTP 认证失败: ${config.serverUrl}:$port, 用户: ${config.username}")
                throw e
            }
            val sftp = ssh.newSFTPClient()
            try {
                return block(ssh, sftp)
            } finally {
                try { sftp.close() } catch (e: Exception) {
                    Timber.tag("CloudSync").w(e, "SFTP 关闭客户端时出错")
                }
            }
        } finally {
            try { ssh.disconnect() } catch (e: Exception) {
                Timber.tag("CloudSync").w(e, "SFTP 断开连接时出错")
            }
        }
    }

    private fun testSftpConnection(config: CloudSyncConfig): ConnectionTestResult {
        return try {
            withSftp(config) { ssh, _ ->
                Timber.tag("CloudSync").i("SFTP 连接测试成功: ${config.serverUrl}")
                ConnectionTestResult(true, "连接成功", "SFTP 服务器")
            }
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "SFTP 连接测试失败: ${config.serverUrl}")
            ConnectionTestResult(false, "SFTP 连接失败: ${e.message}")
        }
    }

    private fun listSftpFiles(config: CloudSyncConfig, remotePath: String): List<RemoteFileInfo> {
        return withSftp(config) { _, sftp ->
            val entries = sftp.ls(remotePath) ?: return@withSftp emptyList()
            val result = entries.filter { it.name != "." && it.name != ".." }.map { entry ->
                RemoteFileInfo(
                    path = if (remotePath.endsWith("/")) "$remotePath${entry.name}" else "$remotePath/${entry.name}",
                    name = entry.name,
                    isDirectory = entry.isDirectory,
                    size = entry.attributes.size,
                    lastModified = entry.attributes.mtime * 1000L,
                )
            }
            Timber.tag("CloudSync").d("SFTP 列出文件: $remotePath, 共 ${result.size} 个")
            result
        }
    }

    private fun uploadSftpFile(config: CloudSyncConfig, remotePath: String, content: ByteArray, lastModified: Long): Boolean {
        return withSftp(config) { _, sftp ->
            val parentDir = remotePath.substringBeforeLast("/")
            if (parentDir.isNotEmpty()) {
                ensureSftpDirectoryExists(sftp, parentDir)
            }
            val tempFile = java.io.File(context.cacheDir, "mdcito_upload_${System.currentTimeMillis()}.tmp")
            try {
                tempFile.writeBytes(content)
                sftp.put(tempFile.absolutePath, remotePath)
                // 设置远程文件的修改时间
                try {
                    val mtime = (lastModified / 1000).toInt()
                    val attrs = net.schmizz.sshj.sftp.FileAttributes.Builder()
                        .withAtimeMtime(mtime.toLong(), mtime.toLong())
                        .build()
                    sftp.setattr(remotePath, attrs)
                    Timber.tag("CloudSync").d("SFTP 设置修改时间: $remotePath -> $lastModified")
                } catch (e: Exception) {
                    Timber.tag("CloudSync").w(e, "SFTP 设置修改时间失败: $remotePath")
                }
                Timber.tag("CloudSync").i("SFTP 上传成功: $remotePath (${content.size} bytes)")
                true
            } finally {
                tempFile.delete()
            }
        }
    }

    private fun downloadSftpFile(config: CloudSyncConfig, remotePath: String): ByteArray? {
        return withSftp(config) { _, sftp ->
            val tempFile = java.io.File(context.cacheDir, "mdcito_download_${System.currentTimeMillis()}.tmp")
            try {
                sftp.get(remotePath, tempFile.absolutePath)
                val data = tempFile.readBytes()
                Timber.tag("CloudSync").i("SFTP 下载成功: $remotePath (${data.size} bytes)")
                data
            } finally {
                tempFile.delete()
            }
        }
    }

    private fun deleteSftpFile(config: CloudSyncConfig, remotePath: String): Boolean {
        return withSftp(config) { _, sftp ->
            sftp.rm(remotePath)
            Timber.tag("CloudSync").i("SFTP 删除文件: $remotePath")
            true
        }
    }

    private fun createSftpDirectory(config: CloudSyncConfig, remotePath: String): Boolean {
        return withSftp(config) { _, sftp ->
            ensureSftpDirectoryExists(sftp, remotePath)
            Timber.tag("CloudSync").i("SFTP 创建目录成功: $remotePath")
            true
        }
    }

    private fun ensureSftpDirectoryExists(sftp: SFTPClient, dirPath: String) {
        val parts = dirPath.split("/").filter { it.isNotEmpty() }
        var currentPath = ""
        for (part in parts) {
            currentPath = "$currentPath/$part"
            try {
                sftp.stat(currentPath)
            } catch (_: Exception) {
                Timber.tag("CloudSync").d("SFTP 目录不存在，创建: $currentPath")
                sftp.mkdirs(currentPath)
            }
        }
    }

    private fun getSftpFileInfo(config: CloudSyncConfig, remotePath: String): RemoteFileInfo? {
        return withSftp(config) { _, sftp ->
            val attrs = sftp.stat(remotePath)
            val name = remotePath.substringAfterLast("/")
            val info = RemoteFileInfo(
                path = remotePath,
                name = name,
                isDirectory = attrs.type == net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY,
                size = attrs.size,
                lastModified = attrs.mtime * 1000L,
            )
            Timber.tag("CloudSync").d("SFTP 获取文件信息: $remotePath -> isDir=${info.isDirectory}, size=${info.size}")
            info
        }
    }
}
