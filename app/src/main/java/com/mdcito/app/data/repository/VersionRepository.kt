package com.mdcito.app.data.repository

import com.mdcito.app.data.db.MdcitoDatabase
import com.mdcito.app.data.db.dao.VersionDao
import com.mdcito.app.data.db.entity.VersionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import androidx.room.withTransaction

@Singleton
class VersionRepository @Inject constructor(
    private val versionDao: VersionDao,
    private val database: MdcitoDatabase,
) {
    companion object {
        // 每文件默认保留的最大快照数量。当某文件的快照数超过此限制时，
        // 自动修剪最旧的快照，仅保留最新的 N 个。此策略在 insertAndTrim 中执行，
        // 确保快照不会无限增长占用存储空间。
        const val DEFAULT_MAX_VERSIONS_PER_FILE = 50
    }

    fun getByFileId(fileId: Long): Flow<List<VersionEntity>> = versionDao.getByFileId(fileId)

    fun getAllVersions(): Flow<List<VersionEntity>> = versionDao.getAllVersions()

    suspend fun getById(id: Long): VersionEntity? = versionDao.getById(id)

    fun getCountByFileId(fileId: Long): Flow<Int> = versionDao.getCountByFileId(fileId)

    suspend fun insert(version: VersionEntity): Long = versionDao.insert(version)

    /**
     * 插入新快照并自动修剪超出限制的旧快照。
     * 处理方式：在事务中插入新快照后，查询该文件按创建时间排序后超出 [maxPerFile] 限制的旧快照 ID，
     * 然后批量删除这些旧快照。默认每文件最多保留 50 个快照（[DEFAULT_MAX_VERSIONS_PER_FILE]）。
     * 使用事务确保插入和修剪的原子性，避免并发写入时的竞态条件。
     */
    suspend fun insertAndTrim(
        version: VersionEntity,
        maxPerFile: Int = DEFAULT_MAX_VERSIONS_PER_FILE
    ): Long {
        require(maxPerFile > 0) { "maxPerFile must be positive, was $maxPerFile" }
        return database.withTransaction {
            val id = versionDao.insert(version)
            trimVersionsForFile(version.fileId, maxPerFile)
            id
        }
    }

    /**
     * 修剪某文件的快照，保留最新的 [maxPerFile] 个，删除其余旧快照。
     * 通过 [VersionDao.getExcessVersionIds] 查询按 created_at DESC 排序后
     * 跳过前 [maxPerFile] 条的 ID，然后批量删除。
     */
    suspend fun trimVersionsForFile(fileId: Long, maxPerFile: Int = DEFAULT_MAX_VERSIONS_PER_FILE) {
        val excessIds = versionDao.getExcessVersionIds(fileId, maxPerFile)
        if (excessIds.isNotEmpty()) {
            versionDao.deleteByIds(excessIds)
        }
    }

    suspend fun delete(version: VersionEntity) = versionDao.delete(version)

    suspend fun deleteByIds(ids: List<Long>) {
        if (ids.isEmpty()) return
        // 分批处理，避免超出 SQLite SQLITE_MAX_VARIABLE_NUMBER 限制（默认 999）
        ids.chunked(500).forEach { chunk ->
            versionDao.deleteByIds(chunk)
        }
    }

    suspend fun deleteByFileId(fileId: Long): Int = versionDao.deleteByFileId(fileId)

    suspend fun getCountByFileIdSuspend(fileId: Long): Int = versionDao.getCountByFileIdSuspend(fileId)

    suspend fun deleteOlderThan(timestamp: Long) = versionDao.deleteOlderThan(timestamp)

    suspend fun deleteAll(): Int = versionDao.deleteAll()

    suspend fun getCount(): Int = versionDao.getCount()

    suspend fun incrementalVacuum() = database.incrementalVacuum()
}
