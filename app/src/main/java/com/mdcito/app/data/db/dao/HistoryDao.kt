package com.mdcito.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mdcito.app.data.db.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT h.* FROM history h ORDER BY h.accessed_at DESC")
    fun getAll(): Flow<List<HistoryEntity>>

    @Query("""
        SELECT h.* FROM history h 
        INNER JOIN files f ON h.file_id = f.id 
        WHERE f.type = 'file' 
        ORDER BY h.accessed_at DESC
    """)
    fun getFileHistory(): Flow<List<HistoryEntity>>

    @Query("""
        SELECT h.* FROM history h 
        INNER JOIN files f ON h.file_id = f.id 
        WHERE f.type = 'folder' 
        ORDER BY h.accessed_at DESC
    """)
    fun getFolderHistory(): Flow<List<HistoryEntity>>

    @Query("""
        SELECT h.* FROM history h
        INNER JOIN (
            SELECT file_id, MAX(accessed_at) AS max_accessed
            FROM history
            GROUP BY file_id
        ) h2 ON h.file_id = h2.file_id AND h.accessed_at = h2.max_accessed
        INNER JOIN files f ON h.file_id = f.id
        WHERE f.type = 'file'
        ORDER BY h.accessed_at DESC
    """)
    fun getFileHistoryLatest(): Flow<List<HistoryEntity>>

    @Query("""
        SELECT h.* FROM history h
        INNER JOIN (
            SELECT file_id, MAX(accessed_at) AS max_accessed
            FROM history
            GROUP BY file_id
        ) h2 ON h.file_id = h2.file_id AND h.accessed_at = h2.max_accessed
        INNER JOIN files f ON h.file_id = f.id
        WHERE f.type = 'folder'
        ORDER BY h.accessed_at DESC
    """)
    fun getFolderHistoryLatest(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE file_id = :fileId ORDER BY accessed_at DESC LIMIT :limit")
    fun getByFileId(fileId: Long, limit: Int = 10): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY accessed_at DESC LIMIT :limit")
    fun getRecent(limit: Int = 5): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity): Long

    @Delete
    suspend fun delete(history: HistoryEntity)

    @Query("DELETE FROM history WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM history WHERE file_id = :fileId")
    suspend fun deleteByFileId(fileId: Long)

    @Query("DELETE FROM history")
    suspend fun deleteAll()

    @Query("""
        DELETE FROM history WHERE file_id IN (
            SELECT id FROM files WHERE type = 'file'
        )
    """)
    suspend fun deleteFileHistory()

    @Query("""
        DELETE FROM history WHERE file_id IN (
            SELECT id FROM files WHERE type = 'folder'
        )
    """)
    suspend fun deleteFolderHistory()
}
