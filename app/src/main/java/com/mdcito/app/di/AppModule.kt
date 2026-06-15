package com.mdcito.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mdcito.app.data.db.MdcitoDatabase
import com.mdcito.app.data.db.dao.FileDao
import com.mdcito.app.data.db.dao.HistoryDao
import com.mdcito.app.data.db.dao.VersionDao
import com.mdcito.app.data.repository.FileRepository
import com.mdcito.app.data.repository.HistoryRepository
import com.mdcito.app.data.repository.SettingsRepository
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MdcitoDatabase {
        val prefs = context.getSharedPreferences("db_maintenance", Context.MODE_PRIVATE)
        return Room.databaseBuilder(
            context,
            MdcitoDatabase::class.java,
            "mdcito.db"
        )
            .addMigrations(MdcitoDatabase.MIGRATION_1_2, MdcitoDatabase.MIGRATION_2_3, MdcitoDatabase.MIGRATION_3_4, MdcitoDatabase.MIGRATION_4_5, MdcitoDatabase.MIGRATION_5_6)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // 迁移后需执行一次 VACUUM 激活 auto_vacuum = INCREMENTAL 模式
                    // VACUUM 是耗时操作，使用独立线程避免阻塞主线程
                    // WAL 模式下 VACUUM 后需 checkpoint(TRUNCATE) 才能真正回收磁盘空间
                    if (!prefs.getBoolean("auto_vacuum_activated", false)) {
                        Thread {
                            try {
                                db.execSQL("VACUUM")
                                db.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
                                prefs.edit().putBoolean("auto_vacuum_activated", true).apply()
                            } catch (_: Exception) {
                                // 失败则下次启动重试
                            }
                        }.start()
                    }
                }
            })
            .build()
    }

    @Provides
    fun provideFileDao(database: MdcitoDatabase): FileDao = database.fileDao()

    @Provides
    fun provideHistoryDao(database: MdcitoDatabase): HistoryDao = database.historyDao()

    @Provides
    fun provideVersionDao(database: MdcitoDatabase): VersionDao = database.versionDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideFileRepository(fileDao: FileDao): FileRepository {
        return FileRepository(fileDao)
    }

    @Provides
    @Singleton
    fun provideHistoryRepository(historyDao: HistoryDao): HistoryRepository {
        return HistoryRepository(historyDao)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        dataStore: DataStore<Preferences>,
        @ApplicationContext context: Context,
    ): SettingsRepository {
        val settingsDataStore = com.mdcito.app.data.datastore.SettingsDataStore(dataStore)
        val secureSettingsDataStore = com.mdcito.app.data.datastore.SecureSettingsDataStore(context)
        return SettingsRepository(settingsDataStore, secureSettingsDataStore)
    }
}
