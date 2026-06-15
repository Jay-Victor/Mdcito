package com.mdcito.app.ui.editor

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.mdcito.app.ui.components.LocalCardGlassIntensity
import com.mdcito.app.ui.components.LocalCardStyle
import com.mdcito.app.ui.components.LocalCardTransparency
import com.mdcito.app.ui.components.MdcitoCard
import com.mdcito.app.ui.settings.SettingsTopBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mdcito.app.data.db.entity.VersionEntity
import com.mdcito.app.ui.components.MdcitoConfirmDialog
import androidx.compose.ui.res.stringResource
import com.mdcito.app.R

@Composable
fun VersionHistoryScreen(
    fileId: Long,
    fileName: String,
    onNavigateBack: () -> Unit,
    onRestoreVersion: (String) -> Unit,
    viewModel: VersionHistoryViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val versions by viewModel.getVersionsForFile(fileId).collectAsState(initial = emptyList())
    var showRestoreConfirm by remember { mutableStateOf<VersionEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<VersionEntity?>(null) }

    // 批量删除状态
    var isSelectMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }

    // 删除结果反馈
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.deleteEvent.collect { event ->
            when (event) {
                is VersionHistoryViewModel.DeleteEvent.Success -> {
                    Toast.makeText(
                        context,
                        context.resources.getQuantityString(
                            R.plurals.snapshots_deleted, event.count, event.count
                        ),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                is VersionHistoryViewModel.DeleteEvent.Error -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.snapshot_delete_failed),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        // 顶部标题栏：标题 + 删除图标
        SettingsTopBar(
            title = if (isSelectMode) stringResource(R.string.snapshots_selected_count, selectedIds.size)
            else stringResource(R.string.version_snapshot),
            onNavigateBack = if (isSelectMode) {
                { isSelectMode = false; selectedIds = emptySet() }
            } else onNavigateBack,
            backIcon = if (isSelectMode) Icons.Filled.Close else null,
            trailingContent = {
                if (!isSelectMode && versions.isNotEmpty()) {
                    IconButton(onClick = { isSelectMode = true }) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = stringResource(R.string.batch_delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
        )

        // 选择模式下的操作栏
        AnimatedVisibility(
            visible = isSelectMode,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = {
                        selectedIds = if (selectedIds.size == versions.size) emptySet()
                        else versions.map { it.id }.toSet()
                    },
                ) {
                    Text(
                        text = if (selectedIds.size == versions.size) stringResource(R.string.deselect_all)
                        else stringResource(R.string.select_all),
                    )
                }
                TextButton(
                    onClick = { showDeleteAllConfirm = true },
                ) {
                    Text(
                        text = stringResource(R.string.delete_all),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { showBatchDeleteConfirm = true },
                    enabled = selectedIds.isNotEmpty(),
                ) {
                    Text(
                        text = stringResource(R.string.delete_selected),
                        color = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.38f),
                    )
                }
            }
        }

        Text(
            text = fileName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
        )

        if (versions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.no_version_history),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            versions.forEach { version ->
                VersionCard(
                    version = version,
                    isSelectMode = isSelectMode,
                    isSelected = selectedIds.contains(version.id),
                    onSelect = {
                        selectedIds = if (selectedIds.contains(version.id)) {
                            selectedIds - version.id
                        } else {
                            selectedIds + version.id
                        }
                    },
                    onRestore = { showRestoreConfirm = version },
                    onDelete = { showDeleteConfirm = version },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    // 恢复确认
    showRestoreConfirm?.let { version ->
        MdcitoConfirmDialog(
            visible = true,
            title = stringResource(R.string.confirm_restore_title),
            message = stringResource(R.string.confirm_restore_msg, formatVersionTime(version.createdAt)),
            confirmText = stringResource(R.string.restore),
            onConfirm = {
                onRestoreVersion(version.content)
                showRestoreConfirm = null
            },
            onDismiss = { showRestoreConfirm = null },
        )
    }

    // 单个删除确认
    showDeleteConfirm?.let { version ->
        MdcitoConfirmDialog(
            visible = true,
            title = stringResource(R.string.confirm_delete_version_title),
            message = stringResource(
                R.string.confirm_delete_snapshot_msg,
                formatVersionTime(version.createdAt)
            ),
            confirmText = stringResource(R.string.delete),
            isDangerous = true,
            onConfirm = {
                viewModel.deleteVersion(version)
                showDeleteConfirm = null
            },
            onDismiss = { showDeleteConfirm = null },
        )
    }

    // 批量删除确认
    if (showBatchDeleteConfirm && selectedIds.isNotEmpty()) {
        MdcitoConfirmDialog(
            visible = true,
            title = stringResource(R.string.batch_delete),
            message = stringResource(R.string.confirm_batch_delete_snapshot, selectedIds.size),
            confirmText = stringResource(R.string.delete),
            isDangerous = true,
            onConfirm = {
                viewModel.deleteVersionsByIds(selectedIds.toList())
                selectedIds = emptySet()
                isSelectMode = false
                showBatchDeleteConfirm = false
            },
            onDismiss = { showBatchDeleteConfirm = false },
        )
    }

    // 全部删除确认
    if (showDeleteAllConfirm) {
        MdcitoConfirmDialog(
            visible = true,
            title = stringResource(R.string.delete_all),
            message = stringResource(R.string.confirm_delete_all_snapshots),
            confirmText = stringResource(R.string.delete),
            isDangerous = true,
            onConfirm = {
                viewModel.deleteAllVersionsForFile(fileId)
                selectedIds = emptySet()
                isSelectMode = false
                showDeleteAllConfirm = false
            },
            onDismiss = { showDeleteAllConfirm = false },
        )
    }
}

@Composable
private fun VersionCard(
    version: VersionEntity,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    MdcitoCard(
        cardStyle = LocalCardStyle.current,
        glassIntensity = LocalCardGlassIntensity.current,
        transparency = LocalCardTransparency.current,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelectMode) Modifier.clickable { onSelect() } else Modifier
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 选择模式下显示复选框
            if (isSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelect() },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatVersionTime(version.createdAt),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.W600,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row {
                    Text(
                        text = formatFileSize(version.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (version.summary.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = version.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // 非选择模式下显示操作按钮
            if (!isSelectMode) {
                IconButton(onClick = onRestore, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Restore,
                        contentDescription = stringResource(R.string.restore),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

private fun formatVersionTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}
