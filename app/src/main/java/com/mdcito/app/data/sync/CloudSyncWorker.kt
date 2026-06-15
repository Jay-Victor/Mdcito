package com.mdcito.app.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mdcito.app.data.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * 云同步后台 Worker
 * 使用 WorkManager 执行自动同步任务
 * Wi-Fi/充电约束由 WorkManager 的 Constraints 处理（在 scheduleAutoSync 中设置），
 * 此处保留运行时检查作为双重保障
 */
@HiltWorker
class CloudSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val cloudSyncManager: CloudSyncManager,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "cloud_sync_periodic"
    }

    override suspend fun doWork(): Result {
        Timber.tag("CloudSync").i("开始云同步任务")

        return try {
            // 检查是否启用
            val enabled = settingsRepository.cloudSyncEnabled.first()
            if (!enabled) {
                Timber.tag("CloudSync").d("云同步已禁用，跳过")
                return Result.success()
            }

            // 双重保障：检查 Wi-Fi 约束（主约束由 WorkManager Constraints 设置）
            val wifiOnly = settingsRepository.cloudSyncWifiOnly.first()
            if (wifiOnly) {
                val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as? android.net.ConnectivityManager
                val activeNetwork = cm?.activeNetwork
                val caps = activeNetwork?.let { cm.getNetworkCapabilities(it) }
                if (caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) != true) {
                    Timber.tag("CloudSync").d("不在 Wi-Fi 网络，跳过同步")
                    return Result.success()
                }
            }

            // 双重保障：检查充电约束
            val chargingOnly = settingsRepository.cloudSyncChargingOnly.first()
            if (chargingOnly) {
                val batteryIntent = applicationContext.registerReceiver(
                    null,
                    android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
                )
                val status = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
                val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == android.os.BatteryManager.BATTERY_STATUS_FULL
                if (!isCharging) {
                    Timber.tag("CloudSync").d("未在充电，跳过同步")
                    return Result.success()
                }
            }

            // 加载配置并执行同步
            val config = cloudSyncManager.loadConfig()
            val result = cloudSyncManager.sync(config)

            // 更新同步状态到 DataStore（修复：Worker 同步后也更新 UI 可见的同步状态）
            cloudSyncManager.updateSyncStatus(result.success, result.errorMessage)

            if (result.success) {
                Timber.tag("CloudSync").i("云同步完成：上传 ${result.uploadedCount}，下载 ${result.downloadedCount}，跳过 ${result.skippedCount}，冲突 ${result.conflictCount}")
                Result.success()
            } else {
                Timber.tag("CloudSync").w("云同步失败：${result.errorMessage}")
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.tag("CloudSync").e(e, "云同步任务异常")
            cloudSyncManager.updateSyncStatus(false, e.message)
            Result.retry()
        }
    }
}
