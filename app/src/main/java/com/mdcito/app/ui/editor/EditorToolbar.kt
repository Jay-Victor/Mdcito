package com.mdcito.app.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertLink
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mdcito.app.util.HapticHelper
import androidx.compose.ui.res.stringResource
import com.mdcito.app.R
import kotlinx.coroutines.delay

@Composable
fun EditorToolbar(
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    canUndo: Boolean = true,
    canRedo: Boolean = true,
    onHeading: (Int) -> Unit = {},
    onBold: () -> Unit = {},
    onItalic: () -> Unit = {},
    onBoldItalic: () -> Unit = {},
    onStrikethrough: () -> Unit = {},
    onQuote: () -> Unit = {},
    onUnorderedList: () -> Unit = {},
    onOrderedList: () -> Unit = {},
    onTaskList: () -> Unit = {},
    onInlineCode: () -> Unit = {},
    onCodeBlock: () -> Unit = {},
    onLink: () -> Unit = {},
    onImage: () -> Unit = {},
    onTable: () -> Unit = {},
    onHorizontalRule: () -> Unit = {},
    onInlineMath: () -> Unit = {},
    onBlockMath: () -> Unit = {},
    onSearch: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isTouchDevice = LocalConfiguration.current.smallestScreenWidthDp < 600
    val toolbarHeight = if (isTouchDevice) 64.dp else 56.dp

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(toolbarHeight)
                    .padding(horizontal = 8.dp, vertical = if (isTouchDevice) 10.dp else 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 可滚动的工具栏按钮区域
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 1. 编辑操作区：撤销、取消撤销
                    ToolbarButtonGroup {
                        ToolbarBtn(icon = Icons.AutoMirrored.Outlined.Undo, label = stringResource(R.string.undo), enabled = canUndo, onClick = onUndo)
                        ToolbarBtn(icon = Icons.AutoMirrored.Outlined.Redo, label = stringResource(R.string.redo), enabled = canRedo, onClick = onRedo)
                    }

                    ToolbarDivider()

                    // 2. 标题格式区：一级标题至六级标题
                    ToolbarButtonGroup {
                        ToolbarBtn(label = "H1", displayLabel = true, onClick = { onHeading(1) })
                        ToolbarBtn(label = "H2", displayLabel = true, onClick = { onHeading(2) })
                        ToolbarBtn(label = "H3", displayLabel = true, onClick = { onHeading(3) })
                        ToolbarBtn(label = "H4", displayLabel = true, onClick = { onHeading(4) })
                        ToolbarBtn(label = "H5", displayLabel = true, onClick = { onHeading(5) })
                        ToolbarBtn(label = "H6", displayLabel = true, onClick = { onHeading(6) })
                    }

                    ToolbarDivider()

                    // 3. 文本样式区：斜体、粗体、粗斜体
                    ToolbarButtonGroup {
                        ToolbarBtn(icon = Icons.Outlined.FormatItalic, label = stringResource(R.string.italic), onClick = onItalic)
                        ToolbarBtn(icon = Icons.Outlined.FormatBold, label = stringResource(R.string.bold), onClick = onBold)
                        ToolbarBtn(label = "B I", displayLabel = true, isBoldItalic = true, onClick = onBoldItalic)
                    }

                    ToolbarDivider()

                    // 4. 文本修饰区：删除线、分割线
                    ToolbarButtonGroup {
                        ToolbarBtn(icon = Icons.Outlined.FormatStrikethrough, label = stringResource(R.string.strikethrough), onClick = onStrikethrough)
                        ToolbarBtn(icon = Icons.Outlined.HorizontalRule, label = stringResource(R.string.horizontal_rule), onClick = onHorizontalRule)
                    }

                    ToolbarDivider()

                    // 5. 列表区：无序列表、有序列表、任务列表
                    ToolbarButtonGroup {
                        ToolbarBtn(icon = Icons.AutoMirrored.Outlined.FormatListBulleted, label = stringResource(R.string.unordered_list), onClick = onUnorderedList)
                        ToolbarBtn(icon = Icons.Outlined.FormatListNumbered, label = stringResource(R.string.ordered_list), onClick = onOrderedList)
                        ToolbarBtn(icon = Icons.Outlined.CheckBox, label = stringResource(R.string.task_list), onClick = onTaskList)
                    }

                    ToolbarDivider()

                    // 6. 代码与引用区：行内代码、代码块、引用块
                    ToolbarButtonGroup {
                        ToolbarBtn(icon = Icons.Outlined.Code, label = stringResource(R.string.inline_code), onClick = onInlineCode)
                        ToolbarBtn(icon = Icons.Outlined.DataObject, label = stringResource(R.string.code_block), onClick = onCodeBlock)
                        ToolbarBtn(icon = Icons.Outlined.FormatQuote, label = stringResource(R.string.quote), onClick = onQuote)
                    }

                    ToolbarDivider()

                    // 7. 媒体与表格区：链接、图片、表格
                    ToolbarButtonGroup {
                        ToolbarBtn(icon = Icons.Outlined.InsertLink, label = stringResource(R.string.link), onClick = onLink)
                        ToolbarBtn(icon = Icons.Outlined.Image, label = stringResource(R.string.image), onClick = onImage)
                        ToolbarBtn(icon = Icons.Outlined.TableChart, label = stringResource(R.string.table), onClick = onTable)
                    }

                    ToolbarDivider()

                    // 8. 数学公式区：行内公式、块级公式
                    ToolbarButtonGroup {
                        ToolbarBtn(label = "∑", displayLabel = true, tooltipText = stringResource(R.string.inline_math), onClick = onInlineMath)
                        ToolbarBtn(label = "∑∑", displayLabel = true, tooltipText = stringResource(R.string.block_math), onClick = onBlockMath)
                    }
                }

                // 搜索按钮固定在右侧，不随滚动隐藏
                ToolbarDivider()

                ToolbarBtn(icon = Icons.Outlined.Search, label = stringResource(R.string.search), onClick = onSearch)
            }
        }
    }
}

@Composable
private fun ToolbarButtonGroup(
    content: @Composable () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun ToolbarBtn(
    icon: ImageVector? = null,
    label: String,
    displayLabel: Boolean = false,
    isBoldItalic: Boolean = false,
    enabled: Boolean = true,
    tooltipText: String? = null,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = tween(100),
        label = "btn-scale",
    )

    val isTouchDevice = LocalConfiguration.current.smallestScreenWidthDp < 600
    val btnSize = if (isTouchDevice) 44.dp else 36.dp
    val iconSize = if (isTouchDevice) 20.dp else 18.dp

    val showHover = isHovered && enabled

    // 长按提示框状态
    var showTooltip by remember { mutableStateOf(false) }
    var anchorBoundsInWindow by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }

    Box(
        modifier = Modifier
            .size(btnSize)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (showHover) Modifier.background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        ),
                    ),
                ) else Modifier
            )
            .pointerInput(enabled) {
                detectTapGestures(
                    onLongPress = {
                        if (enabled) {
                            HapticHelper.lightClick(context)
                            showTooltip = true
                        }
                    },
                    onTap = {
                        if (enabled) {
                            HapticHelper.lightClick(context)
                            onClick()
                        }
                    },
                )
            }
            .onGloballyPositioned { coordinates ->
                anchorBoundsInWindow = coordinates.boundsInWindow()
            },
        contentAlignment = Alignment.Center,
    ) {
        if (displayLabel) {
            if (isBoldItalic) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (enabled) {
                        if (showHover) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    } else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    fontSize = 11.sp,
                )
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (enabled) {
                        if (showHover) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    } else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    fontSize = 13.sp,
                )
            }
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) {
                    if (showHover) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                } else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(iconSize),
            )
        }

        // 长按提示框
        if (showTooltip) {
            SmartToolbarTooltip(
                text = tooltipText ?: label,
                onDismiss = { showTooltip = false },
                anchorBoundsInWindow = anchorBoundsInWindow,
            )
        }
    }
}

/**
 * 智能定位提示框：根据按钮在窗口中的位置自动调整显示方向，
 * 确保提示框始终在可见区域内。
 *
 * 定位策略（参考 Material Design 3 Tooltip 最佳实践）：
 * 1. 优先在按钮上方显示，空间不足则显示在按钮下方
 * 2. 水平方向自动调整，确保不超出窗口左右边界
 * 3. Popup offset 是相对于父组件（ToolbarBtn Box）的偏移量，
 *    而非绝对坐标，因此需要用目标位置减去父组件位置
 * 4. 使用 view.width/height 获取窗口可见区域尺寸（adjustResize 模式下
 *    键盘弹出时窗口会缩小），而非 displayMetrics 的物理屏幕尺寸
 * 5. 保持 4-8dp 间距，避免误触关闭
 */
@Composable
private fun SmartToolbarTooltip(
    text: String,
    onDismiss: () -> Unit,
    anchorBoundsInWindow: androidx.compose.ui.geometry.Rect,
) {
    val density = LocalDensity.current
    val view = LocalView.current

    // 使用窗口可见区域尺寸（adjustResize 模式下键盘弹出时窗口会缩小）
    val windowWidthPx = view.width
    val windowHeightPx = view.height

    val tooltipMarginPx = with(density) { 6.dp.roundToPx() }

    // 实际测量提示框尺寸
    var tooltipSize by remember { mutableStateOf(IntSize.Zero) }

    // 锚点在窗口坐标系中的位置（boundsInWindow 返回相对于窗口的坐标）
    val anchorCenterX = anchorBoundsInWindow.center.x
    val anchorTopY = anchorBoundsInWindow.top
    val anchorBottomY = anchorBoundsInWindow.bottom

    // 使用实际测量的尺寸，若尚未测量则使用合理估算值
    val tooltipWidthPx = if (tooltipSize.width > 0) tooltipSize.width else with(density) { 80.dp.roundToPx() }
    val tooltipHeightPx = if (tooltipSize.height > 0) tooltipSize.height else with(density) { 32.dp.roundToPx() }

    // 垂直方向：优先上方，空间不足则下方
    val spaceAbove = anchorTopY
    val spaceBelow = windowHeightPx - anchorBottomY
    val showAbove = spaceAbove >= tooltipHeightPx + tooltipMarginPx ||
            spaceAbove > spaceBelow

    // 计算提示框在窗口坐标系中的目标 Y 位置
    val targetY = if (showAbove) {
        (anchorTopY - tooltipHeightPx - tooltipMarginPx).toInt()
    } else {
        (anchorBottomY + tooltipMarginPx).toInt()
    }

    // 水平方向：居中，但确保不超出窗口边界
    val idealX = anchorCenterX - tooltipWidthPx / 2f
    val targetX = idealX.coerceIn(
        tooltipMarginPx.toFloat(),
        (windowWidthPx - tooltipWidthPx - tooltipMarginPx).toFloat().coerceAtLeast(tooltipMarginPx.toFloat()),
    ).toInt()

    // 关键：Popup offset 是相对于父组件（ToolbarBtn Box）的偏移量
    // Popup 最终位置 = 父组件.positionInWindow + alignment偏移(0,0) + offset
    // 因此 offset = 目标位置 - 父组件位置
    val popupOffset = IntOffset(
        targetX - anchorBoundsInWindow.left.toInt(),
        targetY - anchorBoundsInWindow.top.toInt()
    )

    // 自动消失
    LaunchedEffect(text) {
        delay(1500)
        onDismiss()
    }

    Popup(
        alignment = Alignment.TopStart,
        offset = popupOffset,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
        onDismissRequest = onDismiss,
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(150)) + scaleIn(
                animationSpec = tween(150),
                initialScale = 0.8f,
            ),
            exit = fadeOut(animationSpec = tween(100)) + scaleOut(
                animationSpec = tween(100),
                targetScale = 0.8f,
            ),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.inverseSurface,
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 4.dp,
                tonalElevation = 2.dp,
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    tooltipSize = coordinates.size
                },
            ) {
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .size(width = 1.dp, height = 24.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    )
}
