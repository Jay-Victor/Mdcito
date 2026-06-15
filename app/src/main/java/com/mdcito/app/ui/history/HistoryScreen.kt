package com.mdcito.app.ui.history

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mdcito.app.R
import com.mdcito.app.data.locale.LanguageHelper
import com.mdcito.app.data.db.entity.FileEntity
import com.mdcito.app.data.db.entity.HistoryEntity
import com.mdcito.app.ui.components.LocalCardGlassIntensity
import com.mdcito.app.ui.components.LocalCardStyle
import com.mdcito.app.ui.components.LocalCardTransparency
import com.mdcito.app.ui.components.MdcitoActionItem
import com.mdcito.app.ui.components.MdcitoBottomSheet
import com.mdcito.app.ui.components.MdcitoCard
import com.mdcito.app.ui.components.MdcitoCardDefaults
import com.mdcito.app.ui.components.MdcitoCenterModal
import com.mdcito.app.ui.components.MdcitoConfirmDialog
import com.mdcito.app.ui.components.MdcitoRenameDialog
import com.mdcito.app.ui.components.MdcitoSearchField
import com.mdcito.app.ui.files.FileTypeFilter
import com.mdcito.app.ui.files.PinFilter
import com.mdcito.app.ui.files.SortOrder
import com.mdcito.app.ui.files.TimeFilter
import com.mdcito.app.util.HapticHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    onNavigateToEditor: (Long) -> Unit,
    onNavigateToVersionHistory: (Long) -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    LanguageHelper.LocalLocaleVersion.current
    val selectedTab by viewModel.selectedTab.collectAsState()
    val fileHistory by viewModel.fileHistory.collectAsState()
    val folderHistory by viewModel.folderHistory.collectAsState()
    val currentFolderId by viewModel.currentFolderId.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()
    val folderSubFolders by viewModel.folderSubFolders.collectAsState()
    val folderSubFiles by viewModel.folderSubFiles.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val isSelectMode by viewModel.isSelectMode.collectAsState()
    val fileNameMap by viewModel.fileNameMap.collectAsState()
    val fileEntityMap by viewModel.fileEntityMap.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showActionSheet by remember { mutableStateOf(false) }
    var selectedHistory by remember { mutableStateOf<HistoryEntity?>(null) }
    var selectedFolderFile by remember { mutableStateOf<FileEntity?>(null) }
    var showFolderFileActionSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showDetailModal by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showBatchTagDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showFolderDeleteConfirm by remember { mutableStateOf(false) }
    var showFolderRenameDialog by remember { mutableStateOf(false) }
    var showFolderDetailModal by remember { mutableStateOf(false) }
    var showFolderTagDialog by remember { mutableStateOf(false) }

    val hapticContext = LocalContext.current

    // 导入结果反馈
    LaunchedEffect(Unit) {
        viewModel.importEvent.collect { event ->
            when (event) {
                is HistoryViewModel.ImportEvent.Success -> {
                    Toast.makeText(
                        hapticContext,
                        hapticContext.getString(R.string.import_file_success),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                is HistoryViewModel.ImportEvent.Error -> {
                    Toast.makeText(
                        hapticContext,
                        event.message,
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    val openFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importFile(it)
            showImportDialog = false
        }
    }

    val openFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importFolder(it)
            showImportDialog = false
        }
    }

    // 删除结果反馈
    LaunchedEffect(Unit) {
        viewModel.deleteEvent.collect { event ->
            when (event) {
                is HistoryViewModel.DeleteEvent.Success -> {
                    Toast.makeText(
                        hapticContext,
                        hapticContext.resources.getQuantityString(
                            R.plurals.history_deleted, event.count, event.count
                        ),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                is HistoryViewModel.DeleteEvent.Error -> {
                    Toast.makeText(
                        hapticContext,
                        hapticContext.getString(R.string.history_delete_failed),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    val currentItems = if (selectedTab == HistoryTab.FILES) fileHistory else folderHistory

    // 文件夹视图：顶层显示历史文件夹列表，进入后显示子内容
    val isInsideFolder = currentFolderId != null
    val folderContentItems: List<FileEntity> = if (isInsideFolder) {
        if (selectedTab == HistoryTab.FILES) folderSubFiles else folderSubFolders
    } else {
        emptyList()
    }

    val hasActiveFilter = filterState.timeFilter != TimeFilter.ALL ||
            filterState.fileTypeFilter != FileTypeFilter.ALL ||
            filterState.pinFilter != PinFilter.ALL ||
            filterState.selectedTags.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                if (isInsideFolder) {
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
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column {
                    Text(
                        text = stringResource(R.string.history),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.W600,
                    )
                    Text(
                        text = stringResource(R.string.n_items, if (isInsideFolder) folderContentItems.size else currentItems.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (isSelectMode) {
                    TextButton(onClick = { viewModel.clearSelection() }) { Text(stringResource(R.string.cancel)) }
                } else {
                    HistoryViewModeToggle(
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

            if (isInsideFolder && breadcrumbs.size > 1) {
                HistoryBreadcrumbBar(
                    breadcrumbs = breadcrumbs,
                    onNavigate = { viewModel.navigateToBreadcrumb(it) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            MdcitoSearchField(
                query = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                placeholder = stringResource(R.string.search_history),
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
                    if (filterState.fileTypeFilter != FileTypeFilter.ALL) {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.updateFilter(filterState.copy(fileTypeFilter = FileTypeFilter.ALL)) },
                            label = { Text(stringResource(filterState.fileTypeFilter.labelRes), fontSize = 12.sp) },
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

            if (isInsideFolder) {
                // 文件夹内部视图：根据 tab 显示子文件夹或文件
                if (folderContentItems.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = if (selectedTab == HistoryTab.FILES)
                                Icons.Outlined.Description else Icons.Outlined.CreateNewFolder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTab == HistoryTab.FILES)
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
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.weight(1f),
                    ) {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            top = 4.dp,
                            end = 20.dp,
                            bottom = if (isSelectMode) 80.dp else 100.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(folderContentItems.size, key = { folderContentItems[it].id }) { index ->
                            val item = folderContentItems[index]
                            SwipeableFolderContentCard(
                                fileEntity = item,
                                isSelectMode = isSelectMode,
                                isSelected = selectedIds.contains(item.id),
                                onSelect = { viewModel.toggleSelect(item.id) },
                                onClick = {
                                    if (isSelectMode) {
                                        viewModel.toggleSelect(item.id)
                                    } else {
                                        if (item.type == "folder") {
                                            viewModel.navigateToFolder(item.id, item.name)
                                        } else {
                                            onNavigateToEditor(item.id)
                                        }
                                    }
                                },
                                onDelete = {
                                    selectedFolderFile = item
                                    showFolderDeleteConfirm = true
                                },
                                onMore = {
                                    selectedFolderFile = item
                                    showFolderFileActionSheet = true
                                },
                            )
                        }
                    }
                    }
                }
            } else if (currentItems.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (selectedTab == HistoryTab.FILES)
                            Icons.Outlined.Description else Icons.Outlined.CreateNewFolder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (selectedTab == HistoryTab.FILES)
                            stringResource(R.string.empty_file_history)
                        else
                            stringResource(R.string.empty_folder_history),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.weight(1f),
                ) {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        top = 4.dp,
                        end = 20.dp,
                        bottom = if (isSelectMode) 80.dp else 100.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(currentItems.size, key = { currentItems[it].id }) { index ->
                        val history = currentItems[index]
                        val fileEntity = fileEntityMap[history.fileId]
                        HistoryCard(
                            history = history,
                            fileEntity = fileEntity,
                            fileName = fileNameMap[history.fileId] ?: stringResource(R.string.file_id_format, history.fileId),
                            isSelectMode = isSelectMode,
                            isSelected = selectedIds.contains(history.id),
                            onSelect = { viewModel.toggleSelect(history.id) },
                            onClick = {
                                if (isSelectMode) {
                                    viewModel.toggleSelect(history.id)
                                } else {
                                    if (fileEntity?.type == "folder") {
                                        viewModel.navigateToFolder(fileEntity.id, fileEntity.name)
                                    } else {
                                        onNavigateToEditor(history.fileId)
                                    }
                                }
                            },
                            onLongClick = {
                                if (!isSelectMode) {
                                    selectedHistory = history
                                    showActionSheet = true
                                }
                            },
                            onDelete = {
                                selectedHistory = history
                                showDeleteConfirm = true
                            },
                            onMore = {
                                selectedHistory = history
                                showActionSheet = true
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
                                    showImportDialog = true
                                }
                                isDragging = false
                            }
                        },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.import_files),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        MdcitoCenterModal(
            onDismissRequest = { showImportDialog = false },
            visible = showImportDialog,
            title = stringResource(R.string.import_files),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        openFileLauncher.launch(arrayOf("text/markdown", "text/plain", "text/*"))
                    },
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.import_file),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.W600,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = if (isInsideFolder) stringResource(R.string.import_file_to_current_desc)
                            else stringResource(R.string.import_files_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        openFolderLauncher.launch(null)
                    },
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.import_folder),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.W600,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Text(
                            text = if (isInsideFolder) stringResource(R.string.import_folder_to_current_desc)
                            else stringResource(R.string.import_folder_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { showImportDialog = false }) { Text(stringResource(R.string.cancel)) }
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
                        selectedIndex = FileTypeFilter.entries.indexOf(filterState.fileTypeFilter),
                        onSelect = { viewModel.updateFilter(filterState.copy(fileTypeFilter = FileTypeFilter.entries[it])) },
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
                    showFilterSheet = false
                }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { showFilterSheet = false }) { Text(stringResource(R.string.done)) }
            }
        }

        MdcitoBottomSheet(
            onDismissRequest = { showActionSheet = false },
            visible = showActionSheet,
            title = fileNameMap[selectedHistory?.fileId ?: 0] ?: stringResource(R.string.action),
        ) {
            selectedHistory?.let { history ->
                val isFolder = fileEntityMap[history.fileId]?.type == "folder"
                if (isFolder) {
                    MdcitoActionItem(Icons.Outlined.CreateNewFolder, stringResource(R.string.open_folder)) {
                        showActionSheet = false
                        val entity = fileEntityMap[history.fileId]
                        if (entity != null) {
                            viewModel.navigateToFolder(entity.id, entity.name)
                        }
                    }
                } else {
                    MdcitoActionItem(Icons.Outlined.Description, stringResource(R.string.open_file)) {
                        showActionSheet = false
                        onNavigateToEditor(history.fileId)
                    }
                }
                MdcitoActionItem(Icons.Outlined.Share, stringResource(R.string.share)) {
                    showActionSheet = false
                    viewModel.shareFile(history.fileId, hapticContext)
                }
                MdcitoActionItem(Icons.Outlined.Edit, stringResource(R.string.rename)) {
                    showActionSheet = false
                    showRenameDialog = true
                }
                MdcitoActionItem(Icons.Outlined.Bookmark, stringResource(R.string.tag)) {
                    showActionSheet = false
                    showTagDialog = true
                }
                MdcitoActionItem(Icons.Outlined.History, stringResource(R.string.view_version_history)) {
                    showActionSheet = false
                    onNavigateToVersionHistory(history.fileId)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                MdcitoActionItem(Icons.Outlined.Info, stringResource(R.string.details)) {
                    showActionSheet = false
                    showDetailModal = true
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                MdcitoActionItem(Icons.Outlined.Delete, stringResource(R.string.delete_record), isDestructive = true) {
                    showActionSheet = false
                    showDeleteConfirm = true
                }
            }
        }

        selectedHistory?.let { history ->
            MdcitoConfirmDialog(
                visible = showDeleteConfirm && !isSelectMode,
                title = stringResource(R.string.delete_history_record),
                message = stringResource(R.string.confirm_delete_history, fileNameMap[history.fileId] ?: stringResource(R.string.unknown_file)),
                confirmText = stringResource(R.string.delete),
                isDangerous = true,
                onConfirm = {
                    viewModel.deleteHistory(history)
                    showDeleteConfirm = false
                    selectedHistory = null
                    HapticHelper.delete(hapticContext)
                },
                onDismiss = { showDeleteConfirm = false },
            )

            MdcitoRenameDialog(
                visible = showRenameDialog,
                currentName = fileNameMap[history.fileId] ?: "",
                onRename = { newName ->
                    viewModel.renameFile(history.fileId, newName)
                    showRenameDialog = false
                },
                onDismiss = { showRenameDialog = false },
            )

            HistoryDetailModal(
                visible = showDetailModal,
                history = history,
                fileName = fileNameMap[history.fileId] ?: stringResource(R.string.unknown_file),
                fileEntity = fileEntityMap[history.fileId],
                onDismiss = { showDetailModal = false },
            )

            val currentFileEntity = fileEntityMap[history.fileId]
            if (currentFileEntity != null) {
                HistoryTagDialog(
                    visible = showTagDialog,
                    fileEntity = currentFileEntity,
                    allTags = allTags,
                    onAddTag = { tag -> viewModel.addTagToFile(history.fileId, tag) },
                    onRemoveTag = { tag -> viewModel.removeTagFromFile(history.fileId, tag) },
                    onDismiss = { showTagDialog = false },
                )
            }
        }

        if (isSelectMode && selectedIds.isNotEmpty()) {
            MdcitoConfirmDialog(
                visible = showBatchDeleteConfirm,
                title = stringResource(R.string.batch_delete),
                message = stringResource(R.string.confirm_batch_delete_history, selectedIds.size),
                confirmText = stringResource(R.string.delete),
                isDangerous = true,
                onConfirm = {
                    val fileIdMap = currentItems.associate { it.id to it.fileId }
                    viewModel.deleteHistoryByIds(selectedIds.toList(), fileIdMap)
                    showBatchDeleteConfirm = false
                },
                onDismiss = { showBatchDeleteConfirm = false },
            )
        }

        MdcitoConfirmDialog(
            visible = showDeleteAllConfirm,
            title = stringResource(R.string.delete_all),
            message = stringResource(R.string.confirm_delete_all_history),
            confirmText = stringResource(R.string.delete_all),
            isDangerous = true,
            onConfirm = {
                viewModel.deleteAll()
                showDeleteAllConfirm = false
            },
            onDismiss = { showDeleteAllConfirm = false },
        )

        HistoryBatchTagDialog(
            visible = showBatchTagDialog,
            allTags = allTags,
            onAddTag = { tag ->
                val fileIdMap = currentItems.associate { it.id to it.fileId }
                viewModel.addTagToSelectedFiles(tag, fileIdMap)
            },
            onDismiss = { showBatchTagDialog = false },
        )

        // 文件夹内部文件的操作面板
        MdcitoBottomSheet(
            onDismissRequest = { showFolderFileActionSheet = false },
            visible = showFolderFileActionSheet,
            title = selectedFolderFile?.name ?: stringResource(R.string.file_operations),
        ) {
            selectedFolderFile?.let { file ->
                val isFolder = file.type == "folder"
                MdcitoActionItem(Icons.Outlined.Share, stringResource(R.string.share)) {
                    showFolderFileActionSheet = false
                    viewModel.shareFile(file.id, hapticContext)
                }
                MdcitoActionItem(Icons.Outlined.Edit, stringResource(R.string.rename)) {
                    showFolderFileActionSheet = false
                    showFolderRenameDialog = true
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                MdcitoActionItem(Icons.Outlined.ContentCopy, stringResource(R.string.copy)) {
                    viewModel.copyFile(file)
                    showFolderFileActionSheet = false
                }
                MdcitoActionItem(Icons.Outlined.Bookmark, stringResource(R.string.tag)) {
                    showFolderFileActionSheet = false
                    showFolderTagDialog = true
                }
                MdcitoActionItem(Icons.Outlined.Info, stringResource(R.string.details)) {
                    showFolderFileActionSheet = false
                    showFolderDetailModal = true
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                MdcitoActionItem(Icons.Outlined.Delete, stringResource(R.string.delete), isDestructive = true) {
                    showFolderFileActionSheet = false
                    showFolderDeleteConfirm = true
                }
            }
        }

        selectedFolderFile?.let { file ->
            MdcitoRenameDialog(
                visible = showFolderRenameDialog,
                currentName = file.name,
                onRename = { newName ->
                    viewModel.renameFile(file.id, newName)
                    showFolderRenameDialog = false
                },
                onDismiss = { showFolderRenameDialog = false },
            )

            MdcitoConfirmDialog(
                visible = showFolderDeleteConfirm,
                title = if (file.type == "file") stringResource(R.string.delete_file) else stringResource(R.string.delete_folder),
                message = stringResource(R.string.confirm_delete_item, file.name),
                confirmText = stringResource(R.string.delete),
                isDangerous = true,
                onConfirm = {
                    viewModel.deleteFileFromFolder(file)
                    showFolderDeleteConfirm = false
                    selectedFolderFile = null
                    HapticHelper.delete(hapticContext)
                },
                onDismiss = { showFolderDeleteConfirm = false },
            )

            FolderFileDetailModal(
                visible = showFolderDetailModal,
                file = file,
                onDismiss = { showFolderDetailModal = false },
            )

            HistoryTagDialog(
                visible = showFolderTagDialog,
                fileEntity = file,
                allTags = allTags,
                onAddTag = { tag -> viewModel.addTagToFile(file.id, tag) },
                onRemoveTag = { tag -> viewModel.removeTagFromFile(file.id, tag) },
                onDismiss = { showFolderTagDialog = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryCard(
    history: HistoryEntity,
    fileEntity: FileEntity?,
    fileName: String,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val actionWidthPx = with(density) { 80.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()

    val offsetX = remember { androidx.compose.animation.core.Animatable(0f) }

    val draggableState = rememberDraggableState { delta ->
        coroutineScope.launch {
            offsetX.snapTo((offsetX.value + delta).coerceIn(-actionWidthPx, 0f))
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
            HistoryCardContent(
                history = history,
                fileEntity = fileEntity,
                fileName = fileName,
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
                    if (currentOffset < -threshold || velocity < -velocityThreshold) {
                        onDelete()
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
                    if (currentOffset < 0f) MaterialTheme.colorScheme.error.copy(alpha = 0.12f * revealProgress)
                    else Color.Transparent
                ),
            horizontalArrangement = Arrangement.End,
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
            HistoryCardContent(
                history = history,
                fileEntity = fileEntity,
                fileName = fileName,
                isSelectMode = false,
                isSelected = isSelected,
                onSelect = onSelect,
                onMore = onMore,
            )
        }
    }
}

@Composable
private fun HistoryCardContent(
    history: HistoryEntity,
    fileEntity: FileEntity?,
    fileName: String,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onMore: () -> Unit,
) {
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(MdcitoCardDefaults.ContentPadding)
            .then(
                if (isSelectMode) Modifier else Modifier
            ),
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
                containerColor = if (fileEntity?.type == "file")
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.tertiaryContainer,
            ),
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = if (fileEntity?.type == "folder") Icons.Outlined.CreateNewFolder
                else Icons.Outlined.Description,
                contentDescription = if (fileEntity?.type == "folder") stringResource(R.string.filter_folders) else stringResource(R.string.file_label),
                tint = if (fileEntity?.type == "folder")
                    MaterialTheme.colorScheme.tertiary
                else
                    MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(48.dp)
                    .padding(10.dp),
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                maxLines = 1,
            )
            if (fileEntity?.type == "folder") {
                Text(
                    text = fileEntity.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MdcitoCardDefaults.glassSubtleContentColor(),
                    maxLines = 1,
                )
            } else {
                Text(
                    text = "${stringResource(R.string.last_access)} ${formatDateTime(history.accessedAt, context)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MdcitoCardDefaults.glassSubtleContentColor(),
                    maxLines = 1,
                )
            }
            val tags = fileEntity?.let { HistoryViewModel.parseTags(it.tags) } ?: emptyList()
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

        if (!isSelectMode) {
            IconButton(
                onClick = onMore,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DragHandle,
                    contentDescription = stringResource(R.string.more_actions),
                    tint = MdcitoCardDefaults.glassFaintContentColor(),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun HistoryDetailModal(
    visible: Boolean,
    history: HistoryEntity,
    fileName: String,
    fileEntity: FileEntity?,
    onDismiss: () -> Unit,
) {
    com.mdcito.app.ui.components.MdcitoCenterModal(
        onDismissRequest = onDismiss,
        visible = visible,
        title = stringResource(R.string.history_details),
    ) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        DetailRow(stringResource(R.string.file_name), fileName)
        DetailRow(stringResource(R.string.type), if (fileEntity?.type == "file") stringResource(R.string.file_label) else stringResource(R.string.filter_folders))
        DetailRow(stringResource(R.string.last_access), sdf.format(Date(history.accessedAt)))
        DetailRow(stringResource(R.string.action), when (history.action) {
            "open" -> stringResource(R.string.open)
            "edit" -> stringResource(R.string.edit)
            "create" -> stringResource(R.string.create)
            else -> history.action
        })
        if (fileEntity != null) {
            DetailRow(stringResource(R.string.file_size), formatFileSize(fileEntity.size))
            DetailRow(stringResource(R.string.created_at), sdf.format(Date(fileEntity.createdAt)))
            DetailRow(stringResource(R.string.modified_at), sdf.format(Date(fileEntity.updatedAt)))
            DetailRow(stringResource(R.string.pin), if (fileEntity.isPinned) stringResource(R.string.yes) else stringResource(R.string.no))
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

@Composable
private fun FilterOptionRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEachIndexed { index, label ->
            FilterChip(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                label = { Text(label, fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                modifier = Modifier.weight(1f),
            )
        }
    }
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

@Composable
private fun HistoryViewModeToggle(
    selectedTab: HistoryTab,
    onSelectTab: (HistoryTab) -> Unit,
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
            HistoryTab.FILES to R.string.file_label,
            HistoryTab.FOLDERS to R.string.filter_folders,
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

private fun formatDateTime(timestamp: Long, context: Context): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> context.getString(R.string.just_now)
        diff < 3_600_000 -> context.getString(R.string.minutes_ago_short, diff / 60_000)
        diff < 86_400_000 && sameDay(timestamp, now) -> {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            "${context.getString(R.string.today)} ${sdf.format(Date(timestamp))}"
        }
        diff < 172_800_000 -> {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            "${context.getString(R.string.yesterday)} ${sdf.format(Date(timestamp))}"
        }
        diff < 604_800_000 -> {
            val sdf = SimpleDateFormat("EEEE HH:mm", Locale.CHINESE)
            sdf.format(Date(timestamp))
        }
        else -> {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

private fun sameDay(t1: Long, t2: Long): Boolean {
    val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = t1 }
    val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = t2 }
    return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
            cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryTagDialog(
    visible: Boolean,
    fileEntity: FileEntity,
    allTags: Set<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newTag by remember { mutableStateOf("") }
    val fileTags = HistoryViewModel.parseTags(fileEntity.tags)

    com.mdcito.app.ui.components.MdcitoCenterModal(
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

        com.mdcito.app.ui.components.MdcitoTextField(
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryBatchTagDialog(
    visible: Boolean,
    allTags: Set<String>,
    onAddTag: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newTag by remember { mutableStateOf("") }

    com.mdcito.app.ui.components.MdcitoCenterModal(
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

        com.mdcito.app.ui.components.MdcitoTextField(
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
private fun HistoryBreadcrumbBar(
    breadcrumbs: List<HistoryBreadcrumbItem>,
    onNavigate: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier,
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
                        imageVector = Icons.Outlined.ChevronRight,
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
private fun SwipeableFolderContentCard(
    fileEntity: FileEntity,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit,
) {
    val tags = HistoryViewModel.parseTags(fileEntity.tags)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val actionWidthPx = with(density) { 80.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()

    val offsetX = remember { androidx.compose.animation.core.Animatable(0f) }

    val draggableState = rememberDraggableState { delta ->
        coroutineScope.launch {
            offsetX.snapTo((offsetX.value + delta).coerceIn(-actionWidthPx, 0f))
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
            FolderContentCardContent(
                fileEntity = fileEntity,
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
                    if (currentOffset < -threshold || velocity < -velocityThreshold) {
                        onDelete()
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
                    if (currentOffset < 0f) MaterialTheme.colorScheme.error.copy(alpha = 0.12f * revealProgress)
                    else Color.Transparent
                ),
            horizontalArrangement = Arrangement.End,
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
            FolderContentCardContent(
                fileEntity = fileEntity,
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
private fun FolderContentCardContent(
    fileEntity: FileEntity,
    tags: List<String>,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onMore: () -> Unit,
) {
    val isFolder = fileEntity.type == "folder"
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
                containerColor = if (isFolder)
                    MaterialTheme.colorScheme.tertiaryContainer
                else
                    MaterialTheme.colorScheme.primaryContainer,
            ),
            modifier = Modifier.size(48.dp),
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
                    .size(48.dp)
                    .padding(10.dp),
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileEntity.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                maxLines = 1,
            )
            Text(
                text = if (isFolder) fileEntity.path
                else "${formatFileSize(fileEntity.size)} · ${formatDate(fileEntity.updatedAt)}",
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

        if (!isSelectMode) {
            IconButton(
                onClick = onMore,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DragHandle,
                    contentDescription = stringResource(R.string.more_actions),
                    tint = MdcitoCardDefaults.glassFaintContentColor(),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun FolderFileDetailModal(
    visible: Boolean,
    file: FileEntity,
    onDismiss: () -> Unit,
) {
    MdcitoCenterModal(
        onDismissRequest = onDismiss,
        visible = visible,
        title = stringResource(R.string.file_details),
    ) {
        val tags = HistoryViewModel.parseTags(file.tags)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        DetailRow(stringResource(R.string.name), file.name)
        DetailRow(stringResource(R.string.type), if (file.type == "file") stringResource(R.string.file_label) else stringResource(R.string.filter_folders))
        DetailRow(stringResource(R.string.size), formatFileSize(file.size))
        DetailRow(stringResource(R.string.path), file.path)
        DetailRow(stringResource(R.string.created_at), sdf.format(Date(file.createdAt)))
        DetailRow(stringResource(R.string.modified_at), sdf.format(Date(file.updatedAt)))
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

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
