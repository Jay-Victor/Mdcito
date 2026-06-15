package com.mdcito.app.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mdcito.app.BuildConfig
import com.mdcito.app.R
import com.mdcito.app.ui.components.MdcitoCardDefaults
import com.mdcito.app.data.locale.LanguageHelper
import com.mdcito.app.ui.settings.update.UpdateDialog
import com.mdcito.app.ui.settings.update.UpdateViewModel

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOpenSource: () -> Unit = {},
    onNavigateToChangelog: () -> Unit = {},
    updateViewModel: UpdateViewModel = hiltViewModel(),
) {
    LanguageHelper.LocalLocaleVersion.current
    val uriHandler = LocalUriHandler.current
    var showUpdateDialog by remember { mutableStateOf(false) }

    val checkState by updateViewModel.checkState.collectAsStateWithLifecycle()
    val downloadState by updateViewModel.downloadState.collectAsStateWithLifecycle()
    val autoCheckUpdate by updateViewModel.autoCheckUpdate.collectAsStateWithLifecycle()
    val updateSource by updateViewModel.updateSource.collectAsStateWithLifecycle()

    // 启动时静默自动检查
    LaunchedEffect(Unit) {
        updateViewModel.autoCheckIfEnabled()
    }

    // 监听安装权限事件
    LaunchedEffect(Unit) {
        updateViewModel.updateEvent.collect { event ->
            if (event is UpdateViewModel.UpdateEvent.InstallPermissionRequired && !event.hasPermission) {
                updateViewModel.openInstallPermissionSettings()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        SettingsTopBar(title = stringResource(R.string.about), onNavigateBack = onNavigateBack)

        Spacer(modifier = Modifier.height(12.dp))

        // ── APP 信息头部 ──
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_square),
                contentDescription = "Mdcito",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp)),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Mdcito",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.W700,
            )
            Text(
                text = stringResource(R.string.markdown_editor),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.version_format,
                    BuildConfig.VERSION_NAME
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── 更新 ──
        SettingsGroupTitle(stringResource(R.string.update_section_title))

        SwitchSetting(
            title = stringResource(R.string.update_auto_check),
            subtitle = stringResource(R.string.update_auto_check_desc),
            checked = autoCheckUpdate,
            onCheckedChange = { updateViewModel.setAutoCheckUpdate(it) },
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsActionItem(
            icon = Icons.Outlined.SystemUpdate,
            title = stringResource(R.string.update_check_card),
            subtitle = when (checkState) {
                is UpdateViewModel.CheckState.Checking -> stringResource(R.string.update_checking)
                is UpdateViewModel.CheckState.Available -> {
                    val result = (checkState as UpdateViewModel.CheckState.Available).result
                    val ver = result.gitee?.versionName ?: result.github?.versionName ?: ""
                    stringResource(R.string.update_new_version_available) + " (v$ver)"
                }
                else -> stringResource(R.string.update_check_subtitle)
            },
            onClick = {
                showUpdateDialog = true
                updateViewModel.resetCheckState()
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsNavigationItem(
            icon = Icons.Outlined.Update,
            label = stringResource(R.string.changelog),
            subtitle = stringResource(R.string.changelog_desc),
            onClick = onNavigateToChangelog,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── 应用信息 ──
        SettingsGroupTitle(stringResource(R.string.app_info))

        SettingsInfoItem(label = stringResource(R.string.developer), value = "Jay-Victor")
        Spacer(modifier = Modifier.height(8.dp))
        SettingsInfoItem(
            label = stringResource(R.string.build),
            value = "${BuildConfig.VERSION_NAME}-release"
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsItemCard {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = stringResource(R.string.repo),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.W600,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            try { uriHandler.openUri("https://github.com/Jay-Victor/Mdcito") } catch (_: Exception) {}
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GitHub",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.W500,
                        )
                        Text(
                            text = "github.com/Jay-Victor/Mdcito",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 28.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            try { uriHandler.openUri("https://gitee.com/Jay-Victor/Mdcito") } catch (_: Exception) {}
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gitee",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.W500,
                        )
                        Text(
                            text = "gitee.com/Jay-Victor/Mdcito",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── 反馈与帮助 ──
        SettingsGroupTitle(stringResource(R.string.feedback_help))

        SettingsActionItem(
            icon = Icons.Outlined.Feedback,
            title = stringResource(R.string.feedback),
            subtitle = stringResource(R.string.feedback_desc),
            onClick = {
                try { uriHandler.openUri("https://github.com/Jay-Victor/Mdcito/issues") } catch (_: Exception) {}
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsActionItem(
            icon = Icons.Outlined.BugReport,
            title = stringResource(R.string.report_bug),
            subtitle = stringResource(R.string.report_bug_desc),
            onClick = {
                try { uriHandler.openUri("https://github.com/Jay-Victor/Mdcito/issues/new") } catch (_: Exception) {}
            },
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── 法律信息 ──
        SettingsGroupTitle(stringResource(R.string.legal))

        SettingsNavigationItem(
            icon = Icons.Outlined.Code,
            label = stringResource(R.string.open_source),
            subtitle = stringResource(R.string.open_source_desc),
            onClick = onNavigateToOpenSource,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── 版权 ──
        SettingsGroupTitle(stringResource(R.string.copyright))

        SettingsInfoItem(
            label = stringResource(R.string.open_source_license),
            value = "",
            onClick = {
                try { uriHandler.openUri("https://github.com/Jay-Victor/Mdcito") } catch (_: Exception) {}
            },
            trailing = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = stringResource(R.string.open_source_license),
                    tint = MdcitoCardDefaults.glassIconTint(),
                    modifier = Modifier.size(16.dp),
                )
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsInfoItem(label = stringResource(R.string.copyright_all), value = "© 2026 Jay-Victor")

        Spacer(modifier = Modifier.height(40.dp))
    }

    // ── 更新管理弹窗 ──
    if (showUpdateDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                showUpdateDialog = false
                if (checkState !is UpdateViewModel.CheckState.Available) {
                    updateViewModel.resetCheckState()
                }
            },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
            ),
        ) {
            // 弹窗打开后自动触发检查，确保先显示"检查中"状态
            LaunchedEffect(Unit) {
                updateViewModel.checkForUpdate(null)
            }
            UpdateDialog(
                checkState = checkState,
                downloadState = downloadState,
                updateSource = updateSource,
                onCheckUpdate = { source -> updateViewModel.checkForUpdate(source) },
                onStartDownload = { mirrorUrl -> updateViewModel.startDownload(mirrorUrl) },
                onPauseDownload = { updateViewModel.pauseDownload() },
                onResumeDownload = { updateViewModel.resumeDownload() },
                onCancelDownload = { updateViewModel.cancelDownload() },
                onInstallApk = { filePath -> updateViewModel.installApk(filePath) },
                onOpenInstallPermission = { updateViewModel.openInstallPermissionSettings() },
                onDismiss = {
                    showUpdateDialog = false
                    if (checkState !is UpdateViewModel.CheckState.Available) {
                        updateViewModel.resetCheckState()
                    }
                },
                onSetUpdateSource = { source -> updateViewModel.setUpdateSource(source) },
                onResetCheckState = {
                    updateViewModel.resetCheckState()
                    showUpdateDialog = false
                },
            )
        }
    }
}
