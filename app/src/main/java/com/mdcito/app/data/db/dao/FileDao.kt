package com.mdcito.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mdcito.app.data.db.entity.FileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {

    @Query("SELECT * FROM files WHERE type = 'file' ORDER BY updated_at DESC")
    fun getAllFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE type = 'folder' ORDER BY name ASC")
    fun getAllFolders(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files ORDER BY updated_at DESC")
    fun getAll(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE type = 'file' AND parent_id IS NULL ORDER BY updated_at DESC")
    fun getRootFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE type = 'folder' AND parent_id IS NULL ORDER BY name ASC")
    fun getRootFolders(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE type = 'file' AND parent_id = :parentId ORDER BY updated_at DESC")
    fun getFilesByParent(parentId: Long): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE type = 'folder' AND parent_id = :parentId ORDER BY name ASC")
    fun getFoldersByParent(parentId: Long): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE is_pinned = 1 ORDER BY updated_at DESC")
    fun getPinned(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE parent_id = :parentId ORDER BY name ASC")
    fun getByParent(parentId: Long?): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE id = :id")
    suspend fun getById(id: Long): FileEntity?

    @Query("SELECT * FROM files WHERE name LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<FileEntity>>

    @Query("SELECT COUNT(*) FROM files WHERE type = 'file'")
    fun getFileCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM files WHERE type = 'folder'")
    fun getFolderCount(): Flow<Int>

    @Query("SELECT SUM(size) FROM files")
    fun getTotalSize(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM files WHERE type = 'file' AND is_imported = 0")
    fun getNonImportedFileCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM files WHERE type = 'folder' AND is_imported = 0")
    fun getNonImportedFolderCount(): Flow<Int>

    @Query("SELECT SUM(size) FROM files WHERE is_imported = 0")
    fun getNonImportedTotalSize(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: FileEntity): Long

    @Update
    suspend fun update(file: FileEntity)

    @Query("UPDATE files SET is_pinned = :pinned WHERE id = :id")
    suspend fun updatePinned(id: Long, pinned: Boolean)

    @Delete
    suspend fun delete(file: FileEntity)

    @Query("DELETE FROM files WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM files")
    suspend fun deleteAll()

    @Query("DELETE FROM files WHERE type = 'file' AND is_imported = 0")
    suspend fun deleteAllFiles()

    @Query("DELETE FROM files WHERE type = 'folder' AND is_imported = 0")
    suspend fun deleteAllFolders()

    @Query("DELETE FROM files WHERE type = 'file' AND parent_id IS NULL AND is_imported = 0")
    suspend fun deleteRootFiles()

    @Query("DELETE FROM files WHERE type = 'folder' AND parent_id IS NULL AND is_imported = 0")
    suspend fun deleteRootFolders()

    @Query("DELETE FROM files WHERE type = 'file' AND parent_id = :parentId AND is_imported = 0")
    suspend fun deleteFilesByParent(parentId: Long)

    @Query("DELETE FROM files WHERE type = 'folder' AND parent_id = :parentId AND is_imported = 0")
    suspend fun deleteFoldersByParent(parentId: Long)
}
