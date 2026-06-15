package com.mdcito.app.ui.editor

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.mdcito.app.markdown.MarkdownDialect
import com.mdcito.app.markdown.MarkdownRenderer
import kotlin.math.roundToInt
import kotlin.math.sqrt
import com.mdcito.app.R
import com.mdcito.app.data.locale.LanguageHelper
import com.mdcito.app.ui.components.MdcitoActionItem
import com.mdcito.app.ui.components.MdcitoBottomSheet
import com.mdcito.app.ui.components.MdcitoCenterModal
import com.mdcito.app.ui.components.MdcitoConfirmDialog
import com.mdcito.app.ui.components.BackgroundImage
import com.mdcito.app.ui.components.EditorSkeletonScreen
import com.mdcito.app.ui.settings.SettingsViewModel
import com.mdcito.app.ui.theme.LocalIsDarkTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun EditorScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVersionHistory: (Long, String) -> Unit = { _, _ -> },
    onNavigateToSettings: () -> Unit = {},
    viewModel: EditorViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    LanguageHelper.LocalLocaleVersion.current

    // 读取编辑器背景设置
    val editorBackgroundImageUri by settingsViewModel.editorBackgroundImageUri.collectAsState()
    val editorBackgroundBlur by settingsViewModel.editorBackgroundBlur.collectAsState()
    val editorBackgroundBlurIntensity by settingsViewModel.editorBackgroundBlurIntensity.collectAsState()
    val editorBackgroundBrightness by settingsViewModel.editorBackgroundBrightness.collectAsState()

    val fileName by viewModel.fileName.collectAsState()
    val content by viewModel.content.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val editorMode by viewModel.editorMode.collectAsState()
    val isModified by viewModel.isModified.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val showLineNumbers by viewModel.showLineNumbers.collectAsState()
    val cursorInfo by viewModel.cursorInfo.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val editorFont by viewModel.editorFont.collectAsState()
    val lastSavedAt by viewModel.lastSavedAt.collectAsState()
    val autoSave by viewModel.autoSave.collectAsState()
    val lineHeight by viewModel.lineHeight.collectAsState()
    val letterSpacing by viewModel.letterSpacing.collectAsState()
    val saveConfirmState by viewModel.saveConfirmState.collectAsState()
    val actionFeedback by viewModel.actionFeedback.collectAsState()
    val highlightedLine by viewModel.highlightedLine.collectAsState()
    val charCount by viewModel.charCount.collectAsState()
    val desiredCursor by viewModel.desiredCursor.collectAsState()
    val desiredSelectionEnd by viewModel.desiredSelectionEnd.collectAsState()
    val scrollToLine by viewModel.scrollToLine.collectAsState()
    val scrollToAnchor by viewModel.scrollToAnchor.collectAsState()
    val canUndo by viewModel::canUndo
    val canRedo by viewModel::canRedo
    val saveOnExitPrompt by settingsViewModel.saveOnExitPrompt.collectAsState()
    val highlightCurrentLine by viewModel.highlightCurrentLine.collectAsState()
    val highlightLineColor by viewModel.highlightLineColor.collectAsState()
    val autoIndent by viewModel.autoIndent.collectAsState()
    val indentType by viewModel.indentType.collectAsState()
    val indentSize by viewModel.indentSize.collectAsState()
    val bracketMatching by viewModel.bracketMatching.collectAsState()
    val spellCheck by viewModel.spellCheck.collectAsState()
    val syncScroll by viewModel.syncScroll.collectAsState()

    // 监听版本快照恢复事件
    LaunchedEffect(Unit) {
        viewModel.restoreEvent.collect { snapshotContent ->
            viewModel.restoreFromSnapshot(snapshotContent)
        }
    }

    var showMoreMenu by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showTocSidebar by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showUnsavedConfirm by remember { mutableStateOf(false) }

    // 系统返回键也触发退出保存提示
    BackHandler(enabled = isModified && saveOnExitPrompt) {
        showUnsavedConfirm = true
    }
    var showDocInfo by remember { mutableStateOf(false) }
    var showTableDialog by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }
    var pendingExportFormat by remember { mutableStateOf<ExportFormat?>(null) }
    var isImmersive by remember { mutableStateOf(false) }
    var editorSelection by remember { mutableStateOf(TextRange(0)) }
    var isPdfExporting by remember { mutableStateOf(false) }

    val isDark = LocalIsDarkTheme.current
    val codeFont by viewModel.codeFont.collectAsState()
    val codeFontFace = remember(codeFont) { viewModel.fontService.getCodeFontCss(codeFont) }

    // PDF 导出所需的预计算值（Compose 上下文中计算，回调中使用）
    val currentDialect by viewModel.markdownDialect.collectAsState()
    val pdfDialect = remember(currentDialect) { MarkdownDialect.fromKey(currentDialect) }
    val pdfBgCss = colorToCssHex(MaterialTheme.colorScheme.background)
    val pdfSurfaceCss = colorToCssHex(MaterialTheme.colorScheme.surfaceVariant)
    val pdfTextCss = colorToCssHex(MaterialTheme.colorScheme.onBackground)
    val pdfOnTextCss = colorToCssHex(MaterialTheme.colorScheme.onSurfaceVariant)

    val context = LocalContext.current

    DisposableEffect(isImmersive) {
        val window = (context as? androidx.appcompat.app.AppCompatActivity)?.window
        if (window != null) {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            if (isImmersive) {
                controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            }
        }
        onDispose {
            val w = (context as? androidx.appcompat.app.AppCompatActivity)?.window
            if (w != null) {
                val c = WindowInsetsControllerCompat(w, w.decorView)
                c.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            }
        }
    }

    val headings = remember(content) {
        MarkdownRenderer.extractHeadings(content)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        uri?.let {
            val format = pendingExportFormat ?: ExportFormat.MARKDOWN
            try {
                if (format == ExportFormat.DOCX) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        viewModel.exportDocx(os)
                        os.flush()
                    }
                    Toast.makeText(context, context.getString(R.string.export_success), Toast.LENGTH_SHORT).show()
                } else {
                    val exportContent = viewModel.getExportContent(format)
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(exportContent.toByteArray(Charsets.UTF_8))
                        os.flush()
                    }
                    Toast.makeText(context, context.getString(R.string.export_success), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.export_failed, e.message), Toast.LENGTH_SHORT).show()
            }
            pendingExportFormat = null
        }
    }

    // 本地图片选择器（优先使用 Photo Picker，Android 13+ 无需权限）
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val desc = context.getString(R.string.image_desc_default)
            viewModel.insertLocalImage(it, desc, editorSelection.start, editorSelection.end)
        }
    }

    // 图床上传图片选择器
    val pickImageForHostLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val desc = context.getString(R.string.image_desc_default)
            viewModel.uploadImageToHost(it, desc, editorSelection.start, editorSelection.end)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 主题背景色层（确保无背景图时状态栏区域也有正确颜色）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        // 编辑器背景图片（在背景色之上）
        BackgroundImage(
            imageUri = editorBackgroundImageUri,
            blurEnabled = editorBackgroundBlur,
            blurIntensity = editorBackgroundBlurIntensity,
            brightness = editorBackgroundBrightness,
        )

        Column(modifier = Modifier.fillMaxSize()) {
            if (!isImmersive && !isLoading) {
                val onBack by rememberUpdatedState {
                    if (isModified && saveOnExitPrompt) showUnsavedConfirm = true
                    else onNavigateBack()
                }
                val onSave by rememberUpdatedState { viewModel.save() }
                val onToc by rememberUpdatedState { showTocSidebar = true }
                val onMore by rememberUpdatedState { showMoreMenu = true }
                EditorHeader(
                    fileName = fileName,
                    saveState = saveState,
                    isModified = isModified,
                    autoSave = autoSave,
                    lastSavedAt = lastSavedAt,
                    charCount = charCount,
                    saveConfirmState = saveConfirmState,
                    onBack = onBack,
                    onSave = onSave,
                    onToc = onToc,
                    onMore = onMore,
                )
            }

            if (!isImmersive && !isLoading) {
                if (saveState == SaveState.SAVING) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(
                        if (isImmersive) {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures {
                                    isImmersive = false
                                }
                            }
                        } else Modifier
                    ),
            ) {
                // ── 加载骨架屏 ──
                androidx.compose.animation.AnimatedVisibility(
                    visible = isLoading,
                    enter = fadeIn(),
                    exit = fadeOut(animationSpec = tween(300)),
                ) {
                    EditorSkeletonScreen(modifier = Modifier.fillMaxSize())
                }

                // ── 编辑器内容 ──
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isLoading,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(),
                ) {
                // ── 同步滚动：存储编辑器/预览滚动比例 ──
                var editorScrollRatio by remember { mutableFloatStateOf(0f) }
                // 预览→编辑器方向：记录预览侧的滚动比例
                var previewScrollRatio by remember { mutableFloatStateOf(0f) }

                if (editorMode == EditorMode.PLAIN) {
                    MarkdownCodeEditor(
                        content = content,
                        onContentChange = { viewModel.updateContent(it) },
                        onSelectionChange = { start, end ->
                            editorSelection = TextRange(start, end)
                            viewModel.updateCursorInfo(start, end)
                        },
                        desiredCursor = desiredCursor,
                        desiredSelectionEnd = desiredSelectionEnd,
                        onDesiredCursorConsumed = { viewModel.consumeDesiredCursor() },
                        onDesiredSelectionEndConsumed = { viewModel.consumeDesiredSelectionEnd() },
                        showLineNumbers = showLineNumbers,
                        highlightedLine = highlightedLine,
                        highlightCurrentLine = highlightCurrentLine,
                        highlightLineColor = highlightLineColor,
                        autoIndent = autoIndent,
                        indentType = indentType,
                        indentSize = indentSize,
                        bracketMatching = bracketMatching,
                        spellCheck = spellCheck,
                        syncScroll = syncScroll,
                        isPreviewMode = editorMode == EditorMode.RENDER,
                        fontSize = fontSize,
                        editorFontFamily = remember(editorFont) { viewModel.fontService.getComposeFontFamily(editorFont) },
                        lineHeight = lineHeight,
                        letterSpacing = letterSpacing,
                        viewModel = viewModel,
                        onScrollPositionChanged = { ratio -> editorScrollRatio = ratio },
                        scrollToLineIndex = scrollToLine,
                        onScrollToLineConsumed = { viewModel.consumeScrollToLine() },
                        // 预览→编辑器同步：传入预览侧记录的滚动比例
                        previewScrollRatio = if (syncScroll) previewScrollRatio else 0f,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MarkdownPreview(
                            content = content,
                            modifier = Modifier.fillMaxSize(),
                            codeFontFace = codeFontFace,
                            dialect = pdfDialect,
                            syncScrollEnabled = syncScroll,
                            editorScrollPosition = if (syncScroll) editorScrollRatio else 0f,
                            scrollToAnchor = scrollToAnchor,
                            onScrollToAnchorConsumed = { viewModel.consumeScrollToAnchor() },
                            // 预览→编辑器方向同步回调
                            onPreviewScrollPositionChanged = { ratio ->
                                previewScrollRatio = ratio
                            },
                            hasBackgroundImage = !editorBackgroundImageUri.isNullOrEmpty(),
                        )
                    }
                }

                if (isImmersive) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                    ) {
                        IconButton(
                            onClick = { isImmersive = false },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FullscreenExit,
                                contentDescription = stringResource(R.string.exit_immersive),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                } // end AnimatedVisibility for editor content
            }

            AnimatedVisibility(
                visible = editorMode == EditorMode.PLAIN && !isLoading,
                enter = androidx.compose.animation.expandVertically(
                    animationSpec = tween(durationMillis = 200),
                    expandFrom = Alignment.Bottom,
                ) + fadeIn(animationSpec = tween(durationMillis = 200)),
                exit = androidx.compose.animation.shrinkVertically(
                    animationSpec = tween(durationMillis = 200),
                    shrinkTowards = Alignment.Bottom,
                ) + fadeOut(animationSpec = tween(durationMillis = 200)),
            ) {
                EditorToolbar(
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() },
                    canUndo = canUndo,
                    canRedo = canRedo,
                    onHeading = { level -> viewModel.insertLinePrefix("${"#".repeat(level)} ", editorSelection.start, editorSelection.end) },
                    onItalic = { viewModel.insertFormatting("*", "*", editorSelection.start, editorSelection.end) },
                    onBold = { viewModel.insertFormatting("**", "**", editorSelection.start, editorSelection.end) },
                    onBoldItalic = { viewModel.insertFormatting("***", "***", editorSelection.start, editorSelection.end) },
                    onStrikethrough = { viewModel.insertFormatting("~~", "~~", editorSelection.start, editorSelection.end) },
                    onQuote = { viewModel.insertLinePrefix("> ", editorSelection.start, editorSelection.end) },
                    onUnorderedList = { viewModel.insertLinePrefix("- ", editorSelection.start, editorSelection.end) },
                    onOrderedList = { viewModel.insertLinePrefix("1. ", editorSelection.start, editorSelection.end) },
                    onTaskList = { viewModel.insertLinePrefix("- [ ] ", editorSelection.start, editorSelection.end) },
                    onInlineCode = { viewModel.insertFormatting("`", "`", editorSelection.start, editorSelection.end) },
                    onCodeBlock = { viewModel.insertCodeBlock(editorSelection.start, editorSelection.end) },
                    onLink = { viewModel.insertLink(editorSelection.start, editorSelection.end) },
                    onImage = { showImageDialog = true },
                    onTable = { showTableDialog = true },
                    onHorizontalRule = { viewModel.insertAtCursor("\n---\n", editorSelection.start, editorSelection.end) },
                    onInlineMath = { viewModel.insertFormatting("$", "$", editorSelection.start, editorSelection.end) },
                    onBlockMath = { viewModel.insertFormatting("$$", "$$", editorSelection.start, editorSelection.end) },
                    onSearch = { showSearchDialog = true },
                    modifier = Modifier.imePadding(),
                )
            }
        }

        val fabIcon = when (editorMode) {
            EditorMode.PLAIN -> Icons.Outlined.Visibility
            EditorMode.RENDER -> Icons.Outlined.Edit
        }
        val fabColor = when (editorMode) {
            EditorMode.PLAIN -> MaterialTheme.colorScheme.primary
            EditorMode.RENDER -> MaterialTheme.colorScheme.tertiary
        }
        DraggableFab(
            icon = fabIcon,
            color = fabColor,
            onClick = { viewModel.toggleEditorMode() },
        )

        if (actionFeedback != null && !isImmersive) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp),
            ) {
                Text(
                    text = actionFeedback!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        TableOfContentsSidebar(
            visible = showTocSidebar,
            headings = headings,
            onHeadingClick = { lineIndex ->
                viewModel.requestScrollToLine(lineIndex)
                viewModel.requestScrollToAnchor("heading-line-$lineIndex")
                viewModel.flashLineHighlight(lineIndex)
                showTocSidebar = false
            },
            onDismiss = { showTocSidebar = false },
            cursorLine = cursorInfo.line,
        )

        MdcitoBottomSheet(
            onDismissRequest = { showMoreMenu = false },
            visible = showMoreMenu,
            title = stringResource(R.string.more_options),
        ) {
            MdcitoActionItem(Icons.Outlined.Search, stringResource(R.string.search)) {
                showMoreMenu = false
                showSearchDialog = true
            }
            MdcitoActionItem(Icons.Outlined.Fullscreen, stringResource(R.string.immersive_mode)) {
                showMoreMenu = false
                isImmersive = true
            }
            MdcitoActionItem(Icons.Outlined.FileDownload, stringResource(R.string.export_file)) {
                showMoreMenu = false
                showExportSheet = true
            }
            MdcitoActionItem(Icons.Outlined.History, stringResource(R.string.view_history)) {
                showMoreMenu = false
                viewModel.fileId?.let { id ->
                    onNavigateToVersionHistory(id, fileName)
                }
            }
            MdcitoActionItem(Icons.Outlined.Info, stringResource(R.string.doc_info)) {
                showMoreMenu = false
                showDocInfo = true
            }
            MdcitoActionItem(Icons.Outlined.Share, stringResource(R.string.share)) {
                showMoreMenu = false
                try {
                    val sharedDir = java.io.File(context.cacheDir, "shared").apply { mkdirs() }
                    val tempFile = java.io.File(sharedDir, fileName.ifEmpty { "untitled.md" }).apply {
                        writeText(content)
                    }
                    val contentUri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        tempFile,
                    )
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        type = "*/*"
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, null))
                } catch (_: Exception) {}
            }
            MdcitoActionItem(Icons.Outlined.Settings, stringResource(R.string.settings)) {
                showMoreMenu = false
                onNavigateToSettings()
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            TextButton(
                onClick = { showMoreMenu = false },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.cancel))
            }
        }

        MdcitoBottomSheet(
            onDismissRequest = { showExportSheet = false },
            visible = showExportSheet,
            title = stringResource(R.string.export_file),
        ) {
            val baseName = fileName.ifEmpty { "untitled" }.let { name ->
                val dot = name.lastIndexOf('.')
                if (dot > 0) name.substring(0, dot) else name
            }
            MdcitoActionItem(Icons.Outlined.Edit, "Markdown (.md)") {
                showExportSheet = false
                pendingExportFormat = ExportFormat.MARKDOWN
                exportLauncher.launch("$baseName.md")
            }
            MdcitoActionItem(Icons.Outlined.Edit, stringResource(R.string.export_plain_text)) {
                showExportSheet = false
                pendingExportFormat = ExportFormat.PLAIN_TEXT
                exportLauncher.launch("$baseName.txt")
            }
            MdcitoActionItem(Icons.Outlined.Edit, "HTML (.html)") {
                showExportSheet = false
                pendingExportFormat = ExportFormat.HTML
                exportLauncher.launch("$baseName.html")
            }
            MdcitoActionItem(Icons.Outlined.Edit, "Word (.docx)") {
                showExportSheet = false
                pendingExportFormat = ExportFormat.DOCX
                exportLauncher.launch("$baseName.docx")
            }
            MdcitoActionItem(Icons.Outlined.PictureAsPdf, "PDF (.pdf)") {
                showExportSheet = false
                // PDF 导出使用 PrintManager，通过系统打印对话框保存为 PDF
                // 无需使用 ActivityResultContracts.CreateDocument
                isPdfExporting = true
                val html = MarkdownRenderer.renderToHtml(content, pdfDialect)
                val fullHtml = MarkdownRenderer.wrapHtml(
                    html,
                    isDark = isDark,
                    codeFontFace = codeFontFace,
                    backgroundColor = pdfBgCss,
                    surfaceColor = pdfSurfaceCss,
                    textColor = pdfTextCss,
                    onTextColor = pdfOnTextCss,
                    dialect = pdfDialect,
                )
                val jobName = fileName.ifEmpty { "untitled" }.let { name ->
                    val dot = name.lastIndexOf('.')
                    if (dot > 0) name.substring(0, dot) else name
                }
                viewModel.exportPdf(
                    fullHtml,
                    activityContext = context,
                    jobName = jobName,
                    onReady = {
                        // WebView 渲染完成，打印对话框已弹出，关闭进度提示
                        isPdfExporting = false
                    },
                    onComplete = {
                        Toast.makeText(context, context.getString(R.string.export_success), Toast.LENGTH_SHORT).show()
                    },
                    onError = { e ->
                        Toast.makeText(context, context.getString(R.string.export_failed, e.message), Toast.LENGTH_SHORT).show()
                    }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            TextButton(
                onClick = { showExportSheet = false },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.cancel))
            }
        }

        EditorSearchModal(
            visible = showSearchDialog,
            viewModel = viewModel,
            onDismiss = {
                if (viewModel.searchState.value.query.isNotBlank()) {
                    viewModel.addToSearchHistory(viewModel.searchState.value.query)
                }
                showSearchDialog = false
            },
        )

        // 三选项退出保存对话框：保存 / 不保存 / 取消
        if (showUnsavedConfirm) {
            MdcitoCenterModal(
                onDismissRequest = { showUnsavedConfirm = false },
                visible = true,
                title = stringResource(R.string.unsaved_changes_title),
            ) {
                Text(
                    text = stringResource(R.string.unsaved_changes_msg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = {
                        showUnsavedConfirm = false
                    }) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        showUnsavedConfirm = false
                        onNavigateBack()
                    }) {
                        Text(stringResource(R.string.dont_save), color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        viewModel.save()
                        showUnsavedConfirm = false
                        onNavigateBack()
                    }) {
                        Text(stringResource(R.string.save_and_exit), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        MdcitoCenterModal(
            onDismissRequest = { showDocInfo = false },
            visible = showDocInfo,
            title = stringResource(R.string.doc_info),
        ) {
            Column {
                DocInfoRow(stringResource(R.string.char_count), "$charCount")
                DocInfoRow(stringResource(R.string.word_count), "${viewModel.getWordCount()}")
                DocInfoRow(stringResource(R.string.line_count), "${viewModel.getLineCount()}")
                DocInfoRow(stringResource(R.string.paragraph_count), "${viewModel.getParagraphCount()}")
                val readingTime = viewModel.getReadingTimeMinutes()
                val readingTimeText = if (readingTime <= 1) {
                    stringResource(R.string.reading_time_less_than_1)
                } else {
                    stringResource(R.string.reading_time_value, readingTime)
                }
                DocInfoRow(stringResource(R.string.reading_time), readingTimeText)
            }
        }

        if (showTableDialog) {
            TableInsertDialog(
                onDismiss = { showTableDialog = false },
                onConfirm = { rows, cols ->
                    viewModel.insertTable(rows, cols, editorSelection.start, editorSelection.end)
                    showTableDialog = false
                },
            )
        }

        if (showImageDialog) {
            ImageInsertDialog(
                imageHostEnabled = viewModel.imageHostEnabled.collectAsState().value,
                isUploading = viewModel.isUploadingImage.collectAsState().value,
                uploadError = viewModel.uploadError.collectAsState().value,
                onDismiss = { showImageDialog = false },
                onConfirm = { url, description ->
                    viewModel.insertImage(url, description, editorSelection.start, editorSelection.end)
                    showImageDialog = false
                },
                onPickLocalImage = {
                    showImageDialog = false
                    if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
                        pickImageLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    } else {
                        pickImageLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                },
                onUploadToHost = {
                    showImageDialog = false
                    if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
                        pickImageForHostLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    } else {
                        pickImageForHostLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                },
                onClearUploadError = { viewModel.clearUploadError() },
            )
        }

        // PDF 导出进度对话框（WebView 渲染阶段显示，打印对话框弹出后自动关闭）
        if (isPdfExporting) {
            MdcitoCenterModal(
                onDismissRequest = {},
                visible = true,
                title = stringResource(R.string.exporting_pdf),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                    Text(
                        text = stringResource(R.string.exporting_pdf_msg),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorHeader(
    fileName: String,
    saveState: SaveState,
    isModified: Boolean,
    autoSave: Boolean,
    lastSavedAt: Long,
    charCount: Int,
    saveConfirmState: SaveConfirmState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onToc: () -> Unit,
    onMore: () -> Unit,
) {
    Surface(
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
            ) {
                Text(
                    text = fileName.ifEmpty { stringResource(R.string.unnamed_file) }.removeSuffix(".md").removeSuffix(".markdown"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.char_count_value, charCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    SaveStatusText(
                        saveState = saveState,
                        isModified = isModified,
                        autoSave = autoSave,
                        lastSavedAt = lastSavedAt,
                    )
                }
            }

            SaveIconButton(
                saveState = saveState,
                isModified = isModified,
                saveConfirmState = saveConfirmState,
                onSave = onSave,
            )

            IconButton(onClick = onToc) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.List,
                    contentDescription = stringResource(R.string.toc),
                    modifier = Modifier.size(20.dp),
                )
            }

            IconButton(onClick = onMore) {
                Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.more))
            }
        }
    }
}

@Composable
private fun SaveStatusText(
    saveState: SaveState,
    isModified: Boolean,
    autoSave: Boolean,
    lastSavedAt: Long,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lastSavedAt) {
        while (true) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(30_000)
        }
    }

    val text = when (saveState) {
        SaveState.SAVING -> stringResource(R.string.saving)
        SaveState.SAVED -> {
            if (lastSavedAt > 0) {
                val diff = now - lastSavedAt
                val saveInfo = when {
                    diff < 5000 -> stringResource(R.string.just_saved)
                    diff < 60000 -> stringResource(R.string.seconds_ago_saved, diff / 1000)
                    diff < 3600000 -> stringResource(R.string.minutes_ago_saved, diff / 60000)
                    diff < 86400000 -> stringResource(R.string.hours_ago_saved, diff / 3600000)
                    else -> stringResource(R.string.days_ago_saved, diff / 86400000)
                }
                saveInfo
            } else stringResource(R.string.saved)
        }
        SaveState.ERROR -> stringResource(R.string.save_failed)
        SaveState.IDLE -> if (isModified) stringResource(R.string.unsaved) else ""
    }
    val color = when (saveState) {
        SaveState.SAVING -> MaterialTheme.colorScheme.primary
        SaveState.SAVED -> MaterialTheme.colorScheme.primary
        SaveState.ERROR -> MaterialTheme.colorScheme.error
        SaveState.IDLE -> if (isModified) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        else Color.Transparent
    }
    if (text.isNotEmpty()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
    }
}

@Composable
private fun SaveIconButton(
    saveState: SaveState,
    isModified: Boolean,
    saveConfirmState: SaveConfirmState,
    onSave: () -> Unit,
) {
    val icon: ImageVector
    val tint: Color

    when (saveConfirmState) {
        SaveConfirmState.SAVED -> {
            icon = Icons.Outlined.Check
            tint = MaterialTheme.colorScheme.primary
        }
        SaveConfirmState.ERROR -> {
            icon = Icons.Outlined.ErrorOutline
            tint = MaterialTheme.colorScheme.error
        }
        SaveConfirmState.NONE -> {
            when (saveState) {
                SaveState.SAVING -> {
                    icon = Icons.Outlined.Save
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                }
                SaveState.SAVED -> {
                    icon = Icons.Outlined.Check
                    tint = MaterialTheme.colorScheme.primary
                }
                SaveState.ERROR -> {
                    icon = Icons.Outlined.ErrorOutline
                    tint = MaterialTheme.colorScheme.error
                }
                SaveState.IDLE -> {
                    icon = Icons.Outlined.Save
                    tint = if (isModified) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                }
            }
        }
    }

    IconButton(onClick = onSave) {
        if (saveState == SaveState.SAVING) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = tint,
            )
        } else {
            Icon(imageVector = icon, contentDescription = stringResource(R.string.save), tint = tint)
        }
    }
}

@Composable
private fun DraggableFab(
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var fabOffsetX by remember { mutableFloatStateOf(0f) }
    val bottomToolbarPx = with(LocalDensity.current) { 80.dp.toPx() }
    var fabOffsetY by remember { mutableFloatStateOf(-bottomToolbarPx) }
    var isDragging by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isDragging) 1.1f else 1f, label = "fabScale")
    val elevation by animateDpAsState(if (isDragging) 8.dp else 4.dp, label = "fabElevation")
    val fabSizePx = with(LocalDensity.current) { 56.dp.toPx() }
    val fabPaddingPx = with(LocalDensity.current) { 24.dp.toPx() }
    var containerWidth by remember { mutableFloatStateOf(0f) }
    var containerHeight by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                containerWidth = coordinates.size.width.toFloat()
                containerHeight = coordinates.size.height.toFloat()
            },
        contentAlignment = Alignment.BottomEnd,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = color,
            shadowElevation = elevation,
            modifier = Modifier
                .padding(24.dp)
                .offset { IntOffset(fabOffsetX.roundToInt(), fabOffsetY.roundToInt()) }
                .size(56.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
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
                                val distance = sqrt(dx * dx + dy * dy)

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
                            onClick()
                        }
                        isDragging = false
                    }
                },
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun DocInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.W600,
        )
    }
}

@Composable
private fun MarkdownCodeEditor(
    content: String,
    onContentChange: (String) -> Unit,
    onSelectionChange: (Int, Int) -> Unit,
    desiredCursor: Int?,
    desiredSelectionEnd: Int?,
    onDesiredCursorConsumed: () -> Unit,
    onDesiredSelectionEndConsumed: () -> Unit,
    showLineNumbers: Boolean,
    highlightedLine: Int?,
    highlightCurrentLine: Boolean,
    highlightLineColor: String,
    autoIndent: Boolean,
    indentType: String,
    indentSize: Int,
    bracketMatching: Boolean,
    spellCheck: Boolean,
    syncScroll: Boolean,
    isPreviewMode: Boolean,
    fontSize: Int,
    editorFontFamily: FontFamily,
    lineHeight: Float,
    letterSpacing: Float,
    viewModel: EditorViewModel,
    onScrollPositionChanged: (Float) -> Unit = {},
    scrollToLineIndex: Int = -1,
    onScrollToLineConsumed: () -> Unit = {},
    previewScrollRatio: Float = 0f,
    modifier: Modifier = Modifier,
) {
    // ── 拼写检查（异步 + 防抖，避免影响输入流畅度） ──
    var spellErrors by remember { mutableStateOf<List<SpellError>>(emptyList()) }
    val spellCheckerCtx = LocalContext.current
    val spellChecker = remember { SpellCheckerService(spellCheckerCtx) }

    LaunchedEffect(content, spellCheck) {
        if (spellCheck && content.isNotEmpty()) {
            // 防抖：内容变化后延迟 300ms 再检查，避免输入时卡顿
            delay(300)
            spellErrors = spellChecker.checkSpellingAsync(content)
        } else {
            spellErrors = emptyList()
        }
    }

    // ── 高亮当前行：根据光标位置实时计算 ──
    var currentCursorLine by remember { mutableStateOf(0) }
    val effectiveHighlightedLine = if (highlightCurrentLine) currentCursorLine else highlightedLine

    var currentSelection by remember { mutableStateOf(TextRange(content.length)) }
    var currentComposition by remember { mutableStateOf<TextRange?>(null) }

    val highlighted = highlightMarkdown(
        content,
        effectiveHighlightedLine,
        highlightLineColor,
        spellErrors,
        bracketMatching,
        currentSelection.start,
    )

    LaunchedEffect(desiredCursor, desiredSelectionEnd) {
        val cursorPos = desiredCursor
        val selEnd = desiredSelectionEnd
        if (cursorPos != null && cursorPos in 0..content.length) {
            if (selEnd != null && selEnd in 0..content.length) {
                currentSelection = TextRange(cursorPos, selEnd)
                onSelectionChange(cursorPos, selEnd)
            } else {
                currentSelection = TextRange(cursorPos)
                onSelectionChange(cursorPos, cursorPos)
            }
            onDesiredCursorConsumed()
            onDesiredSelectionEndConsumed()
        } else if (cursorPos != null) {
            if (cursorPos in 0..content.length) {
                currentSelection = TextRange(cursorPos)
                onSelectionChange(cursorPos, cursorPos)
            }
            onDesiredCursorConsumed()
            onDesiredSelectionEndConsumed()
        }
    }

    LaunchedEffect(content) {
        if (currentSelection.start > content.length) {
            currentSelection = TextRange(content.length)
        }
        if (currentSelection.end > content.length) {
            currentSelection = TextRange(
                currentSelection.start.coerceAtMost(content.length),
                content.length
            )
        }
    }

    val lineCount = content.lines().size
    val computedLineHeight = (fontSize * lineHeight).sp
    val lineNumberWidth = when {
        lineCount < 100 -> 36.dp
        lineCount < 1000 -> 48.dp
        lineCount < 10000 -> 60.dp
        else -> 72.dp
    }
    val fontFamily = editorFontFamily
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()

    // ── 行号滚动同步：共享 verticalScroll 容器 ──
    // 通过让 BasicTextField 高度等于内容高度，禁用其内部滚动，
    // 行号与文本在同一个滚动容器中，天然同步。
    // 预计算内容高度（基于行数×行高），避免依赖 onTextLayout 的延迟更新
    val verticalPadding = 24.dp // top 12dp + bottom 12dp
    val estimatedContentHeightDp = with(density) {
        val lineHeightPx = computedLineHeight.toPx()
        (lineCount * lineHeightPx).toDp() + verticalPadding
    }
    // 使用 onTextLayout 获取实际文本高度，解决自动换行时估算高度偏小导致行号截断的问题
    var actualContentHeightDp by remember(estimatedContentHeightDp) { mutableStateOf(estimatedContentHeightDp) }
    // 保存 TextLayoutResult 用于精确计算行号对应的 Y 坐标
    var textLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    val sharedScrollState = rememberScrollState()

    // ── 观察滚动位置，通知父组件用于同步滚动 ──
    LaunchedEffect(sharedScrollState) {
        snapshotFlow { sharedScrollState.value }
            .collect { scrollValue ->
                val maxValue = sharedScrollState.maxValue
                val ratio = if (maxValue > 0) scrollValue.toFloat() / maxValue else 0f
                onScrollPositionChanged(ratio)
            }
    }

    // ── 目录点击：滚动到指定行（使用 TextLayoutResult 精确定位） ──
    LaunchedEffect(scrollToLineIndex) {
        if (scrollToLineIndex >= 0) {
            val layout = textLayoutResult
            val targetScrollPx = if (layout != null) {
                // 使用 TextLayoutResult 获取目标行的精确 Y 坐标
                // scrollToLineIndex 是逻辑行号（0-based），需要转换为文本偏移量
                val lines = content.lines()
                if (scrollToLineIndex < lines.size) {
                    val charOffset = lines.take(scrollToLineIndex).sumOf { it.length + 1 }
                    // 获取该偏移量所在的可视行号
                    val visualLine = layout.getLineForOffset(charOffset.coerceAtMost(content.length))
                    // 获取该可视行的顶部 Y 坐标
                    layout.getLineTop(visualLine).toInt()
                } else {
                    0
                }
            } else {
                // 降级：用行高估算
                val lineHeightPx = with(density) { computedLineHeight.toPx() }
                (scrollToLineIndex * lineHeightPx).toInt()
            }
            coroutineScope.launch {
                sharedScrollState.animateScrollTo(
                    targetScrollPx.coerceAtMost(sharedScrollState.maxValue)
                )
            }
            onScrollToLineConsumed()
        }
    }

    // ── 预览→编辑器同步滚动：当从预览模式切换回编辑器时，恢复滚动位置 ──
    LaunchedEffect(previewScrollRatio) {
        if (previewScrollRatio > 0f && sharedScrollState.maxValue > 0) {
            val targetPx = (sharedScrollState.maxValue * previewScrollRatio).toInt()
            coroutineScope.launch {
                sharedScrollState.animateScrollTo(targetPx)
            }
        }
    }

    // ── 拼写错误建议弹窗状态 ──
    var showSpellSuggestionPopup by remember { mutableStateOf(false) }
    var activeSpellError by remember { mutableStateOf<SpellError?>(null) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(sharedScrollState)
        ) {
            // ── 行号边栏（独立于编辑区域，共享滚动） ──
            if (showLineNumbers) {
                val lineNumColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                val lineNumColorInt = android.graphics.Color.argb(
                    (lineNumColor.alpha * 255).toInt(),
                    (lineNumColor.red * 255).toInt(),
                    (lineNumColor.green * 255).toInt(),
                    (lineNumColor.blue * 255).toInt(),
                )
                // 行号字体大小与编辑器字体保持精确比例对齐
                val lineNumFontSizePx = with(density) { ((fontSize - 2).sp.toPx()) }
                val topPadPx = with(density) { 12.dp.toPx() }
                val rightPadPx = with(density) { 8.dp.toPx() }
                val lineH = with(density) { computedLineHeight.toPx() }

                // 预计算每行的字符偏移量（实时刷新：内容变化时立即重新计算）
                val lineOffsets = remember(content) {
                    val lines = content.lines()
                    val offsets = IntArray(lines.size)
                    var offset = 0
                    for (i in lines.indices) {
                        offsets[i] = offset
                        offset += lines[i].length + 1
                    }
                    offsets
                }

                // 当前高亮行号（用于行号高亮显示）
                val currentLineIndex = if (highlightCurrentLine) currentCursorLine else -1

                // 读取滚动位置以触发 recomposition，确保行号在滚动时实时更新
                val scrollSnapshot = sharedScrollState.value

                Box(
                    modifier = Modifier
                        .width(lineNumberWidth)
                        .height(maxOf(estimatedContentHeightDp, actualContentHeightDp))
                ) {
                    // 行号右侧分隔线
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    )
                    // 使用 AndroidView 绘制行号，避免 Compose Text 组件高度超限截断
                    // 通过 update 块传递所有动态数据，确保滚动/内容变化时正确重绘
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            object : android.view.View(ctx) {
                                // 当前绘制参数（由 update 块更新）
                                var paintTextSize = lineNumFontSizePx
                                var paintColor = lineNumColorInt
                                var currentLayout: androidx.compose.ui.text.TextLayoutResult? = null
                                var currentLineOffsets: IntArray = lineOffsets
                                var currentContentLength = content.length
                                var currentScrollY = 0
                                var currentLineHeight = lineH
                                var currentTopPad = topPadPx
                                var highlightedLine = currentLineIndex
                                var highlightColor = 0 // 0 表示不高亮

                                val paint = android.graphics.Paint().apply {
                                    textSize = lineNumFontSizePx
                                    color = lineNumColorInt
                                    textAlign = android.graphics.Paint.Align.RIGHT
                                    isAntiAlias = true
                                }
                                val highlightPaint = android.graphics.Paint().apply {
                                    textSize = lineNumFontSizePx
                                    color = lineNumColorInt
                                    textAlign = android.graphics.Paint.Align.RIGHT
                                    isAntiAlias = true
                                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                }

                                override fun onDraw(canvas: android.graphics.Canvas) {
                                    super.onDraw(canvas)
                                    val layout = currentLayout
                                    val scrollY = currentScrollY
                                    val vpBottom = scrollY + height + currentLineHeight

                                    val offsets = currentLineOffsets
                                    for (lineIndex in offsets.indices) {
                                        // 使用 TextLayoutResult 精确计算行号 Y 坐标
                                        // 自动换行时：同一逻辑行的多个可视行共享同一行号
                                        val y: Float = if (layout != null && lineIndex < offsets.size) {
                                            val charOffset = offsets[lineIndex].coerceAtMost(currentContentLength)
                                            val visualLine = layout.getLineForOffset(charOffset)
                                            layout.getLineTop(visualLine) + currentTopPad
                                        } else {
                                            lineIndex * currentLineHeight + currentTopPad
                                        }

                                        // 仅绘制可见区域内的行号（性能优化：大文件跳过不可见行）
                                        if (y + currentLineHeight < scrollY || y > vpBottom) continue

                                        val lineNumber = (lineIndex + 1).toString()
                                        val baselineY = y + paintTextSize * 0.85f

                                        // 当前光标行使用加粗高亮
                                        if (lineIndex == highlightedLine) {
                                            canvas.drawText(
                                                lineNumber,
                                                width - rightPadPx,
                                                baselineY,
                                                highlightPaint,
                                            )
                                        } else {
                                            canvas.drawText(
                                                lineNumber,
                                                width - rightPadPx,
                                                baselineY,
                                                paint,
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        update = { view ->
                            // 更新所有动态参数，确保行号与编辑器内容精确同步
                            view.paintTextSize = lineNumFontSizePx
                            view.paintColor = lineNumColorInt
                            view.paint.textSize = lineNumFontSizePx
                            view.paint.color = lineNumColorInt
                            view.highlightPaint.textSize = lineNumFontSizePx
                            view.highlightPaint.color = lineNumColorInt
                            view.currentLayout = textLayoutResult
                            view.currentLineOffsets = lineOffsets
                            view.currentContentLength = content.length
                            view.currentScrollY = scrollSnapshot
                            view.currentLineHeight = lineH
                            view.currentTopPad = topPadPx
                            view.highlightedLine = currentLineIndex
                            view.invalidate()
                        },
                    )
                }

            }

            // ── 代码编辑区域 ──
            BasicTextField(
                value = TextFieldValue(
                    annotatedString = highlighted,
                    selection = currentSelection,
                    composition = currentComposition,
                ),
                onValueChange = { newValue ->
                    currentSelection = newValue.selection
                    currentComposition = newValue.composition
                    onSelectionChange(newValue.selection.start, newValue.selection.end)

                    // 更新当前光标所在行
                    val textBeforeCursor = newValue.text.substring(0, newValue.selection.start)
                    currentCursorLine = textBeforeCursor.count { it == '\n' }

                    // 检测点击拼写错误：光标位置在某个拼写错误范围内时弹出建议
                    if (spellCheck && spellErrors.isNotEmpty()) {
                        val cursorPos = newValue.selection.start
                        val clickedError = spellErrors.find { error ->
                            cursorPos in error.range
                        }
                        if (clickedError != null && newValue.selection.length == 0) {
                            activeSpellError = clickedError
                            showSpellSuggestionPopup = true
                        }
                    }

                    if (newValue.text != content) {
                        // ── 括号自动补全 ──
                        if (bracketMatching && newValue.text.length > content.length) {
                            val diffStart = findFirstDifference(content, newValue.text)
                            if (diffStart >= 0 && diffStart < newValue.text.length) {
                                val insertedChar = newValue.text[diffStart]
                                val closingBracket = when (insertedChar) {
                                    '(' -> ')'
                                    '[' -> ']'
                                    '{' -> '}'
                                    else -> null
                                }
                                if (closingBracket != null && diffStart + 1 <= newValue.text.length) {
                                    val afterInsert = newValue.text.getOrNull(diffStart + 1)
                                    if (afterInsert == closingBracket) {
                                        onContentChange(newValue.text)
                                    } else {
                                        val before = newValue.text.substring(0, diffStart + 1)
                                        val after = newValue.text.substring(diffStart + 1)
                                        val newText = "$before$closingBracket$after"
                                        onContentChange(newText)
                                        currentSelection = TextRange(diffStart + 1)
                                        return@BasicTextField
                                    }
                                } else {
                                    onContentChange(newValue.text)
                                }
                            } else {
                                onContentChange(newValue.text)
                            }
                        } else if (autoIndent && newValue.text.length > content.length) {
                            // ── 自动缩进：检测换行并插入缩进 ──
                            val diffStart = findFirstDifference(content, newValue.text)
                            if (diffStart >= 0 && newValue.text.getOrNull(diffStart) == '\n') {
                                val indent = viewModel.calculateAutoIndent(
                                    newValue.text, diffStart + 1, indentType, indentSize
                                )
                                if (indent.isNotEmpty()) {
                                    val before = newValue.text.substring(0, diffStart + 1)
                                    val after = newValue.text.substring(diffStart + 1)
                                    val newText = "$before$indent$after"
                                    onContentChange(newText)
                                    currentSelection = TextRange(diffStart + 1 + indent.length)
                                    return@BasicTextField
                                } else {
                                    onContentChange(newValue.text)
                                }
                            } else {
                                onContentChange(newValue.text)
                            }
                        } else {
                            onContentChange(newValue.text)
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = 8.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 12.dp,
                    )
                    .heightIn(min = maxOf(estimatedContentHeightDp, actualContentHeightDp) - verticalPadding),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = computedLineHeight,
                    letterSpacing = letterSpacing.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = fontSize.sp,
                    fontFamily = fontFamily,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                onTextLayout = { result ->
                    val newHeight = with(density) {
                        result.size.height.toDp() + verticalPadding
                    }
                    if (newHeight != actualContentHeightDp) {
                        actualContentHeightDp = newHeight
                    }
                    textLayoutResult = result
                },
                decorationBox = { innerTextField ->
                    Box {
                        if (content.isEmpty()) {
                            Text(
                                text = stringResource(R.string.start_writing),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = fontSize.sp,
                                    lineHeight = computedLineHeight,
                                    letterSpacing = letterSpacing.sp,
                                ),
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }

        // ── 拼写错误建议弹窗 ──
        if (showSpellSuggestionPopup && activeSpellError != null) {
            val error = activeSpellError!!
            MdcitoCenterModal(
                onDismissRequest = {
                    showSpellSuggestionPopup = false
                    activeSpellError = null
                },
                visible = true,
                title = error.word,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.spell_suggestion_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    error.suggestions.forEach { suggestion ->
                        TextButton(
                            onClick = {
                                // 安全校验：确保 range 在当前 content 范围内且对应单词匹配
                                val range = error.range
                                if (range.first < 0 || range.last + 1 > content.length) {
                                    showSpellSuggestionPopup = false
                                    activeSpellError = null
                                    return@TextButton
                                }
                                val wordInRange = content.substring(range.first, range.last + 1)
                                if (wordInRange != error.word) {
                                    showSpellSuggestionPopup = false
                                    activeSpellError = null
                                    return@TextButton
                                }
                                // 用建议替换错误单词
                                val newContent = content.substring(0, range.first) +
                                    suggestion +
                                    content.substring(range.last + 1)
                                onContentChange(newContent)
                                // 更新光标到替换词末尾，避免 selection 越界崩溃
                                val newCursorPos = (range.first + suggestion.length).coerceIn(0, newContent.length)
                                currentSelection = TextRange(newCursorPos)
                                showSpellSuggestionPopup = false
                                activeSpellError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (error.suggestions.isEmpty()) {
                        Text(
                            text = stringResource(R.string.spell_no_suggestions),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = {
                            // 添加到词典
                            spellChecker.addWord(error.word)
                            // 立即重新检查拼写，清除已加入词典的单词的错误标记
                            spellErrors = spellChecker.checkSpelling(content)
                            showSpellSuggestionPopup = false
                            activeSpellError = null
                        }) {
                            Text(stringResource(R.string.spell_add_to_dict))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = {
                            showSpellSuggestionPopup = false
                            activeSpellError = null
                        }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 查找两个字符串的第一个不同位置
 */
private fun findFirstDifference(a: String, b: String): Int {
    val minLen = minOf(a.length, b.length)
    for (i in 0 until minLen) {
        if (a[i] != b[i]) return i
    }
    return minLen
}

/** 将 Compose Color 转为 CSS 可用的 #RRGGBB 格式 */
private fun colorToCssHex(color: Color): String {
    return "#${String.format("%02X%02X%02X", (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())}"
}

@Composable
private fun TableInsertDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    var rows by remember { mutableStateOf(2) }
    var cols by remember { mutableStateOf(3) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Outlined.TableChart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.insert_table), style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column {
                // 行列配置卡片
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        // 行数配置
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.rows),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                tonalElevation = 1.dp,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                ) {
                                    Surface(
                                        onClick = { rows = (rows - 1).coerceAtLeast(1) },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                "-",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontSize = 16.sp,
                                            )
                                        }
                                    }
                                    Text(
                                        "$rows",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(36.dp),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Surface(
                                        onClick = { rows = (rows + 1).coerceAtMost(20) },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                "+",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontSize = 16.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // 列数配置
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.columns),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                tonalElevation = 1.dp,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                ) {
                                    Surface(
                                        onClick = { cols = (cols - 1).coerceAtLeast(1) },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                "-",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontSize = 16.sp,
                                            )
                                        }
                                    }
                                    Text(
                                        "$cols",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(36.dp),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Surface(
                                        onClick = { cols = (cols + 1).coerceAtMost(10) },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                "+",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontSize = 16.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 可视化表格预览
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            stringResource(R.string.preview),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                        // 可视化表格网格
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            // 表头行
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(1.dp),
                            ) {
                                repeat(cols) { colIndex ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(
                                            stringResource(R.string.column_n, colIndex + 1),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                            // 数据行
                            repeat(rows) { rowIndex ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                                ) {
                                    repeat(cols) { colIndex ->
                                        val bgColor = if (rowIndex % 2 == 0) {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        } else {
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                        }
                                        Surface(
                                            color = bgColor,
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(
                                                stringResource(R.string.content),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = { onConfirm(rows, cols) }) {
                Text(stringResource(R.string.insert))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = RoundedCornerShape(20.dp),
    )
}

@Composable
private fun ImageInsertDialog(
    imageHostEnabled: Boolean = false,
    isUploading: Boolean = false,
    uploadError: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    onPickLocalImage: () -> Unit,
    onUploadToHost: () -> Unit = {},
    onClearUploadError: () -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf(if (imageHostEnabled) 2 else 0) } // 0 = URL, 1 = 本地, 2 = 图床
    var url by remember { mutableStateOf("") }
    val defaultDesc = stringResource(R.string.image_desc_default)
    var description by remember { mutableStateOf(defaultDesc) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.insert_image), style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column {
                // Tab 切换：URL / 本地图片 / 图床上传
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        onClick = { selectedTab = 0 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedTab == 0) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Link,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedTab == 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.image_url),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedTab == 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Surface(
                        onClick = { selectedTab = 1 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedTab == 1) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Image,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedTab == 1) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.image_from_local),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedTab == 1) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    // 图床上传 Tab（仅在图床启用时显示）
                    if (imageHostEnabled) {
                        Surface(
                            onClick = { selectedTab = 2 },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTab == 2) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTab == 2) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.upload_to_host),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selectedTab == 2) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> {
                        // URL 输入方式
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text(stringResource(R.string.image_url)) },
                            placeholder = { Text(stringResource(R.string.image_url_placeholder)) },
                            leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        )
                    }
                    1 -> {
                        // 本地图片选择
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            FilledTonalButton(
                                onClick = onPickLocalImage,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.image_from_local), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    2 -> {
                        // 图床上传
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.uploading_image),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                FilledTonalButton(
                                    onClick = onUploadToHost,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.upload_to_host), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            // 上传错误提示
                            uploadError?.let { error ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.image_alt)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    if (selectedTab == 0 && url.isNotBlank()) {
                        onConfirm(url, description)
                    }
                },
                enabled = selectedTab == 0 && url.isNotBlank(),
            ) {
                Text(stringResource(R.string.insert))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = RoundedCornerShape(20.dp),
    )
}
