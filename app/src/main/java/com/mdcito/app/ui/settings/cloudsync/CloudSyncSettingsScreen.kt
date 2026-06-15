package com.mdcito.app.ui.settings.cloudsync

import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mdcito.app.R
import com.mdcito.app.data.sync.CloudSyncConfig
import com.mdcito.app.data.sync.CloudSyncServiceType
import com.mdcito.app.data.sync.ConflictFile
import com.mdcito.app.data.sync.ConflictResolution
import com.mdcito.app.data.sync.StorageInfo
import com.mdcito.app.data.sync.SyncMode
import com.mdcito.app.ui.components.MdcitoCardDefaults
import com.mdcito.app.ui.settings.SettingsActionItem
import com.mdcito.app.ui.settings.SettingsGroupTitle
import com.mdcito.app.ui.settings.SettingsItemCard
import com.mdcito.app.ui.settings.SettingsTopBar
import com.mdcito.app.ui.settings.SwitchSetting
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudSyncSettingsScreen(
    onNavigateBack: () -> Unit,
    oauthCode: String? = null,
    oauthState: String? = null,
    viewModel: CloudSyncViewModel = hiltViewModel(),
) {
    val config by viewModel.config.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncProgress by viewModel.syncProgress.collectAsState()
    val conflictFiles by viewModel.conflictFiles.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // 监听连接测试结果
    LaunchedEffect(Unit) {
        viewModel.connectionTestResult.collect { result ->
            val msg = if (result.success) {
                context.getString(R.string.cloud_sync_test_success)
            } else {
                context.getString(R.string.cloud_sync_test_failed, result.message)
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // 监听同步结果 → Toast 展示
    LaunchedEffect(Unit) {
        viewModel.syncResult.collect { result ->
            if (result.success) {
                val detail = context.getString(
                    R.string.cloud_sync_sync_detail,
                    result.uploadedCount,
                    result.downloadedCount,
                    result.skippedCount,
                    result.conflictCount,
                )
                Toast.makeText(context, detail, Toast.LENGTH_LONG).show()
                // 如果有冲突文件，显示冲突解决界面
                if (result.conflictFiles.isNotEmpty()) {
                    viewModel.setConflictFiles(result.conflictFiles)
                }
            } else {
                val msg = context.getString(
                    R.string.cloud_sync_sync_failed,
                    result.errorMessage ?: "",
                )
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    // 监听 OAuth 授权 URL，使用 Custom Tabs 打开
    LaunchedEffect(Unit) {
        viewModel.oauthUrl.collect { url ->
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(context, Uri.parse(url))
        }
    }

    // 监听凭证校验错误
    val credentialError by viewModel.credentialValidationError.collectAsState()

    // 处理 OAuth 回调 deep link 中的 code 和 state
    LaunchedEffect(oauthCode) {
        oauthCode?.let { code ->
            viewModel.handleOAuthCallback(code, oauthState)
        }
    }

    // 监听 OAuth 授权结果
    LaunchedEffect(Unit) {
        viewModel.oauthResult.collect { result ->
            val msg = if (result.success) {
                context.getString(R.string.cloud_sync_oauth_authorized)
            } else {
                result.errorMessage ?: context.getString(R.string.cloud_sync_oauth_not_authorized)
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ExtendedFloatingActionButton(
                    onClick = { if (!isTesting) viewModel.testConnection() },
                    icon = {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Link,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                    text = { Text(stringResource(R.string.cloud_sync_test_connection)) },
                    containerColor = Color(0xFF9E9E9E),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                )
                ExtendedFloatingActionButton(
                    onClick = { if (!isSyncing) viewModel.performSync() },
                    icon = {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                    text = {
                        if (isSyncing && syncProgress.totalCount > 0) {
                            Text("${syncProgress.percentage}%")
                        } else {
                            Text(stringResource(R.string.cloud_sync_now))
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                )
            }
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = innerPadding.calculateBottomPadding() + 100.dp),
        ) {
            SettingsTopBar(
                title = stringResource(R.string.cloud_sync_settings),
                onNavigateBack = onNavigateBack,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── 同步状态总览卡片 ──
            SyncStatusCard(
                config = config,
                storageInfo = viewModel.storageInfo.collectAsState().value,
                storageLoading = viewModel.storageLoading.collectAsState().value,
                storageError = viewModel.storageError.collectAsState().value,
                onRetryStorage = { viewModel.loadStorageInfo() },
                onToggleEnabled = { viewModel.updateEnabled(it) },
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 凭证校验错误提示
            AnimatedVisibility(
                visible = credentialError != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                credentialError?.let { error ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // ── 服务类型 ──
            SettingsGroupTitle(stringResource(R.string.cloud_sync_service_type))

            ServiceTypeSection(
                currentType = config.serviceType,
                config = config,
                onTypeChange = { viewModel.updateServiceType(it) },
            )

            // FTP 安全警告
            AnimatedVisibility(
                visible = config.serviceType == CloudSyncServiceType.FTP,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFFEBEE))
                            .border(
                                1.dp,
                                Color(0xFFE53935).copy(alpha = 0.3f),
                                RoundedCornerShape(10.dp),
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.cloud_sync_ftp_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC62828),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── 认证信息 ──
            SettingsGroupTitle(stringResource(R.string.cloud_sync_auth_info))

            if (config.serviceType == CloudSyncServiceType.ONEDRIVE ||
                config.serviceType == CloudSyncServiceType.GOOGLE_DRIVE
            ) {
                // OAuth 认证
                OAuthAuthSection(
                    config = config,
                    onClientIdChange = { viewModel.updateOAuthClientId(it) },
                    onAuthorizeClick = { viewModel.startOAuthFlow() },
                    onRevokeAuthClick = { viewModel.revokeOAuthAuthorization() },
                    onCustomEndpointToggle = { viewModel.updateUseCustomAuthEndpoint(it) },
                    onCustomAuthEndpointChange = { viewModel.updateCustomAuthEndpoint(it) },
                    onCustomTokenEndpointChange = { viewModel.updateCustomTokenEndpoint(it) },
                )
            } else {
                // 协议认证（WebDAV/FTP/FTPS/SFTP）
                ProtocolAuthSection(
                    config = config,
                    onServerUrlChange = { viewModel.updateServerUrl(it) },
                    onPortChange = { viewModel.updatePort(it) },
                    onUsernameChange = { viewModel.updateUsername(it) },
                    onPasswordChange = { viewModel.updatePassword(it) },
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── 同步设置 ──
            SettingsGroupTitle(stringResource(R.string.cloud_sync_sync_settings))

            // 远程路径
            RemotePathSection(
                currentPath = config.remotePath,
                onPathChange = { viewModel.updateRemotePath(it) },
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 同步模式
            SyncModeSection(
                currentMode = config.syncMode,
                onModeChange = { viewModel.updateSyncMode(it) },
            )

            // 自动同步间隔（仅自动模式显示）
            AnimatedVisibility(
                visible = config.syncMode == SyncMode.AUTO,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    AutoSyncIntervalSection(
                        currentInterval = config.autoSyncIntervalMinutes,
                        onIntervalChange = { viewModel.updateAutoSyncInterval(it) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SwitchSetting(
                title = stringResource(R.string.cloud_sync_wifi_only),
                subtitle = stringResource(R.string.cloud_sync_wifi_only_desc),
                checked = config.syncOnWifiOnly,
            ) { viewModel.updateWifiOnly(it) }

            Spacer(modifier = Modifier.height(8.dp))

            SwitchSetting(
                title = stringResource(R.string.cloud_sync_charging_only),
                subtitle = stringResource(R.string.cloud_sync_charging_only_desc),
                checked = config.syncOnCharging,
            ) { viewModel.updateChargingOnly(it) }

            Spacer(modifier = Modifier.height(20.dp))

            // ── 高级设置 ──
            SettingsGroupTitle(stringResource(R.string.cloud_sync_advanced))

            ConflictResolutionSection(
                currentResolution = config.conflictResolution,
                onResolutionChange = { viewModel.updateConflictResolution(it) },
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 删除同步开关
            SwitchSetting(
                title = stringResource(R.string.cloud_sync_sync_deletions),
                subtitle = stringResource(R.string.cloud_sync_sync_deletions_desc),
                checked = config.syncDeletions,
            ) { viewModel.updateSyncDeletions(it) }

            Spacer(modifier = Modifier.height(8.dp))

            FileFilterSection(
                currentRules = config.fileFilterRules,
                onRulesChange = { viewModel.updateFileFilterRules(it) },
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── 同步历史 ──
            SettingsGroupTitle(stringResource(R.string.cloud_sync_history_title))

            val syncHistory by viewModel.syncHistory.collectAsState()
            SettingsItemCard {
                if (syncHistory.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.cloud_sync_history_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 8.dp),
                    ) {
                        syncHistory.take(5).forEachIndexed { index, entry ->
                            if (index > 0) {
                                androidx.compose.material3.HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (entry.success) Icons.Outlined.Check else Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (entry.success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (entry.success) {
                                            "↑${entry.uploadedCount} ↓${entry.downloadedCount}"
                                        } else {
                                            entry.errorMessage?.take(30) ?: stringResource(R.string.cloud_sync_result_failed)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text(
                                    text = formatLastSyncTime(entry.timestamp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // 冲突解决弹窗
        if (conflictFiles.isNotEmpty()) {
            ConflictResolutionDialog(
                conflictFiles = conflictFiles,
                onKeepLocal = { viewModel.resolveConflictKeepLocal(it) },
                onKeepRemote = { viewModel.resolveConflictKeepRemote(it) },
                onKeepAllLocal = { viewModel.resolveAllConflictsKeepLocal() },
                onKeepAllRemote = { viewModel.resolveAllConflictsKeepRemote() },
                onDismiss = { viewModel.setConflictFiles(emptyList()) },
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// 同步状态总览卡片
// ════════════════════════════════════════════════════════════════

/**
 * 同步状态总览卡片
 */
@Composable
private fun SyncStatusCard(
    config: CloudSyncConfig,
    storageInfo: StorageInfo?,
    storageLoading: Boolean,
    storageError: String?,
    onRetryStorage: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
) {
    SettingsItemCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 14.dp),
        ) {
            // 标题行：同步状态 + 启用/禁用标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (config.isEnabled) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff,
                        contentDescription = null,
                        tint = if (config.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.cloud_sync_status),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.W600,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (config.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = if (config.isEnabled) stringResource(R.string.cloud_sync_status_enabled) else stringResource(R.string.cloud_sync_status_disabled),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (config.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.W600,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 上次同步时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.cloud_sync_last_sync),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatLastSyncTime(config.lastSyncTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // 上次同步结果
            if (config.lastSyncStatus.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.cloud_sync_last_result),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val isSuccess = config.lastSyncStatus.startsWith("success", ignoreCase = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Outlined.Check else Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = if (isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSuccess) stringResource(R.string.cloud_sync_result_success) else stringResource(R.string.cloud_sync_result_failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.W500,
                        )
                    }
                }
            }

            // 下次自动同步时间（仅自动模式且已启用时显示）
            if (config.isEnabled && config.syncMode == SyncMode.AUTO) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.cloud_sync_next_auto),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatNextSyncTime(config.lastSyncTime, config.autoSyncIntervalMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // 存储空间使用情况
            val showStorageSection = config.serviceType == CloudSyncServiceType.ONEDRIVE ||
                config.serviceType == CloudSyncServiceType.GOOGLE_DRIVE ||
                config.serviceType == CloudSyncServiceType.WEBDAV
            val hasAuth = if (config.serviceType == CloudSyncServiceType.WEBDAV) {
                config.username.isNotBlank()
            } else {
                config.accessToken.isNotBlank()
            }
            if (showStorageSection && hasAuth) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.cloud_sync_storage_usage) + ": ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    when {
                        storageLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.cloud_sync_storage_loading),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        storageError != null -> {
                            Text(
                                text = stringResource(R.string.cloud_sync_storage_error),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = onRetryStorage,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.cloud_sync_storage_retry),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                        storageInfo != null -> {
                            Text(
                                text = stringResource(
                                    R.string.cloud_sync_storage_info,
                                    formatFileSize(storageInfo.usedBytes),
                                    formatFileSize(storageInfo.totalBytes),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 启用/禁用同步开关
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleEnabled(!config.isEnabled) }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.cloud_sync_enabled),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.W500,
                    )
                    Text(
                        text = stringResource(R.string.cloud_sync_enabled_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                androidx.compose.material3.Switch(
                    checked = config.isEnabled,
                    onCheckedChange = onToggleEnabled,
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// 下拉选择组件（参照 MarkdownDialectSection 风格）
// ════════════════════════════════════════════════════════════════

/**
 * 服务类型选择下拉列表
 */
@Composable
private fun ServiceTypeSection(
    currentType: CloudSyncServiceType,
    config: CloudSyncConfig,
    onTypeChange: (CloudSyncServiceType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showSwitchConfirmDialog by remember { mutableStateOf(false) }
    var pendingType by remember { mutableStateOf<CloudSyncServiceType?>(null) }

    val hasCredentials = config.serverUrl.isNotEmpty() ||
        config.username.isNotEmpty() ||
        config.password.isNotEmpty() ||
        config.accessToken.isNotEmpty()

    val options = CloudSyncServiceType.entries.map { type ->
        Triple(type, type.displayName, getServiceTypeDescription(type))
    }

    val currentDisplayName = options.find { it.first == currentType }?.second ?: currentType.displayName

    Column {
        SettingsItemCard(onClick = { expanded = !expanded }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = MdcitoCardDefaults.CompactContentPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.cloud_sync_service_type),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.cloud_sync_service_type_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = currentDisplayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(tween(200)),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(tween(150)),
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(vertical = 6.dp),
            ) {
                options.forEachIndexed { index, (type, name, desc) ->
                    val isSelected = currentType == type

                    val selectedBgColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                        animationSpec = tween(200),
                        label = "service_bg_$index",
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(selectedBgColor)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp),
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable {
                                if (hasCredentials && type != currentType) {
                                    pendingType = type
                                    showSwitchConfirmDialog = true
                                } else {
                                    onTypeChange(type)
                                }
                                expanded = false
                            }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isSelected) {
                                            Modifier.background(MaterialTheme.colorScheme.primary)
                                        } else {
                                            Modifier.border(
                                                1.5.dp,
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                CircleShape,
                                            )
                                        }
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 10.dp),
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W400,
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    if (index < options.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp)
                                .height(0.5.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        )
                    }
                }
            }
        }
    }

    // 切换服务类型确认弹窗
    if (showSwitchConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showSwitchConfirmDialog = false
                pendingType = null
            },
            title = {
                Text(stringResource(R.string.cloud_sync_switch_type_confirm_title))
            },
            text = {
                Text(stringResource(R.string.cloud_sync_switch_type_confirm_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingType?.let { onTypeChange(it) }
                        showSwitchConfirmDialog = false
                        pendingType = null
                    },
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSwitchConfirmDialog = false
                        pendingType = null
                    },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

/**
 * 同步模式选择下拉列表
 */
@Composable
private fun SyncModeSection(
    currentMode: SyncMode,
    onModeChange: (SyncMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val options = SyncMode.entries.map { mode ->
        Triple(mode, stringResource(mode.displayNameResId), getSyncModeDescription(mode))
    }

    val currentDisplayName = options.find { it.first == currentMode }?.second ?: stringResource(currentMode.displayNameResId)

    Column {
        SettingsItemCard(onClick = { expanded = !expanded }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = MdcitoCardDefaults.CompactContentPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.cloud_sync_mode),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.cloud_sync_mode_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = currentDisplayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(tween(200)),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(tween(150)),
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(vertical = 6.dp),
            ) {
                options.forEachIndexed { index, (mode, name, desc) ->
                    val isSelected = currentMode == mode

                    val selectedBgColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                        animationSpec = tween(200),
                        label = "mode_bg_$index",
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(selectedBgColor)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp),
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable {
                                onModeChange(mode)
                                expanded = false
                            }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isSelected) {
                                            Modifier.background(MaterialTheme.colorScheme.primary)
                                        } else {
                                            Modifier.border(
                                                1.5.dp,
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                CircleShape,
                                            )
                                        }
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 10.dp),
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W400,
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    if (index < options.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp)
                                .height(0.5.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 冲突解决策略选择下拉列表
 */
@Composable
private fun ConflictResolutionSection(
    currentResolution: ConflictResolution,
    onResolutionChange: (ConflictResolution) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val options = ConflictResolution.entries.map { resolution ->
        Triple(resolution, stringResource(resolution.displayNameResId), getConflictResolutionDescription(resolution))
    }

    val currentDisplayName = options.find { it.first == currentResolution }?.second ?: stringResource(currentResolution.displayNameResId)

    Column {
        SettingsItemCard(onClick = { expanded = !expanded }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = MdcitoCardDefaults.CompactContentPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.cloud_sync_conflict_resolution),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.cloud_sync_conflict_resolution_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = currentDisplayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(tween(200)),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(tween(150)),
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(vertical = 6.dp),
            ) {
                options.forEachIndexed { index, (resolution, name, desc) ->
                    val isSelected = currentResolution == resolution

                    val selectedBgColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                        animationSpec = tween(200),
                        label = "conflict_bg_$index",
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(selectedBgColor)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp),
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable {
                                onResolutionChange(resolution)
                                expanded = false
                            }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isSelected) {
                                            Modifier.background(MaterialTheme.colorScheme.primary)
                                        } else {
                                            Modifier.border(
                                                1.5.dp,
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                CircleShape,
                                            )
                                        }
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 10.dp),
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W400,
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    if (index < options.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp)
                                .height(0.5.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        )
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// 认证信息组件
// ════════════════════════════════════════════════════════════════

/**
 * 协议认证（WebDAV/FTP/FTPS/SFTP）
 */
@Composable
private fun ProtocolAuthSection(
    config: com.mdcito.app.data.sync.CloudSyncConfig,
    onServerUrlChange: (String) -> Unit,
    onPortChange: (Int) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
) {
    var serverUrl by remember(config.serverUrl) { mutableStateOf(config.serverUrl) }
    var port by remember(config.port) { mutableStateOf(if (config.port == 0) "" else config.port.toString()) }
    var username by remember(config.username) { mutableStateOf(config.username) }
    var password by remember(config.password) { mutableStateOf(config.password) }
    var passwordVisible by remember { mutableStateOf(false) }

    SettingsItemCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 14.dp),
        ) {
            // 服务器地址
            Text(
                text = stringResource(R.string.cloud_sync_server_url),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W500,
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = serverUrl,
                onValueChange = {
                    serverUrl = it
                    onServerUrlChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.cloud_sync_server_url_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
            )

            if (serverUrl.startsWith("http://")) {
                Text(
                    text = stringResource(R.string.cloud_sync_http_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 端口
            Text(
                text = stringResource(R.string.cloud_sync_port),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W500,
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = port,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }
                    port = filtered
                    val portNum = filtered.toIntOrNull() ?: 0
                    onPortChange(if (portNum in 1..65535) portNum else 0)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.cloud_sync_port_default),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 用户名
            Text(
                text = stringResource(R.string.cloud_sync_username),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W500,
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    onUsernameChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 密码
            Text(
                text = stringResource(R.string.cloud_sync_password),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W500,
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    onPasswordChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(
                            text = if (passwordVisible) stringResource(R.string.cloud_sync_password_hide) else stringResource(R.string.cloud_sync_password_show),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * OAuth 认证（OneDrive/Google Drive）
 */
@Composable
private fun OAuthAuthSection(
    config: com.mdcito.app.data.sync.CloudSyncConfig,
    onClientIdChange: (String) -> Unit,
    onAuthorizeClick: () -> Unit,
    onRevokeAuthClick: () -> Unit,
    onCustomEndpointToggle: (Boolean) -> Unit,
    onCustomAuthEndpointChange: (String) -> Unit,
    onCustomTokenEndpointChange: (String) -> Unit,
) {
    val context = LocalContext.current
    var clientId by remember(config.oauthClientId) { mutableStateOf(config.oauthClientId) }
    var customAuthEndpoint by remember(config.customAuthEndpoint) { mutableStateOf(config.customAuthEndpoint) }
    var customTokenEndpoint by remember(config.customTokenEndpoint) { mutableStateOf(config.customTokenEndpoint) }
    val isAuthorized = config.accessToken.isNotEmpty()

    SettingsItemCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 14.dp),
        ) {
            // OAuth 客户端 ID
            Text(
                text = stringResource(R.string.cloud_sync_oauth_client_id),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W500,
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = clientId,
                onValueChange = {
                    clientId = it
                    onClientIdChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.cloud_sync_oauth_client_id_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
            )

            // OAuth 帮助链接
            val helpUrl = when (config.serviceType) {
                CloudSyncServiceType.ONEDRIVE -> "https://learn.microsoft.com/en-us/azure/active-directory/develop/quickstart-register-app"
                CloudSyncServiceType.GOOGLE_DRIVE -> "https://developers.google.com/identity/oauth2/web/guides/get-google-api-clientid"
                else -> null
            }
            if (helpUrl != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(helpUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.cloud_sync_oauth_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 授权按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.cloud_sync_oauth_authorize),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = if (isAuthorized) stringResource(R.string.cloud_sync_oauth_authorized) else stringResource(R.string.cloud_sync_oauth_not_authorized),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isAuthorized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isAuthorized) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { onRevokeAuthClick() }) {
                            Text(
                                text = stringResource(R.string.cloud_sync_oauth_revoke),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                } else {
                    TextButton(onClick = onAuthorizeClick) {
                        Text(
                            text = stringResource(R.string.cloud_sync_oauth_authorize),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 自定义端点开关
            SwitchSetting(
                title = stringResource(R.string.cloud_sync_custom_endpoint),
                subtitle = stringResource(R.string.cloud_sync_custom_endpoint_desc),
                checked = config.useCustomAuthEndpoint,
            ) { onCustomEndpointToggle(it) }

            // 自定义端点输入
            AnimatedVisibility(
                visible = config.useCustomAuthEndpoint,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.cloud_sync_custom_auth_endpoint),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.W500,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customAuthEndpoint,
                        onValueChange = {
                            customAuthEndpoint = it
                            onCustomAuthEndpointChange(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.cloud_sync_custom_token_endpoint),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.W500,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customTokenEndpoint,
                        onValueChange = {
                            customTokenEndpoint = it
                            onCustomTokenEndpointChange(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// 其他设置组件
// ════════════════════════════════════════════════════════════════

/**
 * 远程路径输入
 */
@Composable
private fun RemotePathSection(
    currentPath: String,
    onPathChange: (String) -> Unit,
) {
    var path by remember(currentPath) { mutableStateOf(currentPath) }

    SettingsItemCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 14.dp),
        ) {
            Text(
                text = stringResource(R.string.cloud_sync_remote_path),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W500,
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = path,
                onValueChange = {
                    path = it
                    onPathChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.cloud_sync_remote_path_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * 自动同步间隔滑块（离散档位：15, 30, 60, 120, 240, 480 分钟）
 */
@Composable
private fun AutoSyncIntervalSection(
    currentInterval: Int,
    onIntervalChange: (Int) -> Unit,
) {
    val intervalSteps = listOf(15, 30, 60, 120, 240, 480)
    // 找到最接近的档位索引
    val currentIndex = intervalSteps.indexOfFirst { it >= currentInterval }.coerceIn(0, intervalSteps.lastIndex)
    val selectedInterval = intervalSteps[currentIndex]

    SettingsItemCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.cloud_sync_auto_interval),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "${selectedInterval}${stringResource(R.string.cloud_sync_minutes)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.W600,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "15",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = currentIndex.toFloat(),
                    onValueChange = { index ->
                        val step = intervalSteps[index.toInt()]
                        onIntervalChange(step)
                    },
                    valueRange = 0f..(intervalSteps.size - 1).toFloat(),
                    steps = intervalSteps.size - 2,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "480",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 文件过滤规则（可视化逐行编辑）
 */
@Composable
private fun FileFilterSection(
    currentRules: String,
    onRulesChange: (String) -> Unit,
) {
    // 将 JSON 数组解析为逐行文本用于显示
    val displayText = remember(currentRules) {
        parseJsonArrayToLines(currentRules)
    }
    var showDialog by remember { mutableStateOf(false) }
    // 弹窗中的编辑文本（逐行格式）
    var editLines by remember(currentRules) { mutableStateOf(parseJsonArrayToLines(currentRules)) }

    SettingsActionItem(
        icon = Icons.Outlined.Settings,
        title = stringResource(R.string.cloud_sync_file_filter),
        subtitle = if (currentRules.isEmpty()) stringResource(R.string.cloud_sync_file_filter_desc) else displayText.take(40),
        onClick = {
            editLines = parseJsonArrayToLines(currentRules)
            showDialog = true
        },
    )

    if (showDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.cloud_sync_file_filter),
                    fontWeight = FontWeight.W600,
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.cloud_sync_filter_syntax_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editLines,
                        onValueChange = { editLines = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 8,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // 将逐行文本转换回 JSON 数组格式
                    val jsonArray = linesToJsonArray(editLines)
                    onRulesChange(jsonArray)
                    showDialog = false
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

/**
 * 将 JSON 数组字符串解析为逐行纯文本
 * 例如：["*.tmp","*.log"] → "*.tmp\n*.log"
 */
private fun parseJsonArrayToLines(json: String): String {
    if (json.isBlank()) return ""
    return try {
        val array = JSONArray(json)
        (0 until array.length()).joinToString("\n") { array.getString(it) }
    } catch (_: Exception) {
        // 如果不是有效的 JSON 数组，按原样返回（兼容旧数据）
        json
    }
}

/**
 * 将逐行纯文本转换为 JSON 数组字符串
 * 例如："*.tmp\n*.log" → ["*.tmp","*.log"]
 */
private fun linesToJsonArray(lines: String): String {
    if (lines.isBlank()) return ""
    val items = lines.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    if (items.isEmpty()) return ""
    val array = JSONArray()
    items.forEach { array.put(it) }
    return array.toString()
}

// ════════════════════════════════════════════════════════════════
// 辅助函数
// ════════════════════════════════════════════════════════════════

@Composable
private fun getServiceTypeDescription(type: CloudSyncServiceType): String = when (type) {
    CloudSyncServiceType.WEBDAV -> stringResource(R.string.cs_desc_webdav)
    CloudSyncServiceType.FTP -> stringResource(R.string.cs_desc_ftp)
    CloudSyncServiceType.FTPS -> stringResource(R.string.cs_desc_ftps)
    CloudSyncServiceType.SFTP -> stringResource(R.string.cs_desc_sftp)
    CloudSyncServiceType.ONEDRIVE -> stringResource(R.string.cs_desc_onedrive)
    CloudSyncServiceType.GOOGLE_DRIVE -> stringResource(R.string.cs_desc_google_drive)
}

@Composable
private fun getSyncModeDescription(mode: SyncMode): String = when (mode) {
    SyncMode.MANUAL -> stringResource(R.string.cs_desc_sync_manual)
    SyncMode.AUTO -> stringResource(R.string.cs_desc_sync_auto)
}

@Composable
private fun getConflictResolutionDescription(resolution: ConflictResolution): String = when (resolution) {
    ConflictResolution.NEWER_WINS -> stringResource(R.string.cs_desc_conflict_newer)
    ConflictResolution.LOCAL_WINS -> stringResource(R.string.cs_desc_conflict_local)
    ConflictResolution.REMOTE_WINS -> stringResource(R.string.cs_desc_conflict_remote)
    ConflictResolution.MANUAL -> stringResource(R.string.cs_desc_conflict_manual)
}

@Composable
private fun formatLastSyncTime(timestamp: Long): String {
    if (timestamp == 0L) return stringResource(R.string.cs_sync_never)
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
private fun formatNextSyncTime(lastSyncTime: Long, intervalMinutes: Int): String {
    if (lastSyncTime == 0L) return stringResource(R.string.cs_sync_imminent)
    val nextTime = lastSyncTime + intervalMinutes * 60_000L
    val remainingMinutes = ((nextTime - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0)
    return when {
        remainingMinutes <= 0 -> stringResource(R.string.cs_sync_imminent)
        remainingMinutes < 60 -> stringResource(R.string.cs_sync_minutes_about, remainingMinutes)
        else -> stringResource(R.string.cs_sync_hours_about, remainingMinutes / 60)
    }
}

// ════════════════════════════════════════════════════════════════
// 冲突解决弹窗
// ════════════════════════════════════════════════════════════════

@Composable
private fun ConflictResolutionDialog(
    conflictFiles: List<ConflictFile>,
    onKeepLocal: (ConflictFile) -> Unit,
    onKeepRemote: (ConflictFile) -> Unit,
    onKeepAllLocal: () -> Unit,
    onKeepAllRemote: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.cloud_sync_conflict_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.cloud_sync_conflict_message, conflictFiles.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(conflictFiles) { conflict ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                        ) {
                            Text(
                                text = conflict.relativePath,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val localTime = formatLastSyncTime(conflict.localLastModified)
                            val remoteTime = formatLastSyncTime(conflict.remoteLastModified)
                            Text(
                                text = "${stringResource(R.string.cloud_sync_conflict_local)} $localTime (${formatFileSize(conflict.localSize)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "${stringResource(R.string.cloud_sync_conflict_remote)} $remoteTime (${formatFileSize(conflict.remoteSize)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { onKeepLocal(conflict) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(stringResource(R.string.cloud_sync_conflict_keep_local))
                                }
                                FilledTonalButton(
                                    onClick = { onKeepRemote(conflict) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(stringResource(R.string.cloud_sync_conflict_keep_remote))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onKeepAllLocal) {
                    Text(stringResource(R.string.cloud_sync_conflict_keep_all_local))
                }
                FilledTonalButton(onClick = onKeepAllRemote) {
                    Text(stringResource(R.string.cloud_sync_conflict_keep_all_remote))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cloud_sync_conflict_close))
                }
            }
        },
    )
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    }
}
