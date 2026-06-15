package com.mdcito.app.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mdcito.app.R
import com.mdcito.app.data.locale.LanguageHelper
import com.mdcito.app.data.log.LogEntry
import com.mdcito.app.data.log.LogLevel
import com.mdcito.app.data.log.colorDark
import com.mdcito.app.data.log.colorLight
import com.mdcito.app.ui.components.MdcitoCard
import com.mdcito.app.ui.components.MdcitoCardDefaults
import com.mdcito.app.ui.theme.LocalIsDarkTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: LogSettingsViewModel = hiltViewModel(),
) {
    val debugMode by viewModel.debugMode.collectAsState()
    val logCount by viewModel.logCount.collectAsState()
    val expandedLevels by viewModel.expandedLevels.collectAsState()
    val levelLogs by viewModel.levelLogs.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()
    val showExportDialog by viewModel.showExportDialog.collectAsState()
    val selectedExportLevels by viewModel.selectedExportLevels.collectAsState()
    val showClearConfirm by viewModel.showClearConfirmDialog.collectAsState()
    val showClearAllConfirm by viewModel.showClearAllConfirmDialog.collectAsState()
    val context = LocalContext.current

    LanguageHelper.LocalLocaleVersion.current

    LaunchedEffect(Unit) {
        viewModel.logEvent.collect { event ->
            when (event) {
                is LogSettingsViewModel.LogEvent.CopySuccess -> {
                    Toast.makeText(context, context.getString(R.string.log_copy_success, event.count), Toast.LENGTH_SHORT).show()
                }
                is LogSettingsViewModel.LogEvent.CopyFailed -> {
                    Toast.makeText(context, context.getString(R.string.log_copy_failed, event.message), Toast.LENGTH_SHORT).show()
                }
                is LogSettingsViewModel.LogEvent.ClearSuccess -> {
                    Toast.makeText(context, context.getString(R.string.log_clear_success, context.getString(event.level.labelResId)), Toast.LENGTH_SHORT).show()
                }
                is LogSettingsViewModel.LogEvent.ClearAllSuccess -> {
                    Toast.makeText(context, context.getString(R.string.log_clear_all_success, event.count), Toast.LENGTH_SHORT).show()
                }
                is LogSettingsViewModel.LogEvent.ExportSuccess -> {
                    Toast.makeText(context, context.getString(R.string.log_export_success, event.filePath), Toast.LENGTH_LONG).show()
                }
                is LogSettingsViewModel.LogEvent.ExportFailed -> {
                    Toast.makeText(context, context.getString(R.string.log_export_failed, event.message), Toast.LENGTH_SHORT).show()
                }
                is LogSettingsViewModel.LogEvent.ExportProgress -> {
                    // 进度由 exportProgress StateFlow 驱动
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
        SettingsTopBar(
            title = stringResource(R.string.log_settings),
            onNavigateBack = onNavigateBack,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── 调试选项 ──
        SettingsGroupTitle(stringResource(R.string.debug_options))

        SwitchSetting(
            title = stringResource(R.string.debug_mode),
            subtitle = stringResource(R.string.debug_mode_desc),
            checked = debugMode,
        ) {
            viewModel.setDebugMode(it)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── 日志级别卡片 ──
        SettingsGroupTitle(stringResource(R.string.log_level_cards))

        for (level in LogLevel.entries) {
            val isExpanded = level in expandedLevels
            val count = logCount[level] ?: 0
            val logs = levelLogs[level] ?: emptyList()

            LogLevelCard(
                level = level,
                logCount = count,
                isExpanded = isExpanded,
                debugMode = debugMode,
                onToggleExpand = { viewModel.toggleLevelExpanded(level) },
                onCopy = { viewModel.copyLogs(level) },
                onClear = { viewModel.showClearConfirm(level) },
                logs = logs,
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── 日志操作（仅调试模式开启时显示） ──
        if (debugMode) {
            SettingsGroupTitle(stringResource(R.string.log_operations))

            // 导出日志
            SettingsItemCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MdcitoCardDefaults.ContentPadding,
                            vertical = MdcitoCardDefaults.CompactContentPadding,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FileDownload,
                        contentDescription = null,
                        tint = MdcitoCardDefaults.glassIconTint(),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.export_log),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.export_log_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MdcitoCardDefaults.glassSubtleContentColor(),
                        )
                        Text(
                            text = stringResource(R.string.export_log_path_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MdcitoCardDefaults.glassSubtleContentColor().copy(alpha = 0.7f),
                        )
                    }
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        TextButton(onClick = { viewModel.showExportDialog() }) {
                            Text(stringResource(R.string.export_label))
                        }
                    }
                }
            }

            if (isExporting) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { exportProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 生成测试日志
            SettingsItemCard(onClick = { viewModel.generateTestLogs() }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MdcitoCardDefaults.ContentPadding,
                            vertical = MdcitoCardDefaults.CompactContentPadding,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Science,
                        contentDescription = null,
                        tint = MdcitoCardDefaults.glassIconTint(),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.generate_test_logs),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.generate_test_logs_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MdcitoCardDefaults.glassSubtleContentColor(),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 一键清除所有日志
            SettingsItemCard(onClick = { viewModel.showClearAllConfirm() }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MdcitoCardDefaults.ContentPadding,
                            vertical = MdcitoCardDefaults.CompactContentPadding,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.clear_all_logs),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        )
                        Text(
                            text = stringResource(R.string.clear_all_logs_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MdcitoCardDefaults.glassSubtleContentColor(),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    // ── 清除确认弹窗 ──
    if (showClearConfirm != null) {
        val clearLevel = showClearConfirm!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissClearConfirm() },
            title = { Text(stringResource(R.string.confirm_clear_log_title)) },
            text = {
                Text(stringResource(R.string.confirm_clear_log_msg, stringResource(clearLevel.labelResId)))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearLogs(clearLevel)
                    viewModel.dismissClearConfirm()
                }) {
                    Text(
                        stringResource(R.string.confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClearConfirm() }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // ── 一键清除所有日志确认弹窗 ──
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClearAllConfirm() },
            title = { Text(stringResource(R.string.confirm_clear_all_log_title)) },
            text = {
                Text(stringResource(R.string.confirm_clear_all_log_msg))
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAllLogs() }) {
                    Text(
                        stringResource(R.string.confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClearAllConfirm() }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // ── 导出选项弹窗 ──
    if (showExportDialog) {
        ExportLogDialog(
            selectedLevels = selectedExportLevels,
            onToggleLevel = { viewModel.toggleExportLevel(it) },
            onSelectAll = { viewModel.selectAllExportLevels() },
            onExport = { viewModel.exportLogs() },
            onDismiss = { viewModel.dismissExportDialog() },
        )
    }
}

@Composable
private fun LogLevelCard(
    level: LogLevel,
    logCount: Int,
    isExpanded: Boolean,
    debugMode: Boolean,
    onToggleExpand: () -> Unit,
    onCopy: () -> Unit,
    onClear: () -> Unit,
    logs: List<LogEntry>,
) {
    val isDark = LocalIsDarkTheme.current
    val levelColor = if (isDark) level.colorDark else level.colorLight

    MdcitoCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            // ── 卡片头部：级别标签 + 说明 + 计数 + 操作按钮 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MdcitoCardDefaults.ContentPadding,
                        vertical = 10.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 级别 Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = levelColor.copy(alpha = if (debugMode) 0.15f else 0.08f),
                ) {
                    Text(
                        text = stringResource(level.labelResId),
                        color = if (debugMode) levelColor else levelColor.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 级别说明
                Text(
                    text = stringResource(level.descResId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MdcitoCardDefaults.glassSubtleContentColor(),
                    modifier = Modifier.weight(1f),
                )

                // 调试模式开启时才显示日志条数和操作按钮
                if (debugMode) {
                    // 日志条数
                    Text(
                        text = stringResource(R.string.log_count_format, logCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MdcitoCardDefaults.glassSubtleContentColor(),
                    )

                    // 复制按钮
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.copy_logs),
                            tint = MdcitoCardDefaults.glassIconTint(),
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    // 清除按钮
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(R.string.clear_logs),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    // 展开/收起箭头
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp
                            else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (isExpanded) stringResource(R.string.collapse)
                            else stringResource(R.string.expand),
                            tint = MdcitoCardDefaults.glassFaintContentColor(),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // ── 日志展示区域（仅调试模式开启时可见） ──
            AnimatedVisibility(
                visible = debugMode && isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                LogDisplayArea(
                    logs = logs,
                    levelColor = levelColor,
                )
            }
        }
    }
}

@Composable
private fun LogDisplayArea(
    logs: List<LogEntry>,
    levelColor: Color,
) {
    if (logs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.no_logs_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MdcitoCardDefaults.glassSubtleContentColor(),
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MdcitoCardDefaults.ContentPadding)
            .padding(bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // 分隔线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MdcitoCardDefaults.glassSubtleContentColor().copy(alpha = 0.2f))
                .padding(bottom = 4.dp),
        )

        // 日志条目列表（限制显示高度，内部可滚动）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                .border(
                    BorderStroke(1.dp, levelColor.copy(alpha = 0.15f)),
                    RoundedCornerShape(6.dp),
                )
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (entry in logs) {
                    LogEntryItem(entry = entry, levelColor = levelColor)
                }
            }
        }
    }
}

@Composable
private fun LogEntryItem(
    entry: LogEntry,
    levelColor: Color,
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
    val time = timeFormat.format(Date(entry.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        // 左侧色条
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(levelColor)
                .align(Alignment.CenterVertically),
        )
        Spacer(modifier = Modifier.width(6.dp))
        // 时间
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            ),
            color = MdcitoCardDefaults.glassSubtleContentColor(),
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        Spacer(modifier = Modifier.width(6.dp))
        // 标签
        Text(
            text = entry.tag,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            ),
            color = levelColor,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .widthIn(max = 80.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        // 消息
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            ),
            color = MdcitoCardDefaults.glassContentColor(),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ExportLogDialog(
    selectedLevels: Set<LogLevel>,
    onToggleLevel: (LogLevel) -> Unit,
    onSelectAll: () -> Unit,
    onExport: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.export_log), fontWeight = FontWeight.W600) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.export_level_select_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 全选按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.select_all_levels),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.W500,
                    )
                    TextButton(onClick = onSelectAll) {
                        Text(stringResource(R.string.select_all_export_levels))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 级别选择
                for (level in LogLevel.entries) {
                    val isDark = LocalIsDarkTheme.current
                    val levelColor = if (isDark) level.colorDark else level.colorLight
                    val isSelected = level in selectedLevels

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleLevel(level) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleLevel(level) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = levelColor,
                            ),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = levelColor.copy(alpha = 0.15f),
                        ) {
                            Text(
                                text = stringResource(level.labelResId),
                                color = levelColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.export_format_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onExport,
                enabled = selectedLevels.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(stringResource(R.string.export_label))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
