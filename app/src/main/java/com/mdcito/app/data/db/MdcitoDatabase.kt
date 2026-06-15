package com.mdcito.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mdcito.app.data.db.converter.Converters
import com.mdcito.app.data.db.dao.FileDao
import com.mdcito.app.data.db.dao.HistoryDao
import com.mdcito.app.data.db.dao.VersionDao
import com.mdcito.app.data.db.entity.FileEntity
import com.mdcito.app.data.db.entity.HistoryEntity
import com.mdcito.app.data.db.entity.VersionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Database(
    entities = [
        FileEntity::class,
        HistoryEntity::class,
        VersionEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MdcitoDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao
    abstract fun historyDao(): HistoryDao
    abstract fun versionDao(): VersionDao

    suspend fun incrementalVacuum() {
        withContext(Dispatchers.IO) {
            openHelper.writableDatabase.execSQL("PRAGMA incremental_vacuum")
            // WAL 模式下需 checkpoint 才能将回收的页真正归还给文件系统
            openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
        }
    }

    suspend fun vacuum() {
        withContext(Dispatchers.IO) {
            openHelper.writableDatabase.execSQL("VACUUM")
        }
    }

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE files ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE files ADD COLUMN content TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 仅设置标志，VACUUM 不能在事务中执行，需在 onOpen 回调中完成
                db.execSQL("PRAGMA auto_vacuum = INCREMENTAL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 将 file_id 单列索引替换为 (file_id, created_at) 复合索引，优化修剪查询性能
                db.execSQL("DROP INDEX IF EXISTS index_versions_file_id")
                db.execSQL("CREATE INDEX index_versions_file_id_created_at ON versions (file_id, created_at)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE files ADD COLUMN is_imported INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
