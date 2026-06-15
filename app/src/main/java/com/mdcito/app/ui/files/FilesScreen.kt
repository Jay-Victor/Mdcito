package com.mdcito.app.ui.files

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.mdcito.app.R
import com.mdcito.app.data.locale.LanguageHelper
import com.mdcito.app.data.db.entity.FileEntity
import com.mdcito.app.ui.components.LocalCardGlassIntensity
import com.mdcito.app.ui.components.LocalCardStyle
import com.mdcito.app.ui.components.LocalCardTransparency
import com.mdcito.app.ui.components.MdcitoActionItem
import com.mdcito.app.ui.components.MdcitoBottomSheet
import com.mdcito.app.ui.components.MdcitoCard
import com.mdcito.app.ui.components.MdcitoCardDefaults
import com.mdcito.app.ui.components.MdcitoCenterModal
import com.mdcito.app.ui.components.MdcitoSearchField
import com.mdcito.app.ui.components.MdcitoConfirmDialog
import com.mdcito.app.ui.components.MdcitoRenameDialog
import com.mdcito.app.ui.components.MdcitoTextField
import com.mdcito.app.util.HapticHelper
import kotlin.math.roundToInt
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilesScreen(
    onNavigateToEditor: (Long) -> Unit,
    onNavigateToNewFile: () -> Unit,
    viewModel: FilesViewModel = hiltViewModel(),
) {
    LanguageHelper.LocalLocaleVersion.current
    val selectedTab by viewModel.selectedTab.collectAsState()
    val files by viewModel.files.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val isSelectMode by viewModel.isSelectMode.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val currentFolderId by viewModel.currentFolderId.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showFileActionSheet by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showBatchTagDialog by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<FileEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDetailModal by remember { mutableStateOf(false) }

    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val hapticContext = LocalContext.current

    // 删除结果反馈
    LaunchedEffect(Unit) {
        viewModel.deleteEvent.collect { event ->
            when (event) {
                is FilesViewModel.DeleteEvent.Success -> {
                    Toast.makeText(
                        hapticContext,
                        hapticContext.resources.getQuantityString(
                            R.plurals.files_deleted, event.count, event.count
                        ),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                is FilesViewModel.DeleteEvent.Error -> {
                    Toast.makeText(
                        hapticContext,
                        hapticContext.getString(R.string.file_delete_failed),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    val currentItems = if (selectedTab == FileViewTab.FILES) files else folders
    val hasActiveFilter = filterState.fileType != FileTypeFilter.ALL ||
            filterState.timeFilter != TimeFilter.ALL ||
            filterState.pinFilter != PinFilter.ALL ||
            filterState.selectedTags.isNotEmpty()

    val lazyListState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize().graphicsLayer { clip = false }) {
        Column(modifier = Modifier.fillMaxSize().graphicsLayer { clip = false }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                if (currentFolderId != null) {
                    IconButton(
                        onClick = { viewModel.navigateUp() },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column {
                    Text(
                        text = stringResource(R.string.files),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.W600,
                    )
                    Text(
                        text = stringResource(R.string.n_items, currentItems.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (isSelectMode) {
                    TextButton(onClick = { viewModel.clearSelection() }) { Text(stringResource(R.string.cancel)) }
                } else {
                    ViewModeToggle(
                        selectedTab = selectedTab,
                        onSelectTab = { viewModel.selectTab(it) },
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            Icons.Outlined.FilterList, contentDescription = stringResource(R.string.filter),
                            tint = if (hasActiveFilter) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = { viewModel.enterBatchSelectMode() },
                        enabled = currentItems.isNotEmpty(),
                    ) {
                        Icon(
                            Icons.Outlined.DeleteSweep,
                            contentDescription = stringResource(R.string.batch_delete),
                            tint = if (currentItems.isNotEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        )
                    }
                }
            }

            // 选择模式下的操作栏
            AnimatedVisibility(
                visible = isSelectMode,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = {
                            if (selectedIds.size == currentItems.size) {
                                viewModel.clearSelection()
                                viewModel.enterBatchSelectMode()
                            } else {
                                viewModel.selectAll(currentItems.map { it.id })
                            }
                        },
                    ) {
                        Text(
                            text = if (selectedIds.size == currentItems.size) stringResource(R.string.deselect_all)
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

            if (breadcrumbs.size > 1) {
                BreadcrumbBar(
                    breadcrumbs = breadcrumbs,
                    onNavigate = { index -> viewModel.navigateToBreadcrumb(index) },
                )
            }

            MdcitoSearchField(
                query = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                placeholder = stringResource(R.string.search_files),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )

            if (hasActiveFilter && !isSelectMode) {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (filterState.fileType != FileTypeFilter.ALL) {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.updateFilter(filterState.copy(fileType = FileTypeFilter.ALL)) },
                            label = { Text(stringResource(filterState.fileType.labelRes), fontSize = 12.sp) },
                            trailingIcon = {
                                Icon(Icons.Outlined.Close, null, modifier = Modifier.size(14.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                    if (filterState.timeFilter != TimeFilter.ALL) {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.updateFilter(filterState.copy(timeFilter = TimeFilter.ALL)) },
                            label = { Text(stringResource(filterState.timeFilter.labelRes), fontSize = 12.sp) },
                            trailingIcon = {
                                Icon(Icons.Outlined.Close, null, modifier = Modifier.size(14.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                    if (filterState.pinFilter != PinFilter.ALL) {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.updateFilter(filterState.copy(pinFilter = PinFilter.ALL)) },
                            label = { Text(stringResource(filterState.pinFilter.labelRes), fontSize = 12.sp) },
                            trailingIcon = {
                                Icon(Icons.Outlined.Close, null, modifier = Modifier.size(14.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                    filterState.selectedTags.forEach { tag ->
                        FilterChip(
                            selected = true,
                            onClick = {
                                viewModel.updateFilter(
                                    filterState.copy(selectedTags = filterState.selectedTags - tag)
                                )
                            },
                            label = { Text(tag, fontSize = 12.sp) },
                            trailingIcon = {
                                Icon(Icons.Outlined.Close, null, modifier = Modifier.size(14.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                    TextButton(onClick = { viewModel.resetFilter() }) {
                        Text(stringResource(R.string.clear_all), fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (currentItems.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (selectedTab == FileViewTab.FILES)
                            Icons.AutoMirrored.Outlined.InsertDriveFile else Icons.Outlined.CreateNewFolder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (selectedTab == FileViewTab.FILES)
                            stringResource(R.string.empty_files_hint)
                        else
                            stringResource(R.string.empty_folders_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        viewModel.refresh()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer { clip = false },
                ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.graphicsLayer { clip = false },
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        top = 4.dp,
                        end = 20.dp,
                        bottom = if (isSelectMode) 80.dp else 100.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(currentItems.size, key = { currentItems[it].id }) { index ->
                        val item = currentItems[index]
                        SwipeableFileCard(
                            file = item,
                            isSelectMode = isSelectMode,
                            isSelected = selectedIds.contains(item.id),
                            onSelect = { viewModel.toggleSelect(item.id) },
                            onClick = {
                                if (isSelectMode) {
                                    viewModel.toggleSelect(item.id)
                                } else {
                                    if (item.type == "file") onNavigateToEditor(item.id)
                                    else viewModel.navigateToFolder(item.id, item.name)
                                }
                            },
                            onPin = { viewModel.togglePinned(item) },
                            onDelete = {
                                selectedFile = item
                                showDeleteConfirm = true
                            },
                            onMore = {
                                selectedFile = item
                                showFileActionSheet = true
                            },
                        )
                    }
                }
                }
            }
        }

        if (!isSelectMode) {
            var fabOffsetX by remember { mutableFloatStateOf(0f) }
            var fabOffsetY by remember { mutableFloatStateOf(0f) }
            var isDragging by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(if (isDragging) 1.1f else 1f, label = "fabScale")
            val elevation by animateDpAsState(if (isDragging) 8.dp else 4.dp, label = "fabElevation")
            val fabSizePx = with(LocalDensity.current) { 56.dp.toPx() }
            val fabPaddingPx = with(LocalDensity.current) { 16.dp.toPx() }
            var containerWidth by remember { mutableFloatStateOf(0f) }
            var containerHeight by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        containerWidth = coordinates.size.width.toFloat()
                        containerHeight = coordinates.size.height.toFloat()
                    },
                contentAlignment = Alignment.BottomEnd,
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = elevation,
                    modifier = Modifier
                        .padding(16.dp)
                        .offset { IntOffset(fabOffsetX.roundToInt(), fabOffsetY.roundToInt()) }
                        .size(56.dp)
                        .scale(scale)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                down.consume()
                                isDragging = false
                                var hasMoved = false
                                val startX = fabOffsetX
                                val startY = fabOffsetY

                                do {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull()
                                    if (change != null && change.pressed) {
                                        val dx = change.position.x - change.previousPosition.x
                                        val dy = change.position.y - change.previousPosition.y
                                        val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                                        if (!hasMoved && distance > viewConfiguration.touchSlop) {
                                            hasMoved = true
                                            isDragging = true
                                        }

                                        if (hasMoved) {
                                            val newX = fabOffsetX + dx
                                            val newY = fabOffsetY + dy
                                            fabOffsetX = newX.coerceIn(
                                                -(containerWidth - fabSizePx - fabPaddingPx * 2),
                                                0f
                                            )
                                            fabOffsetY = newY.coerceIn(
                                                -(containerHeight - fabSizePx - fabPaddingPx * 2),
                                                0f
                                            )
                                            change.consume()
                                        }
                                    }
                                } while (event.changes.any { it.pressed })

                                if (!hasMoved) {
                                    showCreateDialog = true
                                }
                                isDragging = false
                            }
                        },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.create_new),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        val context = LocalContext.current
        val workspacePath by viewModel.workspacePath.collectAsState()
        val currentFolderEntity by viewModel.currentFolderEntity.collectAsState()

        var selectedSaveUri by remember { mutableStateOf<android.net.Uri?>(null) }
        val defaultPathStr = stringResource(R.string.default_path)
        var selectedSaveName by remember { mutableStateOf(defaultPathStr) }

        // 当用户在文件夹内部时，自动锁定保存路径为当前文件夹；否则显示实际工作区路径
        val isInFolder = currentFolderId != null
        LaunchedEffect(isInFolder, currentFolderEntity, workspacePath) {
            if (isInFolder && currentFolderEntity != null) {
                val folderUri = try { android.net.Uri.parse(currentFolderEntity!!.path) } catch (_: Exception) { null }
                selectedSaveUri = folderUri
                selectedSaveName = currentFolderEntity!!.name
            } else {
                selectedSaveUri = null
                // 显示实际工作区路径而非笼统的"默认路径"
                val wsPath = workspacePath
                if (wsPath != null) {
                    if (wsPath.startsWith("content://")) {
                        selectedSaveName = try {
                            val treeUri = android.net.Uri.parse(wsPath)
                            val docFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
                            docFile?.name ?: wsPath.substringAfterLast("/")
                        } catch (_: Exception) {
                            wsPath.substringAfterLast("/")
                        }
                    } else {
                        // 传统文件路径，显示最后两级目录名（如 Documents/Mdcito）
                        val file = java.io.File(wsPath)
                        val parentName = file.parentFile?.name
                        selectedSaveName = if (parentName != null) "$parentName/${file.name}" else file.name
                    }
                } else {
                    // workspacePath 未设置时显示默认路径标识
                    selectedSaveName = defaultPathStr
                }
            }
        }

        val pickDirLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri: android.net.Uri? ->
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

        MdcitoCenterModal(
            onDismissRequest = { showCreateDialog = false },
            visible = showCreateDialog,
            title = stringResource(R.string.create_new),
        ) {
            key(showCreateDialog) {
                var createType by remember { mutableStateOf(if (selectedTab == FileViewTab.FILES) "file" else "folder") }
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
                            .clip(RoundedCornerShape(8.dp))
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
                            .clip(RoundedCornerShape(8.dp))
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
                errorText = if (itemName.isEmpty()) stringResource(R.string.name_empty_error) else null,
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
                    .then(
                        if (isInFolder) Modifier
                        else Modifier.clickable { pickDirLauncher.launch(null) }
                    ),
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
                        tint = if (isInFolder) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isInFolder) stringResource(R.string.save_location_current)
                            else stringResource(R.string.save_location),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isInFolder) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = selectedSaveName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selectedSaveUri == null) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }
                    if (!isInFolder) {
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                            if (createType == "file") {
                                val tags = itemTags.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                viewModel.createFile(
                                    name = itemName,
                                    type = "file",
                                    tags = tags,
                                    saveUri = selectedSaveUri,
                                )
                            } else {
                                viewModel.createFile(
                                    name = itemName,
                                    type = "folder",
                                    saveUri = selectedSaveUri,
                                )
                            }
                            showCreateDialog = false
                        } else {
                            isError = true
                        }
                    },
                    enabled = itemName.isNotEmpty(),
                ) { Text(stringResource(R.string.create)) }
            }
        }
    }

        MdcitoBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            visible = showFilterSheet,
            title = stringResource(R.string.filter_and_sort),
        ) {
            var selectedFilterTab by remember { mutableStateOf(0) }
            val filterTabIds = listOf(R.string.sort_by, R.string.type, R.string.time, R.string.pin) + if (allTags.isNotEmpty()) listOf(R.string.tag) else emptyList()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                filterTabIds.forEachIndexed { index, tabId ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedFilterTab == index) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedFilterTab = index },
                    ) {
                        Text(
                            text = stringResource(tabId),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedFilterTab == index) FontWeight.W600 else FontWeight.Normal,
                            color = if (selectedFilterTab == index) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            ) {
                when (selectedFilterTab) {
                    0 -> FilterWheelContent(
                        items = SortOrder.entries.map { stringResource(it.labelRes) },
                        selectedIndex = SortOrder.entries.indexOf(sortOrder),
                        onSelect = { viewModel.updateSortOrder(SortOrder.entries[it]) },
                    )
                    1 -> FilterWheelContent(
                        items = FileTypeFilter.entries.map { stringResource(it.labelRes) },
                        selectedIndex = FileTypeFilter.entries.indexOf(filterState.fileType),
                        onSelect = { viewModel.updateFilter(filterState.copy(fileType = FileTypeFilter.entries[it])) },
                    )
                    2 -> FilterWheelContent(
                        items = TimeFilter.entries.map { stringResource(it.labelRes) },
                        selectedIndex = TimeFilter.entries.indexOf(filterState.timeFilter),
                        onSelect = { viewModel.updateFilter(filterState.copy(timeFilter = TimeFilter.entries[it])) },
                    )
                    3 -> FilterWheelContent(
                        items = PinFilter.entries.map { stringResource(it.labelRes) },
                        selectedIndex = PinFilter.entries.indexOf(filterState.pinFilter),
                        onSelect = { viewModel.updateFilter(filterState.copy(pinFilter = PinFilter.entries[it])) },
                    )
                    4 -> if (allTags.isNotEmpty()) {
                        FilterWheelContent(
                            items = allTags.toList(),
                            selectedIndex = -1,
                            onSelect = { tagIndex ->
                                val tagName = allTags.toList()[tagIndex]
                                val newTags = if (tagName in filterState.selectedTags)
                                    filterState.selectedTags - tagName
                                else
                                    filterState.selectedTags + tagName
                                viewModel.updateFilter(filterState.copy(selectedTags = newTags))
                            },
                            isMultiSelect = true,
                            selectedItems = filterState.selectedTags,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = {
                    viewModel.resetFilter()
                    viewModel.updateSortOrder(SortOrder.TIME_DESC)
                }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { showFilterSheet = false }) { Text(stringResource(R.string.done)) }
            }
        }

        MdcitoBottomSheet(
            onDismissRequest = { showFileActionSheet = false },
            visible = showFileActionSheet,
            title = selectedFile?.name ?: stringResource(R.string.file_operations),
        ) {
            selectedFile?.let { file ->
                MdcitoActionItem(Icons.Outlined.Share, stringResource(R.string.share)) {
                    showFileActionSheet = false
                    viewModel.shareFile(file, hapticContext)
                }
                MdcitoActionItem(Icons.Outlined.Edit, stringResource(R.string.rename)) {
                    showFileActionSheet = false
                    showRenameDialog = true
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                MdcitoActionItem(
                    Icons.Outlined.PushPin,
                    if (file.isPinned) stringResource(R.string.unpin) else stringResource(R.string.pin)
                ) {
                    viewModel.togglePinned(file)
                    showFileActionSheet = false
                }
                MdcitoActionItem(Icons.Outlined.ContentCopy, stringResource(R.string.copy)) {
                    viewModel.copyFile(file)
                    showFileActionSheet = false
                }
                MdcitoActionItem(Icons.Outlined.Bookmark, stringResource(R.string.tag)) {
                    showFileActionSheet = false
                    showTagDialog = true
                }
                MdcitoActionItem(Icons.Outlined.Info, stringResource(R.string.details)) {
                    showFileActionSheet = false
                    showDetailModal = true
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                MdcitoActionItem(Icons.Outlined.Delete, stringResource(R.string.delete), isDestructive = true) {
                    showFileActionSheet = false
                    showDeleteConfirm = true
                }
            }
        }

        selectedFile?.let { file ->
            MdcitoRenameDialog(
                visible = showRenameDialog,
                currentName = file.name,
                onRename = { newName ->
                    viewModel.renameFile(file, newName)
                    showRenameDialog = false
                },
                onDismiss = { showRenameDialog = false },
            )

            MdcitoConfirmDialog(
                visible = showDeleteConfirm && !isSelectMode,
                title = if (file.type == "file") stringResource(R.string.delete_file) else stringResource(R.string.delete_folder),
                message = stringResource(R.string.confirm_delete_item, file.name),
                confirmText = stringResource(R.string.delete),
                isDangerous = true,
                onConfirm = {
                    viewModel.deleteFile(file)
                    showDeleteConfirm = false
                    selectedFile = null
                    HapticHelper.delete(hapticContext)
                },
                onDismiss = { showDeleteConfirm = false },
            )

            TagDialog(
                visible = showTagDialog,
                file = currentItems.find { it.id == file.id } ?: file,
                allTags = allTags,
                onAddTag = { tag ->
                    val latestFile = currentItems.find { it.id == file.id } ?: file
                    viewModel.addTagToFile(latestFile, tag)
                },
                onRemoveTag = { tag ->
                    val latestFile = currentItems.find { it.id == file.id } ?: file
                    viewModel.removeTagFromFile(latestFile, tag)
                },
                onDismiss = { showTagDialog = false },
            )

            FileDetailModal(
                visible = showDetailModal,
                file = file,
                onDismiss = { showDetailModal = false },
            )
        }

        if (isSelectMode && selectedIds.isNotEmpty()) {
            MdcitoConfirmDialog(
                visible = showBatchDeleteConfirm,
                title = stringResource(R.string.batch_delete),
                message = stringResource(R.string.confirm_batch_delete, selectedIds.size),
                confirmText = stringResource(R.string.delete),
                isDangerous = true,
                onConfirm = {
                    viewModel.deleteByIds(selectedIds.toList())
                    showBatchDeleteConfirm = false
                },
                onDismiss = { showBatchDeleteConfirm = false },
            )
        }

        MdcitoConfirmDialog(
            visible = showDeleteAllConfirm,
            title = stringResource(R.string.delete_all),
            message = stringResource(R.string.confirm_delete_all),
            confirmText = stringResource(R.string.delete_all),
            isDangerous = true,
            onConfirm = {
                viewModel.deleteAll()
                showDeleteAllConfirm = false
            },
            onDismiss = { showDeleteAllConfirm = false },
        )

        BatchTagDialog(
            visible = showBatchTagDialog,
            allTags = allTags,
            onAddTag = { tag -> viewModel.addTagToSelectedFiles(tag) },
            onDismiss = { showBatchTagDialog = false },
        )
    }
}

@Composable
private fun BreadcrumbBar(
    breadcrumbs: List<BreadcrumbItem>,
    onNavigate: (Int) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            breadcrumbs.forEachIndexed { index, item ->
                if (index > 0) {
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MdcitoCardDefaults.glassFaintContentColor(),
                    )
                }
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (index == breadcrumbs.lastIndex) FontWeight.W600 else FontWeight.Normal,
                    color = if (index == breadcrumbs.lastIndex) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(enabled = index < breadcrumbs.lastIndex) { onNavigate(index) }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableFileCard(
    file: FileEntity,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit,
) {
    val tags = FilesViewModel.parseTags(file.tags)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val actionWidthPx = with(density) { 80.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()

    val offsetX = remember { androidx.compose.animation.core.Animatable(0f) }

    val draggableState = rememberDraggableState { delta ->
        coroutineScope.launch {
            offsetX.snapTo((offsetX.value + delta).coerceIn(-actionWidthPx, actionWidthPx))
        }
    }

    if (isSelectMode) {
        MdcitoCard(
            onClick = onSelect,
            cardStyle = LocalCardStyle.current,
            glassIntensity = LocalCardGlassIntensity.current,
            transparency = LocalCardTransparency.current,
            modifier = Modifier.fillMaxWidth(),
        ) {
            FileCardContent(
                file = file,
                tags = tags,
                isSelectMode = true,
                isSelected = isSelected,
                onSelect = onSelect,
                onMore = onMore,
            )
        }
        return
    }

    val currentOffset = offsetX.value
    val revealProgress = (kotlin.math.abs(currentOffset) / actionWidthPx).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStopped = { velocity: Float ->
                    val threshold = actionWidthPx * 0.5f
                    val velocityThreshold = actionWidthPx * 2f
                    when {
                        currentOffset > threshold || velocity > velocityThreshold -> onPin()
                        currentOffset < -threshold || velocity < -velocityThreshold -> onDelete()
                    }
                    coroutineScope.launch {
                        offsetX.animateTo(
                            0f,
                            SpringSpec(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                        )
                    }
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when {
                        currentOffset > 0f -> Color(0xFFC8A84E).copy(alpha = 0.15f * revealProgress)
                        currentOffset < 0f -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f * revealProgress)
                        else -> Color.Transparent
                    }
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.graphicsLayer { alpha = revealProgress },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = if (file.isPinned) stringResource(R.string.unpin) else stringResource(R.string.pin),
                        tint = Color(0xFFC8A84E),
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (file.isPinned) stringResource(R.string.unpin) else stringResource(R.string.pin),
                        color = Color(0xFFC8A84E),
                        fontWeight = FontWeight.W600,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.graphicsLayer { alpha = revealProgress },
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.W600,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        MdcitoCard(
            onClick = onClick,
            cardStyle = LocalCardStyle.current,
            glassIntensity = LocalCardGlassIntensity.current,
            transparency = LocalCardTransparency.current,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(currentOffset.roundToInt(), 0) },
        ) {
            FileCardContent(
                file = file,
                tags = tags,
                isSelectMode = false,
                isSelected = isSelected,
                onSelect = onSelect,
                onMore = onMore,
            )
        }
    }
}

@Composable
private fun FileCardContent(
    file: FileEntity,
    tags: List<String>,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onMore: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(MdcitoCardDefaults.ContentPadding),
    ) {
        if (isSelectMode) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .then(
                        if (!isSelected) Modifier
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .padding(2.dp)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

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

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                maxLines = 1,
            )
            Text(
                text = if (file.type == "file")
                    "${formatFileSize(file.size)} · ${formatDate(file.updatedAt)}"
                else
                    file.path,
                style = MaterialTheme.typography.bodySmall,
                color = MdcitoCardDefaults.glassSubtleContentColor(),
                maxLines = 1,
            )
            if (tags.isNotEmpty() && !isSelectMode) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    tags.take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.height(18.dp),
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    if (tags.size > 3) {
                        Text(
                            text = "+${tags.size - 3}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MdcitoCardDefaults.glassSubtleContentColor(),
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }

        if (file.isPinned && !isSelectMode) {
            Icon(
                imageVector = Icons.Outlined.PushPin,
                contentDescription = stringResource(R.string.pin),
                tint = Color(0xFFC8A84E),
                modifier = Modifier.size(18.dp),
            )
        }

        if (!isSelectMode) {
            IconButton(
                onClick = onMore,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DragHandle,
                    contentDescription = stringResource(R.string.more_actions),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagDialog(
    visible: Boolean,
    file: FileEntity,
    allTags: Set<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newTag by remember { mutableStateOf("") }
    val fileTags = FilesViewModel.parseTags(file.tags)

    MdcitoCenterModal(
        onDismissRequest = onDismiss,
        visible = visible,
        title = stringResource(R.string.tag_management),
    ) {
        if (fileTags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                fileTags.forEach { tag ->
                    FilterChip(
                        selected = true,
                        onClick = { onRemoveTag(tag) },
                        label = { Text(tag, fontSize = 13.sp) },
                        trailingIcon = {
                            Icon(Icons.Outlined.Close, null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (allTags.isNotEmpty()) {
            Text(
                text = stringResource(R.string.existing_tags),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                allTags.filter { it !in fileTags }.forEach { tag ->
                    FilterChip(
                        selected = false,
                        onClick = { onAddTag(tag) },
                        label = { Text(tag, fontSize = 13.sp) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        MdcitoTextField(
            value = newTag,
            onValueChange = { newTag = it },
            placeholder = stringResource(R.string.enter_new_tag),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = {
                    if (newTag.isNotBlank()) {
                        onAddTag(newTag.trim())
                        newTag = ""
                    }
                },
                enabled = newTag.isNotBlank(),
            ) { Text(stringResource(R.string.add)) }
        }
    }
}

@Composable
private fun ViewModeToggle(
    selectedTab: FileViewTab,
    onSelectTab: (FileViewTab) -> Unit,
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
        listOf(
            FileViewTab.FILES to R.string.file_label,
            FileViewTab.FOLDERS to R.string.filter_folders,
        ).forEach { (tab, labelResId) ->
            val selected = selectedTab == tab
            Box(
                modifier = Modifier
                    .background(
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                        shape = pillShape,
                    )
                    .clip(pillShape)
                    .clickable { onSelectTab(tab) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(labelResId),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.W600 else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BatchTagDialog(
    visible: Boolean,
    allTags: Set<String>,
    onAddTag: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newTag by remember { mutableStateOf("") }

    MdcitoCenterModal(
        onDismissRequest = onDismiss,
        visible = visible,
        title = stringResource(R.string.batch_add_tags),
    ) {
        if (allTags.isNotEmpty()) {
            Text(
                text = stringResource(R.string.select_existing_tags),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                allTags.forEach { tag ->
                    FilterChip(
                        selected = false,
                        onClick = { onAddTag(tag) },
                        label = { Text(tag, fontSize = 13.sp) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        MdcitoTextField(
            value = newTag,
            onValueChange = { newTag = it },
            placeholder = stringResource(R.string.enter_new_tag),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = {
                    if (newTag.isNotBlank()) {
                        onAddTag(newTag.trim())
                        newTag = ""
                    }
                },
                enabled = newTag.isNotBlank(),
            ) { Text(stringResource(R.string.add)) }
        }
    }
}

@Composable
private fun FileDetailModal(
    visible: Boolean,
    file: FileEntity,
    onDismiss: () -> Unit,
) {
    MdcitoCenterModal(
        onDismissRequest = onDismiss,
        visible = visible,
        title = stringResource(R.string.file_details),
    ) {
        val tags = FilesViewModel.parseTags(file.tags)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())

        DetailRow(stringResource(R.string.name), file.name)
        DetailRow(stringResource(R.string.type), if (file.type == "file") stringResource(R.string.file_label) else stringResource(R.string.filter_folders))
        DetailRow(stringResource(R.string.size), formatFileSize(file.size))
        DetailRow(stringResource(R.string.path), file.path)
        DetailRow(stringResource(R.string.created_at), sdf.format(java.util.Date(file.createdAt)))
        DetailRow(stringResource(R.string.modified_at), sdf.format(java.util.Date(file.updatedAt)))
        DetailRow(stringResource(R.string.pin), if (file.isPinned) stringResource(R.string.yes) else stringResource(R.string.no))
        if (tags.isNotEmpty()) {
            DetailRow(stringResource(R.string.tag), tags.joinToString("、"))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.W500,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun getDirDisplayName(context: android.content.Context, uri: android.net.Uri): String {
    return try {
        val docId = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)
        docId?.name ?: uri.lastPathSegment ?: context.getString(R.string.custom_directory)
    } catch (_: Exception) {
        uri.lastPathSegment ?: context.getString(R.string.custom_directory)
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

@Composable
private fun FilterWheelContent(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    isMultiSelect: Boolean = false,
    selectedItems: Set<String> = emptySet(),
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(items.size) { index ->
            val item = items[index]
            val isSelected = if (isMultiSelect) item in selectedItems else index == selectedIndex

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(index) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isMultiSelect) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.W600 else FontWeight.Normal,
                )
            }
        }
    }
}
