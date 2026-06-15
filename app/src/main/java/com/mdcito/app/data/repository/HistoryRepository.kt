package com.mdcito.app.data.repository

import com.mdcito.app.data.db.dao.HistoryDao
import com.mdcito.app.data.db.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao,
) {

    fun getAll(): Flow<List<HistoryEntity>> = historyDao.getAll()

    fun getFileHistory(): Flow<List<HistoryEntity>> = historyDao.getFileHistory()

    fun getFolderHistory(): Flow<List<HistoryEntity>> = historyDao.getFolderHistory()

    fun getFileHistoryLatest(): Flow<List<HistoryEntity>> = historyDao.getFileHistoryLatest()

    fun getFolderHistoryLatest(): Flow<List<HistoryEntity>> = historyDao.getFolderHistoryLatest()

    fun getByFileId(fileId: Long, limit: Int = 10): Flow<List<HistoryEntity>> =
        historyDao.getByFileId(fileId, limit)

    fun getRecent(limit: Int = 5): Flow<List<HistoryEntity>> =
        historyDao.getRecent(limit)

    suspend fun recordAccess(fileId: Long, action: String = "open"): Long {
        return historyDao.insert(
            HistoryEntity(fileId = fileId, action = action)
        )
    }

    suspend fun delete(history: HistoryEntity) = historyDao.delete(history)

    suspend fun deleteByIds(ids: List<Long>) {
        if (ids.isEmpty()) return
        // 分批处理，避免超出 SQLite SQLITE_MAX_VARIABLE_NUMBER 限制（默认 999）
        ids.chunked(500).forEach { chunk ->
            historyDao.deleteByIds(chunk)
        }
    }

    suspend fun deleteByFileId(fileId: Long) = historyDao.deleteByFileId(fileId)

    suspend fun deleteAll() = historyDao.deleteAll()

    suspend fun deleteFileHistory() = historyDao.deleteFileHistory()

    suspend fun deleteFolderHistory() = historyDao.deleteFolderHistory()
}
