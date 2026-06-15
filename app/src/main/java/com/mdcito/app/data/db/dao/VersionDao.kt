package com.mdcito.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mdcito.app.data.db.entity.VersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VersionDao {

    @Query("SELECT * FROM versions WHERE file_id = :fileId ORDER BY created_at DESC")
    fun getByFileId(fileId: Long): Flow<List<VersionEntity>>

    @Query("SELECT * FROM versions ORDER BY created_at DESC")
    fun getAllVersions(): Flow<List<VersionEntity>>

    @Query("SELECT * FROM versions WHERE id = :id")
    suspend fun getById(id: Long): VersionEntity?

    @Query("SELECT COUNT(*) FROM versions WHERE file_id = :fileId")
    fun getCountByFileId(fileId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(version: VersionEntity): Long

    @Delete
    suspend fun delete(version: VersionEntity)

    @Query("DELETE FROM versions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM versions WHERE file_id = :fileId")
    suspend fun deleteByFileId(fileId: Long): Int

    @Query("SELECT COUNT(*) FROM versions WHERE file_id = :fileId")
    suspend fun getCountByFileIdSuspend(fileId: Long): Int

    @Query("DELETE FROM versions WHERE created_at < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Query("DELETE FROM versions")
    suspend fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM versions")
    suspend fun getCount(): Int

    /**
     * 查询某文件超出保留数量的旧快照 ID。
     * 按 created_at 降序排列，跳过最新的 [keepCount] 条，返回其余的 ID。
     * 用于自动修剪：当快照数超过限制时，删除这些旧快照。
     */
    @Query("""
        SELECT id FROM versions
        WHERE file_id = :fileId
        ORDER BY created_at DESC
        LIMIT -1 OFFSET :keepCount
    """)
    suspend fun getExcessVersionIds(fileId: Long, keepCount: Int): List<Long>
}
