package com.mdcito.app.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role as SemRole
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mdcito.app.R
import com.mdcito.app.data.locale.LanguageHelper
import com.mdcito.app.ui.components.MdcitoCardDefaults
import com.mdcito.app.ui.components.MdcitoSearchField
import timber.log.Timber

private data class OpenSourceLibrary(
    val name: String,
    val license: String,
    val url: String,
)

private val openSourceLibraries = listOf(
    OpenSourceLibrary(
        "Jetpack Compose",
        "Apache 2.0",
        "https://github.com/androidx/androidx",
    ),
    OpenSourceLibrary(
        "Material 3",
        "Apache 2.0",
        "https://github.com/androidx/androidx",
    ),
    OpenSourceLibrary(
        "Material Icons Extended",
        "Apache 2.0",
        "https://github.com/androidx/androidx",
    ),
    OpenSourceLibrary(
        "Compose UI",
        "Apache 2.0",
        "https://github.com/androidx/androidx",
    ),
    OpenSourceLibrary(
        "Compose Runtime",
        "Apache 2.0",
        "https://github.com/androidx/androidx",
    ),
    OpenSourceLibrary(
        "Hilt",
        "Apache 2.0",
        "https://github.com/google/dagger",
    ),
    OpenSourceLibrary(
        "Hilt Navigation Compose",
        "Apache 2.0",
        "https://github.com/androidx/androidx",
    ),
    OpenSourceLibrary(
        "Hilt WorkManager",
        "Apache 2.0",
        "https://github.com/androidx/androidx",
    ),
    OpenSourceLibrary(
        "Room",
        "Apache 2.0",
        "https://github.com/androidx/androidx",
    ),
    OpenSourceLibrary(
        "DataStore",
        "Apache 2.0",
        "https://github.com/androidx/androidx",
    ),
    OpenSourceLibrary(
        "Navigation Compose",
        "Apache 2.0",
        "https://github.com/androidx/androidx",
    ),
    OpenSourceLibrary(
        "Lifecycle",
        "Apache 2.0",
        "https://github.com/androidx/androidx",
    ),
    OpenSourceLibrary(
        "AndroidX Core",
        "Apache 2.0",
        "https://github.com/androidx/androidx",
    ),
    OpenSourceLibrary(
        "Activity Compose",
        "Apache 2.0",
        "https://github.com/androidx/androidx",
    ),
    OpenSourceLibrary(
        "AppCompat",
        "Apache 2.0",
        "https://github.com/androidx/androidx",
    ),
    OpenSourceLibrary(
        "SplashScreen",
        "Apache 2.0",
        "https://github.com/androidx/androidx",
    ),
    OpenSourceLibrary(
        "DocumentFile",
        "Apache 2.0",
        "https://github.com/androidx/androidx",
    ),
    OpenSourceLibrary(
        "Security Crypto",
        "Apache 2.0",
        "https://github.com/androidx/androidx",
    ),
    OpenSourceLibrary(
        "WorkManager",
        "Apache 2.0",
        "https://github.com/androidx/androidx",
    ),
    OpenSourceLibrary(
        "AndroidX Browser",
        "Apache 2.0",
        "https://github.com/androidx/androidx",
    ),
    OpenSourceLibrary(
        "Material Components",
        "Apache 2.0",
        "https://github.com/material-components/material-components-android",
    ),
    OpenSourceLibrary(
        "Kotlin Coroutines",
        "Apache 2.0",
        "https://github.com/Kotlin/kotlinx.coroutines",
    ),
    OpenSourceLibrary(
        "Coil",
        "Apache 2.0",
        "https://github.com/coil-kt/coil",
    ),
    OpenSourceLibrary(
        "OkHttp",
        "Apache 2.0",
        "https://github.com/square/okhttp",
    ),
    OpenSourceLibrary(
        "commonmark-java",
        "BSD 2-Clause",
        "https://github.com/commonmark/commonmark-java",
    ),
    OpenSourceLibrary(
        "JetBrains Markdown",
        "Apache 2.0",
        "https://github.com/JetBrains/markdown",
    ),
    OpenSourceLibrary(
        "SymSpellKt",
        "MIT",
        "https://github.com/Wavesonics/SymSpellKt",
    ),
    OpenSourceLibrary(
        "Android Image Cropper",
        "Apache 2.0",
        "https://github.com/CanHub/Android-Image-Cropper",
    ),
    OpenSourceLibrary(
        "Apache POI",
        "Apache 2.0",
        "https://github.com/apache/poi",
    ),
    OpenSourceLibrary(
        "Gson",
        "Apache 2.0",
        "https://github.com/google/gson",
    ),
    OpenSourceLibrary(
        "Commons Net",
        "Apache 2.0",
        "https://github.com/apache/commons-net",
    ),
    OpenSourceLibrary(
        "SSHJ",
        "Apache 2.0",
        "https://github.com/hierynomus/sshj",
    ),
    OpenSourceLibrary(
        "Timber",
        "Apache 2.0",
        "https://github.com/JakeWharton/timber",
    ),
    OpenSourceLibrary(
        "highlight.js",
        "BSD 3-Clause",
        "https://github.com/highlightjs/highlight.js",
    ),
    OpenSourceLibrary(
        "KaTeX",
        "MIT",
        "https://github.com/KaTeX/KaTeX",
    ),
    OpenSourceLibrary(
        "Noto Sans SC (思源黑体)",
        "OFL 1.1",
        "https://github.com/notofonts/noto-cjk",
    ),
    OpenSourceLibrary(
        "JetBrains Mono",
        "OFL 1.1",
        "https://github.com/JetBrains/JetBrainsMono",
    ),
    OpenSourceLibrary(
        "LXGW WenKai (霞鹜文楷)",
        "OFL 1.1",
        "https://github.com/lxgw/LxgwWenKai",
    ),
)

/** 许可类型分类 */
private val licenseCategories = listOf(
    "All" to "",
    "Apache 2.0" to "Apache 2.0",
    "MIT" to "MIT",
    "BSD" to "BSD",
    "BSD 3-Clause" to "BSD 3-Clause",
    "OFL 1.1" to "OFL 1.1",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OpenSourceScreen(
    onNavigateBack: () -> Unit,
) {
    // 触发语言变更时重组页面
    LanguageHelper.LocalLocaleVersion.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedLicense by remember { mutableStateOf("") }

    // 根据搜索和许可筛选库列表
    val filteredLibraries = remember(searchQuery, selectedLicense) {
        openSourceLibraries.filter { lib ->
            val matchesSearch = searchQuery.isBlank() ||
                lib.name.contains(searchQuery, ignoreCase = true) ||
                lib.license.contains(searchQuery, ignoreCase = true)
            val matchesLicense = selectedLicense.isBlank() ||
                lib.license.startsWith(selectedLicense, ignoreCase = true)
            matchesSearch && matchesLicense
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SettingsTopBar(
            title = stringResource(R.string.open_source),
            onNavigateBack = onNavigateBack,
        )

        // 说明文字
        Text(
            text = stringResource(R.string.open_source_notice),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        )

        // 搜索框
        MdcitoSearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = stringResource(R.string.open_source_search),
            modifier = Modifier.fillMaxWidth(),
        )

        // 许可类型过滤 Chips
        AnimatedVisibility(
            visible = searchQuery.isBlank(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                licenseCategories.forEach { (label, value) ->
                    val displayLabel = if (label == "All") {
                        stringResource(R.string.open_source_count, openSourceLibraries.size)
                    } else {
                        label
                    }
                    FilterChip(
                        selected = selectedLicense == value,
                        onClick = {
                            selectedLicense = if (selectedLicense == value) "" else value
                        },
                        label = {
                            Text(
                                text = displayLabel,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 结果计数
        if (searchQuery.isNotBlank() || selectedLicense.isNotBlank()) {
            Text(
                text = stringResource(
                    R.string.open_source_count,
                    filteredLibraries.size,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        // 库列表
        filteredLibraries.forEach { library ->
            LibraryItem(
                library = library,
                onClick = {
                    try {
                        uriHandler.openUri(library.url)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to open URL: ${library.url}")
                        Toast.makeText(
                            context,
                            context.getString(R.string.open_url_failed),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
            )
        }

        // 空结果提示
        if (filteredLibraries.isEmpty()) {
            Text(
                text = stringResource(R.string.no_search_results),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
            )
        }

        Spacer(modifier = Modifier.height(34.dp))
    }
}

@Composable
private fun LibraryItem(
    library: OpenSourceLibrary,
    onClick: () -> Unit,
) {
    val openInBrowser = stringResource(R.string.open_in_browser)

    SettingsItemCard(
        onClick = onClick,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "${library.name}, ${library.license}"
            role = SemRole.Button
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MdcitoCardDefaults.ContentPadding,
                    vertical = MdcitoCardDefaults.CompactContentPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = library.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.W500,
                )
                Text(
                    text = library.url.removePrefix("https://"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = library.license,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = openInBrowser,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
