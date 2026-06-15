package com.mdcito.app.ui.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import com.mdcito.app.R
import androidx.compose.ui.res.stringResource
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mdcito.app.data.db.entity.FileEntity
import com.mdcito.app.data.db.entity.HistoryEntity
import com.mdcito.app.ui.components.LocalCardGlassIntensity
import com.mdcito.app.ui.components.LocalCardStyle
import com.mdcito.app.ui.components.LocalCardTransparency
import com.mdcito.app.ui.components.MdcitoCard
import com.mdcito.app.ui.components.MdcitoCardDefaults
import com.mdcito.app.ui.components.MdcitoCenterModal
import com.mdcito.app.ui.components.MdcitoTextField

@Composable
fun HomeScreen(
    onNavigateToEditor: (Long) -> Unit,
    onNavigateToNewFile: () -> Unit,
    onNavigateToFiles: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val stats by viewModel.stats.collectAsState()
    val pinnedFiles by viewModel.pinnedFiles.collectAsState()
    val recentHistory by viewModel.recentHistory.collectAsState()
    val fileNameMap by viewModel.fileNameMap.collectAsState()
    val fileEntityMap by viewModel.fileEntityMap.collectAsState()
    val pinnedFilterTab by viewModel.pinnedFilterTab.collectAsState()
    val recentFilterTab by viewModel.recentFilterTab.collectAsState()
    val hasPinnedFiles by viewModel.hasPinnedFiles.collectAsState()
    val hasRecentHistory by viewModel.hasRecentHistory.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var createType by remember { mutableStateOf("file") }

    val context = LocalContext.current

    // 导入结果反馈
    LaunchedEffect(Unit) {
        viewModel.importEvent.collect { event ->
            when (event) {
                is HomeViewModel.ImportEvent.Success -> {
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.import_file_success),
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
                is HomeViewModel.ImportEvent.Error -> {
                    android.widget.Toast.makeText(
                        context,
                        event.message,
                        android.widget.Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    var selectedSaveUri by remember { mutableStateOf<Uri?>(null) }
    val defaultPathLabel = stringResource(R.string.default_path)
    var selectedSaveName by remember { mutableStateOf(defaultPathLabel) }

    val pickDirLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (_: Exception) {}
            selectedSaveUri = it
            selectedSaveName = getDirDisplayName(context, it)
        }
    }

    val openFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importFile(it)
        }
    }

    val openFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (_: Exception) {}
            val dirName = getDirDisplayName(context, it)
            viewModel.importFolder(it, dirName)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            BrandSection()
        }

        item {
            QuickStartSection(
                onNewFile = {
                    createType = "file"
                    showCreateDialog = true
                },
                onNewFolder = {
                    createType = "folder"
                    showCreateDialog = true
                },
                onOpenFile = {
                    openFileLauncher.launch(arrayOf("text/markdown", "text/plain", "text/*"))
                },
                onOpenFolder = {
                    openFolderLauncher.launch(null)
                },
            )
        }

        item {
            StatsSection(stats = stats)
        }

        if (hasPinnedFiles) {
            item {
                SectionHeader(
                    title = stringResource(R.string.pinned),
                    onViewAll = onNavigateToFiles,
                    tabs = HomeFilterTab.entries,
                    selectedTab = pinnedFilterTab,
                    onTabSelected = { viewModel.selectPinnedTab(it) },
                )
            }
            if (pinnedFiles.isNotEmpty()) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(pinnedFiles) { file ->
                            PinnedFileCard(
                                file = file,
                                onClick = {
                                    if (file.type == "file") onNavigateToEditor(file.id)
                                    else onNavigateToFiles()
                                },
                            )
                        }
                    }
                }
            } else {
                item {
                    EmptyFilterHint(stringResource(R.string.no_pinned_files))
                }
            }
        }

        if (hasRecentHistory) {
            item {
                SectionHeader(
                    title = stringResource(R.string.recent_access),
                    onViewAll = onNavigateToHistory,
                    tabs = HomeFilterTab.entries,
                    selectedTab = recentFilterTab,
                    onTabSelected = { viewModel.selectRecentTab(it) },
                )
            }
            if (recentHistory.isNotEmpty()) {
                items(recentHistory, key = { it.id }) { history ->
                    RecentItem(
                        history = history,
                        fileName = fileNameMap[history.fileId] ?: stringResource(R.string.file_id_format, history.fileId),
                        fileEntity = fileEntityMap[history.fileId],
                        onClick = {
                            if (fileEntityMap[history.fileId]?.type == "folder") {
                                onNavigateToHistory()
                            } else {
                                onNavigateToEditor(history.fileId)
                            }
                        })
                }
            } else {
                item {
                    EmptyFilterHint(stringResource(R.string.no_recent_access))
                }
            }
        }

        if (!hasPinnedFiles && !hasRecentHistory) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(72.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.start_markdown_journey),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.tap_to_create_doc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    MdcitoCenterModal(
        onDismissRequest = { showCreateDialog = false },
        visible = showCreateDialog,
        title = if (createType == "file") stringResource(R.string.new_file) else stringResource(R.string.new_folder),
    ) {
        var itemName by remember { mutableStateOf("") }
        var itemTags by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (createType == "file") MaterialTheme.colorScheme.primary
                else Color.Transparent,
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { createType = "file" }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (createType == "file") MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.file_label),
                        fontWeight = FontWeight.W600,
                        color = if (createType == "file") MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (createType == "folder") MaterialTheme.colorScheme.primary
                else Color.Transparent,
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { createType = "folder" }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (createType == "folder") MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.filter_folders),
                        fontWeight = FontWeight.W600,
                        color = if (createType == "folder") MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        MdcitoTextField(
            value = itemName,
            onValueChange = {
                itemName = it
                isError = it.isEmpty()
            },
            placeholder = if (createType == "file") stringResource(R.string.file_name_hint) else stringResource(R.string.folder_name_hint),
            isError = isError,
            errorText = if (itemName.isEmpty()) stringResource(R.string.error_name_empty) else null,
        )

        if (createType == "file") {
            Spacer(modifier = Modifier.height(10.dp))
            MdcitoTextField(
                value = itemTags,
                onValueChange = { itemTags = it },
                placeholder = stringResource(R.string.tags_hint),
                singleLine = true,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { pickDirLauncher.launch(null) },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.save_location),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = selectedSaveName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selectedSaveUri == null) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { showCreateDialog = false }) { Text(stringResource(R.string.cancel)) }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = {
                    if (itemName.isNotEmpty()) {
                        val tags = itemTags.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        viewModel.createFile(
                            name = itemName,
                            type = createType,
                            tags = tags,
                            saveUri = selectedSaveUri,
                        )
                        showCreateDialog = false
                        selectedSaveUri = null
                        selectedSaveName = defaultPathLabel
                    } else {
                        isError = true
                    }
                },
                enabled = itemName.isNotEmpty(),
            ) { Text(stringResource(R.string.create)) }
        }
    }
    }
}

private fun getDirDisplayName(context: android.content.Context, uri: Uri): String {
    return try {
        val docId = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)
        docId?.name ?: uri.lastPathSegment ?: context.getString(R.string.custom_directory)
    } catch (_: Exception) {
        uri.lastPathSegment ?: context.getString(R.string.custom_directory)
    }
}

@Composable
private fun BrandSection() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 8.dp, bottom = 8.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 6.dp,
            modifier = Modifier.size(56.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_square),
                contentDescription = stringResource(R.string.app_logo_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = "Mdcito",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.W700,
                fontSize = 28.sp,
            )
            Text(
                text = stringResource(R.string.markdown_editor),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickStartSection(
    onNewFile: () -> Unit,
    onNewFolder: () -> Unit,
    onOpenFile: () -> Unit,
    onOpenFolder: () -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.quick_start),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.W600,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            QuickActionCard(
                icon = Icons.AutoMirrored.Outlined.InsertDriveFile,
                label = stringResource(R.string.new_file),
                iconColor = Color(0xFF6B9E7E),
                onClick = onNewFile,
                modifier = Modifier.weight(1f),
            )
            QuickActionCard(
                icon = Icons.Outlined.CreateNewFolder,
                label = stringResource(R.string.new_folder),
                iconColor = Color(0xFF7E8EB8),
                onClick = onNewFolder,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            QuickActionCard(
                icon = Icons.Outlined.FolderOpen,
                label = stringResource(R.string.open_file),
                iconColor = Color(0xFFC8B47E),
                onClick = onOpenFile,
                modifier = Modifier.weight(1f),
            )
            QuickActionCard(
                icon = Icons.Outlined.Add,
                label = stringResource(R.string.open_folder),
                iconColor = Color(0xFFA47EB8),
                onClick = onOpenFolder,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MdcitoCard(
        onClick = onClick,
        cardStyle = LocalCardStyle.current,
        glassIntensity = LocalCardGlassIntensity.current,
        transparency = LocalCardTransparency.current,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = iconColor.copy(alpha = 0.12f),
                ),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(10.dp),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W500,
            )
        }
    }
}

@Composable
private fun StatsSection(stats: HomeStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatCard(value = "${stats.fileCount}", label = stringResource(R.string.file_label), modifier = Modifier.weight(1f))
        StatCard(value = "${stats.folderCount}", label = stringResource(R.string.filter_folders), modifier = Modifier.weight(1f))
        StatCard(value = stats.formatTotalSize(), label = stringResource(R.string.total_size), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    MdcitoCard(
        cardStyle = LocalCardStyle.current,
        glassIntensity = LocalCardGlassIntensity.current,
        transparency = LocalCardTransparency.current,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MdcitoCardDefaults.glassSubtleContentColor(),
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onViewAll: () -> Unit,
    tabs: List<HomeFilterTab> = emptyList(),
    selectedTab: HomeFilterTab? = null,
    onTabSelected: (HomeFilterTab) -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W600,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onViewAll) {
                Text(stringResource(R.string.view_all))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        if (tabs.isNotEmpty() && selectedTab != null) {
            Spacer(modifier = Modifier.height(8.dp))
            HomeSegmentedControl(
                tabs = tabs,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
            )
        }
    }
}

@Composable
private fun HomeSegmentedControl(
    tabs: List<HomeFilterTab>,
    selectedTab: HomeFilterTab,
    onTabSelected: (HomeFilterTab) -> Unit,
) {
    val pillShape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = pillShape,
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = pillShape,
            )
            .padding(3.dp),
    ) {
        tabs.forEach { tab ->
            val selected = selectedTab == tab
            Box(
                modifier = Modifier
                    .background(
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                        shape = pillShape,
                    )
                    .clip(pillShape)
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (tab) {
                        HomeFilterTab.ALL -> stringResource(R.string.filter_all)
                        HomeFilterTab.FILES -> stringResource(R.string.filter_files)
                        HomeFilterTab.FOLDERS -> stringResource(R.string.filter_folders)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.W600 else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyFilterHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun PinnedFileCard(file: FileEntity, onClick: () -> Unit) {
    MdcitoCard(
        onClick = onClick,
        cardStyle = LocalCardStyle.current,
        glassIntensity = LocalCardGlassIntensity.current,
        transparency = LocalCardTransparency.current,
        modifier = Modifier.width(160.dp),
    ) {
        Column(
            modifier = Modifier.padding(MdcitoCardDefaults.CompactContentPadding),
        ) {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (file.type == "file")
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.tertiaryContainer,
                ),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = if (file.type == "file") Icons.Outlined.Description
                    else Icons.Outlined.CreateNewFolder,
                    contentDescription = if (file.type == "file") stringResource(R.string.file_label) else stringResource(R.string.filter_folders),
                    tint = if (file.type == "file")
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(10.dp),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = file.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                maxLines = 2,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (file.type == "file") stringResource(R.string.file_label) else stringResource(R.string.filter_folders),
                style = MaterialTheme.typography.bodySmall,
                color = MdcitoCardDefaults.glassSubtleContentColor(),
            )
        }
    }
}

@Composable
private fun RecentItem(history: HistoryEntity, fileName: String, fileEntity: FileEntity? = null, onClick: () -> Unit) {
    val context = LocalContext.current
    val isFolder = fileEntity?.type == "folder"
    MdcitoCard(
        onClick = onClick,
        cardStyle = LocalCardStyle.current,
        glassIntensity = LocalCardGlassIntensity.current,
        transparency = LocalCardTransparency.current,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 12.dp),
        ) {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFolder)
                        MaterialTheme.colorScheme.tertiaryContainer
                    else
                        MaterialTheme.colorScheme.primaryContainer,
                ),
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = if (isFolder) Icons.Outlined.CreateNewFolder
                    else Icons.Outlined.Description,
                    contentDescription = null,
                    tint = if (isFolder)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(8.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.W600,
                )
                if (fileEntity != null && fileEntity.type == "folder") {
                    Text(
                        text = fileEntity.path,
                        style = MaterialTheme.typography.bodySmall,
                        color = MdcitoCardDefaults.glassSubtleContentColor(),
                        maxLines = 1,
                    )
                } else {
                    Text(
                        text = formatTimestamp(context, history.accessedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MdcitoCardDefaults.glassSubtleContentColor(),
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = stringResource(R.string.open),
                tint = MdcitoCardDefaults.glassFaintContentColor(),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun formatTimestamp(context: android.content.Context, timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> context.getString(R.string.just_now)
        diff < 3_600_000 -> context.getString(R.string.minutes_ago, diff / 60_000)
        diff < 86_400_000 -> context.getString(R.string.hours_ago, diff / 3_600_000)
        else -> context.getString(R.string.days_ago, diff / 86_400_000)
    }
}
