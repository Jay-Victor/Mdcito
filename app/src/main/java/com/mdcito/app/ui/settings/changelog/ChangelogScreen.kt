package com.mdcito.app.ui.settings.changelog

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mdcito.app.R
import com.mdcito.app.data.update.ChangelogEntry
import com.mdcito.app.ui.components.LocalCardGlassIntensity
import com.mdcito.app.ui.components.LocalCardStyle
import com.mdcito.app.ui.components.LocalCardTransparency
import com.mdcito.app.ui.components.MdcitoCard
import com.mdcito.app.ui.components.MdcitoCardDefaults
import com.mdcito.app.ui.settings.SettingsTopBar

private const val COLLAPSED_MAX_LINES = 3

@Composable
fun ChangelogScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChangelogViewModel = hiltViewModel(),
) {
    val changelogState by viewModel.changelogState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        SettingsTopBar(
            title = stringResource(R.string.changelog),
            onNavigateBack = onNavigateBack,
            trailingContent = {
                when (changelogState) {
                    is ChangelogState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    is ChangelogState.Error -> {
                        IconButton(onClick = { viewModel.fetchChangelog() }) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = stringResource(R.string.update_retry),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    is ChangelogState.Success -> {
                        IconButton(onClick = { viewModel.fetchChangelog() }) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = stringResource(R.string.update_retry),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (val state = changelogState) {
            is ChangelogState.Loading -> {
                LoadingContent()
            }
            is ChangelogState.Success -> {
                ChangelogList(entries = state.entries)
            }
            is ChangelogState.Error -> {
                ErrorContent(
                    onRetry = { viewModel.fetchChangelog() },
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.changelog_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorContent(
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.changelog_load_failed),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.update_retry))
            }
        }
    }
}

@Composable
private fun ChangelogList(entries: List<ChangelogEntry>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = entries,
            key = { entry -> entry.versionName },
        ) { entry ->
            VersionCard(
                entry = entry,
                isLatest = entry.isLatest,
            )
        }

        // 底部间距
        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun VersionCard(
    entry: ChangelogEntry,
    isLatest: Boolean,
) {
    val cardStyle = LocalCardStyle.current
    val glassIntensity = LocalCardGlassIntensity.current
    val transparency = LocalCardTransparency.current

    val cardModifier = Modifier.fillMaxWidth()

    if (isLatest) {
        Surface(
            modifier = cardModifier,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp,
        ) {
            VersionCardContent(entry = entry, isLatest = true)
        }
    } else {
        MdcitoCard(
            cardStyle = cardStyle,
            glassIntensity = glassIntensity,
            transparency = transparency,
            modifier = cardModifier,
        ) {
            VersionCardContent(entry = entry, isLatest = false)
        }
    }
}

@Composable
private fun VersionCardContent(
    entry: ChangelogEntry,
    isLatest: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val canExpand = entry.changes.size > COLLAPSED_MAX_LINES

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MdcitoCardDefaults.ContentPadding,
                vertical = 14.dp,
            ),
    ) {
        // 第一行：版本号 + 最新标识 + 日期
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "v${entry.versionName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W700,
                    color = if (isLatest) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (isLatest) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = stringResource(R.string.changelog_latest),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.W600,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(
                text = entry.releaseDate,
                style = MaterialTheme.typography.bodySmall,
                color = if (isLatest) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MdcitoCardDefaults.glassSubtleContentColor()
                },
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 标题：Mdcito V版本号
        Text(
            text = entry.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.W600,
            color = if (isLatest) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )

        // 更新内容（支持展开/折叠）
        if (entry.changes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.animateContentSize(),
            ) {
                val displayedChanges = if (expanded || !canExpand) {
                    entry.changes
                } else {
                    entry.changes.take(COLLAPSED_MAX_LINES)
                }

                displayedChanges.forEach { change ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.5.dp),
                    ) {
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isLatest) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MdcitoCardDefaults.glassSubtleContentColor()
                            },
                            modifier = Modifier.width(12.dp),
                        )
                        Text(
                            text = change,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isLatest) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MdcitoCardDefaults.glassSubtleContentColor()
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // 展开/折叠按钮
                if (canExpand) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(
                                if (expanded) R.string.changelog_collapse
                                else R.string.changelog_expand,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = if (expanded) Icons.Outlined.ExpandLess
                            else Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            // 分隔线
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                color = if (isLatest) {
                    MaterialTheme.colorScheme.outlineVariant
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                },
            )
            Spacer(modifier = Modifier.height(4.dp))

            // 操作按钮：查看发布 + 下载
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                // 查看发布
                if (entry.releaseUrl.isNotBlank()) {
                    TextButton(
                        onClick = {
                            try { uriHandler.openUri(entry.releaseUrl) } catch (_: Exception) {}
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 12.dp,
                            vertical = 4.dp,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.changelog_view_release),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }

                // 下载
                if (entry.releaseUrl.isNotBlank()) {
                    TextButton(
                        onClick = {
                            // 下载页面与发布页面相同，跳转到 Release 页面即可下载
                            try { uriHandler.openUri(entry.releaseUrl) } catch (_: Exception) {}
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 12.dp,
                            vertical = 4.dp,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.changelog_download),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}
