package com.mdcito.app.ui.settings.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mdcito.app.R
import com.mdcito.app.data.update.DownloadState
import com.mdcito.app.data.update.DualCheckResult
import com.mdcito.app.data.update.UpdateInfo
import com.mdcito.app.data.update.UpdateSource
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialog(
    checkState: UpdateViewModel.CheckState,
    downloadState: DownloadState,
    updateSource: String,
    onCheckUpdate: (UpdateSource?) -> Unit = {},
    onStartDownload: (String?) -> Unit = {},
    onPauseDownload: () -> Unit = {},
    onResumeDownload: () -> Unit = {},
    onCancelDownload: () -> Unit = {},
    onInstallApk: (String) -> Unit = {},
    onOpenInstallPermission: () -> Unit = {},
    onDismiss: () -> Unit = {},
    onSetUpdateSource: (String) -> Unit = {},
    onResetCheckState: () -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题 + 关闭按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.update_check_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.W700,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 使用 if-else 而非 when 逗号分支，确保 Compose slot table 稳定，
            // 避免 Idle→Checking 切换时 CircularProgressIndicator 被销毁重建导致动画卡死
            if (checkState is UpdateViewModel.CheckState.Idle ||
                checkState is UpdateViewModel.CheckState.Checking
            ) {
                CheckingContent()
            } else when (checkState) {
                is UpdateViewModel.CheckState.Available -> {
                    UpdateAvailableContent(
                        result = checkState.result,
                        downloadState = downloadState,
                        updateSource = updateSource,
                        onStartDownload = onStartDownload,
                        onPauseDownload = onPauseDownload,
                        onResumeDownload = onResumeDownload,
                        onCancelDownload = onCancelDownload,
                        onInstallApk = onInstallApk,
                        onOpenInstallPermission = onOpenInstallPermission,
                        onSetUpdateSource = onSetUpdateSource,
                        onCheckUpdate = onCheckUpdate,
                        uriHandler = uriHandler,
                    )
                }

                is UpdateViewModel.CheckState.UpToDate -> {
                    UpToDateContent()
                }

                is UpdateViewModel.CheckState.Error -> {
                    ErrorContent(
                        message = checkState.message,
                        onRetry = { onCheckUpdate(null) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CheckingContent() {
    Spacer(modifier = Modifier.height(16.dp))
    CircularProgressIndicator(modifier = Modifier.size(48.dp))
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.update_checking),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(16.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateAvailableContent(
    result: DualCheckResult,
    downloadState: DownloadState,
    updateSource: String,
    onStartDownload: (String?) -> Unit,
    onPauseDownload: () -> Unit,
    onResumeDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onInstallApk: (String) -> Unit,
    onOpenInstallPermission: () -> Unit,
    onSetUpdateSource: (String) -> Unit,
    onCheckUpdate: (UpdateSource?) -> Unit,
    uriHandler: androidx.compose.ui.platform.UriHandler,
) {
    // 选择当前展示的更新信息（优先 Gitee，其次 GitHub）
    var selectedPlatform by remember { mutableStateOf(UpdateSource.GITEE) }
    val updateInfo: UpdateInfo? = when (selectedPlatform) {
        UpdateSource.GITEE -> result.gitee
        UpdateSource.GITHUB -> result.github
    }

    // 版本信息 + 双平台标签
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Outlined.Update,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = stringResource(R.string.update_new_version_available),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W700,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = if (result.gitee != null && result.github != null) {
                    "Gitee v${result.gitee.versionName} / GitHub v${result.github.versionName}"
                } else {
                    "v${result.gitee?.versionName ?: result.github?.versionName ?: "?"}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 双平台选择 Chip（仅当两边都有结果时显示）
    if (result.gitee != null && result.github != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedPlatform == UpdateSource.GITEE,
                onClick = { selectedPlatform = UpdateSource.GITEE },
                label = {
                    Text(
                        "Gitee (v${result.gitee.versionName})",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )
            FilterChip(
                selected = selectedPlatform == UpdateSource.GITHUB,
                onClick = { selectedPlatform = UpdateSource.GITHUB },
                label = {
                    Text(
                        "GitHub (v${result.github.versionName})",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    // 来源标签 + 大小 + 时间
    if (updateInfo != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = updateInfo.source.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (updateInfo.downloadSize > 0) {
                Text(
                    text = formatFileSize(updateInfo.downloadSize),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (updateInfo.publishedAt.isNotBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = updateInfo.publishedAt.substringBefore('T'),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 更新日志
        if (updateInfo.releaseNotes.isNotBlank()) {
            var expanded by remember { mutableStateOf(false) }
            val notes = updateInfo.releaseNotes
            val preview = notes.take(200)

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.update_release_notes),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.W600,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (expanded || notes.length <= 200) notes else "$preview...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (notes.length > 200) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = { expanded = !expanded },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                        ) {
                            Text(
                                text = if (expanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 下载区域
        DownloadSection(
            updateInfo = updateInfo,
            downloadState = downloadState,
            onStartDownload = onStartDownload,
            onPauseDownload = onPauseDownload,
            onResumeDownload = onResumeDownload,
            onCancelDownload = onCancelDownload,
            onInstallApk = onInstallApk,
            uriHandler = uriHandler,
        )
    } else {
        // 选中的平台没有结果，提示切换
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.update_platform_no_result),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }

    // 重新检查按钮（切换到单个源检查）
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(
        onClick = {
            onSetUpdateSource(if (selectedPlatform == UpdateSource.GITEE) "GITEE" else "GITHUB")
            onCheckUpdate(selectedPlatform)
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = "以 ${selectedPlatform.displayName} 重新检查",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun DownloadSection(
    updateInfo: UpdateInfo,
    downloadState: DownloadState,
    onStartDownload: (String?) -> Unit,
    onPauseDownload: () -> Unit,
    onResumeDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onInstallApk: (String) -> Unit,
    uriHandler: androidx.compose.ui.platform.UriHandler,
) {
    when (downloadState) {
        is DownloadState.Idle -> {
            Button(
                onClick = { onStartDownload(null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.FileDownload, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.update_download))
            }

            if (updateInfo.mirrorUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.update_mirror_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    updateInfo.mirrorUrls.forEach { mirror ->
                        OutlinedButton(
                            onClick = { onStartDownload(mirror.url) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(mirror.name, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    try { uriHandler.openUri(updateInfo.source.repoUrl + "/releases") } catch (_: Exception) {}
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = if (updateInfo.source == UpdateSource.GITHUB)
                        stringResource(R.string.update_visit_github)
                    else stringResource(R.string.update_visit_gitee),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        is DownloadState.Downloading -> DownloadProgressContent(
            progress = downloadState.progress,
            downloadedBytes = downloadState.downloadedBytes,
            totalBytes = downloadState.totalBytes,
            speedBytesPerSec = downloadState.speedBytesPerSec,
            remainingTimeSecs = downloadState.remainingTimeSecs,
            isPaused = false,
            onPause = onPauseDownload,
            onCancel = onCancelDownload,
        )

        is DownloadState.Paused -> DownloadProgressContent(
            progress = downloadState.progress,
            downloadedBytes = downloadState.downloadedBytes,
            totalBytes = downloadState.totalBytes,
            speedBytesPerSec = 0L,
            remainingTimeSecs = 0L,
            isPaused = true,
            onResume = onResumeDownload,
            onCancel = onCancelDownload,
        )

        is DownloadState.Completed -> {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.update_download_complete), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { onInstallApk(downloadState.filePath) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.update_install_now)) }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.update_install_later)) }
        }

        is DownloadState.Error -> {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(downloadState.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { onStartDownload(null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.update_retry_download)) }
        }
    }
}

@Composable
private fun DownloadProgressContent(
    progress: Float,
    downloadedBytes: Long,
    totalBytes: Long,
    speedBytesPerSec: Long,
    remainingTimeSecs: Long,
    isPaused: Boolean,
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "${(progress * 100).toInt()}%",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.W700,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "${formatFileSize(downloadedBytes)}/${formatFileSize(totalBytes)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (speedBytesPerSec > 0) {
            Text(
                "${formatFileSize(speedBytesPerSec)}/s",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (remainingTimeSecs > 0 && speedBytesPerSec > 0) {
        Text(
            formatRemainingTime(remainingTimeSecs),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = if (isPaused) onResume else onPause,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
        ) {
            Icon(
                if (isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                if (isPaused) stringResource(R.string.update_resume) else stringResource(R.string.update_pause),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text(stringResource(R.string.cancel), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun UpToDateContent() {
    Spacer(modifier = Modifier.height(8.dp))
    Icon(
        Icons.Outlined.CheckCircle, null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(48.dp)
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        stringResource(R.string.update_already_latest),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.W600,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        stringResource(R.string.update_already_latest_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Spacer(modifier = Modifier.height(8.dp))
    Icon(
        Icons.Outlined.ErrorOutline, null,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(48.dp)
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        stringResource(R.string.update_check_error),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.W600,
        color = MaterialTheme.colorScheme.error,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = onRetry,
        shape = RoundedCornerShape(12.dp),
    ) { Text(stringResource(R.string.update_retry)) }
}

// ── 工具函数 ──
private val fileSizeFormat = DecimalFormat("#.##")

private fun formatFileSize(bytes: Long): String = when {
    bytes <= 0 -> "0 B"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${fileSizeFormat.format(bytes.toDouble() / 1024)} KB"
    bytes < 1024 * 1024 * 1024 -> "${fileSizeFormat.format(bytes.toDouble() / (1024 * 1024))} MB"
    else -> "${fileSizeFormat.format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
}

private fun formatRemainingTime(seconds: Long): String = when {
    seconds < 60 -> "约 ${seconds}s"
    seconds < 3600 -> "约 ${seconds / 60}m${seconds % 60}s"
    else -> "约 ${seconds / 3600}h${(seconds % 3600) / 60}m"
}
