package com.mdcito.app.data.repository

import com.mdcito.app.data.db.dao.FileDao
import com.mdcito.app.data.db.entity.FileEntity
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepository @Inject constructor(
    private val fileDao: FileDao,
) {

    fun getAllFiles(): Flow<List<FileEntity>> = fileDao.getAllFiles()

    fun getAllFolders(): Flow<List<FileEntity>> = fileDao.getAllFolders()

    fun getAll(): Flow<List<FileEntity>> = fileDao.getAll()

    fun getRootFiles(): Flow<List<FileEntity>> = fileDao.getRootFiles()

    fun getRootFolders(): Flow<List<FileEntity>> = fileDao.getRootFolders()

    fun getFilesByParent(parentId: Long): Flow<List<FileEntity>> = fileDao.getFilesByParent(parentId)

    fun getFoldersByParent(parentId: Long): Flow<List<FileEntity>> = fileDao.getFoldersByParent(parentId)

    fun getPinned(): Flow<List<FileEntity>> = fileDao.getPinned()

    fun getByParent(parentId: Long?): Flow<List<FileEntity>> = fileDao.getByParent(parentId)

    fun search(query: String): Flow<List<FileEntity>> = fileDao.search(query)

    fun getFileCount(): Flow<Int> = fileDao.getFileCount()

    fun getFolderCount(): Flow<Int> = fileDao.getFolderCount()

    fun getTotalSize(): Flow<Long?> = fileDao.getTotalSize()

    fun getNonImportedFileCount(): Flow<Int> = fileDao.getNonImportedFileCount()

    fun getNonImportedFolderCount(): Flow<Int> = fileDao.getNonImportedFolderCount()

    fun getNonImportedTotalSize(): Flow<Long?> = fileDao.getNonImportedTotalSize()

    suspend fun getFileById(id: Long): FileEntity? = fileDao.getById(id)

    suspend fun createFile(file: FileEntity): Long = fileDao.insert(file)

    suspend fun updateFile(file: FileEntity) = fileDao.update(file)

    suspend fun togglePinned(id: Long, pinned: Boolean) = fileDao.updatePinned(id, pinned)

    suspend fun deleteFile(file: FileEntity) {
        Timber.tag("FileRepo").w("删除文件: id=%d, name=%s", file.id, file.name)
        fileDao.delete(file)
    }

    suspend fun deleteByIds(ids: List<Long>) {
        Timber.tag("FileRepo").w("批量删除文件: ids=%s", ids)
        fileDao.deleteByIds(ids)
    }

    suspend fun deleteAll() {
        Timber.tag("FileRepo").w("删除所有文件和文件夹")
        fileDao.deleteAll()
    }

    suspend fun deleteAllFiles() {
        Timber.tag("FileRepo").w("删除所有文件")
        fileDao.deleteAllFiles()
    }

    suspend fun deleteAllFolders() {
        Timber.tag("FileRepo").w("删除所有文件夹")
        fileDao.deleteAllFolders()
    }

    suspend fun deleteRootFiles() {
        Timber.tag("FileRepo").w("删除根目录文件")
        fileDao.deleteRootFiles()
    }

    suspend fun deleteRootFolders() {
        Timber.tag("FileRepo").w("删除根目录文件夹")
        fileDao.deleteRootFolders()
    }

    suspend fun deleteFilesByParent(parentId: Long) {
        Timber.tag("FileRepo").w("删除父目录下文件: parentId=%d", parentId)
        fileDao.deleteFilesByParent(parentId)
    }

    suspend fun deleteFoldersByParent(parentId: Long) {
        Timber.tag("FileRepo").w("删除父目录下文件夹: parentId=%d", parentId)
        fileDao.deleteFoldersByParent(parentId)
    }
}
