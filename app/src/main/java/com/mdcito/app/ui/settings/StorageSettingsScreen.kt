package com.mdcito.app.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mdcito.app.R
import com.mdcito.app.data.locale.LanguageHelper
import com.mdcito.app.ui.components.MdcitoCardDefaults
import com.mdcito.app.ui.components.MdcitoConfirmDialog

@Composable
fun StorageSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    LanguageHelper.LocalLocaleVersion.current
    val workspacePath by viewModel.workspacePath.collectAsState()
    val cacheSize by viewModel.cacheSize.collectAsState()
    val snapshotCount by viewModel.snapshotCount.collectAsState()
    val isClearingCache by viewModel.isClearingCache.collectAsState()
    val isClearingVersions by viewModel.isClearingVersions.collectAsState()
    val isRestoringWorkspace by viewModel.isRestoringWorkspace.collectAsState()
    val isDefaultPath = viewModel.isDefaultWorkspace()
    val context = LocalContext.current
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showClearVersionsConfirm by remember { mutableStateOf(false) }
    var showRestoreWorkspaceConfirm by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showClearDataConfirmStep1 by remember { mutableStateOf(false) }
    var showClearDataConfirmStep2 by remember { mutableStateOf(false) }

    // 监听清除版本快照结果
    LaunchedEffect(Unit) {
        viewModel.clearVersionEvent.collect { event ->
            when (event) {
                is SettingsViewModel.ClearVersionEvent.Success -> {
                    Toast.makeText(context, context.resources.getQuantityString(
                        R.plurals.snapshots_deleted, event.count, event.count
                    ), Toast.LENGTH_SHORT).show()
                }
                is SettingsViewModel.ClearVersionEvent.Error -> {
                    Toast.makeText(context, context.getString(R.string.snapshot_delete_failed), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 监听清除缓存结果
    LaunchedEffect(Unit) {
        viewModel.clearCacheEvent.collect { event ->
            when (event) {
                is SettingsViewModel.ClearCacheEvent.Success -> {
                    Toast.makeText(context, context.getString(R.string.cache_cleared_freed, event.freedSize), Toast.LENGTH_SHORT).show()
                }
                is SettingsViewModel.ClearCacheEvent.Error -> {
                    Toast.makeText(context, context.getString(R.string.cache_clear_failed, event.message), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 监听恢复工作区结果
    LaunchedEffect(Unit) {
        viewModel.workspaceEvent.collect { event ->
            when (event) {
                is SettingsViewModel.WorkspaceEvent.Restored -> {
                    Toast.makeText(context, context.getString(R.string.workspace_restored), Toast.LENGTH_SHORT).show()
                }
                is SettingsViewModel.WorkspaceEvent.Error -> {
                    Toast.makeText(context, context.getString(R.string.workspace_restore_failed, event.message), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 监听重置设置事件
    LaunchedEffect(Unit) {
        viewModel.resetSettingsEvent.collect { event ->
            when (event) {
                is SettingsViewModel.ResetSettingsEvent.Success -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.reset_settings_success),
                        Toast.LENGTH_LONG,
                    ).show()
                }
                is SettingsViewModel.ResetSettingsEvent.Error -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.reset_settings_failed, event.message),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    // 监听清除数据事件
    LaunchedEffect(Unit) {
        viewModel.clearAllDataEvent.collect { event ->
            when (event) {
                is SettingsViewModel.ClearAllDataEvent.Success -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.clear_data_success),
                        Toast.LENGTH_LONG,
                    ).show()
                    // 强制重启应用
                    (context as? androidx.activity.ComponentActivity)?.let { activity ->
                        val packageManager = activity.packageManager
                        val intent = packageManager.getLaunchIntentForPackage(activity.packageName)
                        val componentName = intent?.component
                        val mainIntent = android.content.Intent.makeRestartActivityTask(componentName)
                        activity.startActivity(mainIntent)
                        activity.finish()
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }
                }
                is SettingsViewModel.ClearAllDataEvent.Error -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.clear_data_failed, event.message),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    val pickDirLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (_: Exception) {}
            viewModel.setWorkspacePath(it.toString())
            Toast.makeText(context, context.getString(R.string.workspace_path_changed), Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        SettingsTopBar(title = stringResource(R.string.storage_settings), onNavigateBack = onNavigateBack)

        Spacer(modifier = Modifier.height(12.dp))

        // ── 工作区板块 ──
        SettingsGroupTitle(stringResource(R.string.workspace))

        SettingsItemCard(onClick = { pickDirLauncher.launch(null) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = MdcitoCardDefaults.CompactContentPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = MdcitoCardDefaults.iconTint(),
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.workspace_path), style = MaterialTheme.typography.bodyLarge)
                    val internalStorageLabel = stringResource(R.string.internal_storage_label)
                    val displayPath = workspacePath?.let { path ->
                        if (path.startsWith("content://")) {
                            try {
                                val uri = Uri.parse(path)
                                uri.lastPathSegment?.replace("primary:", internalStorageLabel) ?: path
                            } catch (_: Exception) {
                                path
                            }
                        } else {
                            // 文件路径：简化显示
                            path.replace("/storage/emulated/0/", internalStorageLabel)
                        }
                    } ?: stringResource(R.string.workspace_path_default)
                    Text(
                        text = displayPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                TextButton(onClick = { pickDirLauncher.launch(null) }) {
                    Text(stringResource(R.string.change))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 恢复默认路径按钮
        SettingsItemCard(
            onClick = if (!isDefaultPath && !isRestoringWorkspace) {
                { showRestoreWorkspaceConfirm = true }
            } else null,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = MdcitoCardDefaults.CompactContentPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = null,
                    tint = if (!isDefaultPath) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.restore_default_path),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (!isDefaultPath) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                    Text(
                        text = stringResource(R.string.restore_default_path_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isRestoringWorkspace) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── 缓存板块 ──
        SettingsGroupTitle(stringResource(R.string.cache_group))

        SettingsActionItem(
            icon = Icons.Outlined.Delete,
            title = stringResource(R.string.clear_cache),
            subtitle = if (cacheSize.isNotEmpty()) {
                stringResource(R.string.cache_size_info, cacheSize)
            } else {
                stringResource(R.string.clear_cache_desc)
            },
            onClick = {
                if (!isClearingCache) {
                    showClearCacheConfirm = true
                }
            },
            trailing = {
                if (isClearingCache) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                }
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsActionItem(
            icon = Icons.Outlined.Delete,
            title = stringResource(R.string.clear_version_history),
            subtitle = stringResource(R.string.snapshot_count_info, snapshotCount),
            onClick = {
                if (!isClearingVersions) {
                    showClearVersionsConfirm = true
                }
            },
            trailing = {
                if (isClearingVersions) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                }
            },
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── 重置板块 ──
        SettingsGroupTitle(stringResource(R.string.reset_group))

        SettingsTextActionItem(
            icon = Icons.Outlined.RestartAlt,
            title = stringResource(R.string.reset_all_settings),
            subtitle = stringResource(R.string.reset_all_settings_desc),
            buttonText = stringResource(R.string.reset),
            onButtonClick = { showResetConfirm = true },
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── 危险区域板块 ──
        SettingsGroupTitle(stringResource(R.string.danger_zone))

        SettingsTextActionItem(
            icon = Icons.Outlined.DeleteForever,
            title = stringResource(R.string.clear_all_data),
            subtitle = stringResource(R.string.clear_all_data_desc),
            buttonText = stringResource(R.string.clear),
            buttonColor = MaterialTheme.colorScheme.error,
            titleColor = MaterialTheme.colorScheme.error,
            onButtonClick = { showClearDataConfirmStep1 = true },
        )

        Spacer(modifier = Modifier.height(40.dp))
    }

    // 确认对话框：清除缓存
    MdcitoConfirmDialog(
        visible = showClearCacheConfirm,
        title = stringResource(R.string.confirm_clear_cache_title),
        message = stringResource(R.string.confirm_clear_cache_msg),
        confirmText = stringResource(R.string.clear),
        isDangerous = true,
        onConfirm = {
            viewModel.clearCache()
            showClearCacheConfirm = false
        },
        onDismiss = { showClearCacheConfirm = false },
    )

    // 确认对话框：清除版本快照
    MdcitoConfirmDialog(
        visible = showClearVersionsConfirm,
        title = stringResource(R.string.confirm_clear_history_title),
        message = stringResource(R.string.confirm_clear_history_msg),
        confirmText = stringResource(R.string.clear),
        isDangerous = true,
        onConfirm = {
            viewModel.clearVersionHistory()
            showClearVersionsConfirm = false
        },
        onDismiss = { showClearVersionsConfirm = false },
    )

    // 确认对话框：恢复默认工作区路径
    MdcitoConfirmDialog(
        visible = showRestoreWorkspaceConfirm,
        title = stringResource(R.string.restore_default_path),
        message = stringResource(R.string.confirm_restore_workspace_msg),
        confirmText = stringResource(R.string.restore),
        isDangerous = false,
        onConfirm = {
            viewModel.restoreWorkspace()
            showRestoreWorkspaceConfirm = false
        },
        onDismiss = { showRestoreWorkspaceConfirm = false },
    )

    // 确认对话框：重置所有设置
    MdcitoConfirmDialog(
        visible = showResetConfirm,
        title = stringResource(R.string.confirm_reset_title),
        message = stringResource(R.string.confirm_reset_msg),
        confirmText = stringResource(R.string.reset),
        isDangerous = true,
        onConfirm = {
            showResetConfirm = false
            viewModel.resetAllSettings()
        },
        onDismiss = { showResetConfirm = false },
    )

    // 确认对话框：清除所有数据 - 双重确认流程：第一步警告
    MdcitoConfirmDialog(
        visible = showClearDataConfirmStep1,
        title = stringResource(R.string.confirm_clear_data_title),
        message = stringResource(R.string.confirm_clear_data_msg_step1),
        confirmText = stringResource(R.string.continue_label),
        isDangerous = true,
        onConfirm = {
            showClearDataConfirmStep1 = false
            showClearDataConfirmStep2 = true
        },
        onDismiss = { showClearDataConfirmStep1 = false },
    )

    // 确认对话框：清除所有数据 - 双重确认流程：第二步最终确认
    MdcitoConfirmDialog(
        visible = showClearDataConfirmStep2,
        title = stringResource(R.string.confirm_clear_data_title),
        message = stringResource(R.string.confirm_clear_data_msg_step2),
        confirmText = stringResource(R.string.clear),
        isDangerous = true,
        onConfirm = {
            showClearDataConfirmStep2 = false
            viewModel.clearAllData()
        },
        onDismiss = { showClearDataConfirmStep2 = false },
    )
}
