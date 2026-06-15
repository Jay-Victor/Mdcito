package com.mdcito.app.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mdcito.app.markdown.MarkdownHeading
import androidx.compose.ui.res.stringResource
import com.mdcito.app.R

// ──────────────────────────────────────────────────────
//  每个标题等级的专属配色方案（6级，各有独立视觉身份）
// ──────────────────────────────────────────────────────
private val levelPalettes = listOf(
    // H1 — 深靛蓝主调，权威感
    LevelPalette(
        base = Color(0xFF4A5587),
        bg = Color(0xFF4A5587).copy(alpha = 0.10f),
        badgeBg = Color(0xFF4A5587).copy(alpha = 0.18f),
        border = Color(0xFF4A5587).copy(alpha = 0.18f),
    ),
    // H2 — 珊瑚暖橙，醒目但不刺眼
    LevelPalette(
        base = Color(0xFFE07B54),
        bg = Color(0xFFE07B54).copy(alpha = 0.09f),
        badgeBg = Color(0xFFE07B54).copy(alpha = 0.16f),
        border = Color(0xFFE07B54).copy(alpha = 0.15f),
    ),
    // H3 — 青绿，清新层级
    LevelPalette(
        base = Color(0xFF2D9C94),
        bg = Color(0xFF2D9C94).copy(alpha = 0.08f),
        badgeBg = Color(0xFF2D9C94).copy(alpha = 0.15f),
        border = Color(0xFF2D9C94).copy(alpha = 0.14f),
    ),
    // H4 — 琥珀金，温暖过渡
    LevelPalette(
        base = Color(0xFFD4A03A),
        bg = Color(0xFFD4A03A).copy(alpha = 0.08f),
        badgeBg = Color(0xFFD4A03A).copy(alpha = 0.14f),
        border = Color(0xFFD4A03A).copy(alpha = 0.13f),
    ),
    // H5 — 薰衣草紫，优雅收尾
    LevelPalette(
        base = Color(0xFF9B72AA),
        bg = Color(0xFF9B72AA).copy(alpha = 0.08f),
        badgeBg = Color(0xFF9B72AA).copy(alpha = 0.14f),
        border = Color(0xFF9B72AA).copy(alpha = 0.12f),
    ),
    // H6 — 石板灰，沉稳末端
    LevelPalette(
        base = Color(0xFF6B7280),
        bg = Color(0xFF6B7280).copy(alpha = 0.07f),
        badgeBg = Color(0xFF6B7280).copy(alpha = 0.13f),
        border = Color(0xFF6B7280).copy(alpha = 0.11f),
    ),
)

private data class LevelPalette(
    val base: Color,
    val bg: Color,
    val badgeBg: Color,
    val border: Color,
)

// ──────────────────────────────────────────────────────
//  主组件
// ──────────────────────────────────────────────────────
@Composable
fun TableOfContentsSidebar(
    visible: Boolean,
    headings: List<MarkdownHeading>,
    onHeadingClick: (Int) -> Unit,
    onDismiss: () -> Unit,
    cursorLine: Int = 1,
) {
    var collapsedNodes by remember { mutableStateOf(emptySet<Int>()) }
    val tocTree = remember(headings) { buildTocTree(headings) }
    val allParentIds = remember(tocTree) { collectParentIds(tocTree) }
    val allExpanded = allParentIds.isNotEmpty() && allParentIds.none { it in collapsedNodes }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 遮罩层
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = onDismiss),
            )

            // 目录面板
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 20.dp,
                    modifier = Modifier
                        .width(296.dp)
                        .fillMaxHeight(0.82f),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // ── 标题栏：图标 | 目录 | 数量 | 全部收起 | 关闭 ──
                        TocHeaderBar(
                            headingsCount = headings.size,
                            hasParents = allParentIds.isNotEmpty(),
                            isAllExpanded = allExpanded,
                            onToggleAll = {
                                collapsedNodes = if (allExpanded) allParentIds else emptySet()
                            },
                            onDismiss = onDismiss,
                        )

                        // ── 内容区 ──
                        if (headings.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.List,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                        modifier = Modifier.size(40.dp),
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = stringResource(R.string.no_headings_found),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                tocTree.forEach { node ->
                                    item(key = "toc-${node.heading.lineIndex}") {
                                        TocTreeItem(
                                            node = node,
                                            collapsedNodes = collapsedNodes,
                                            onToggleCollapse = { nodeId ->
                                                collapsedNodes = if (nodeId in collapsedNodes) {
                                                    collapsedNodes - nodeId
                                                } else {
                                                    collapsedNodes + nodeId
                                                }
                                            },
                                            cursorLine = cursorLine,
                                            headings = headings,
                                            onHeadingClick = onHeadingClick,
                                            depth = 0,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────
//  标题栏：图标 + 目录名 + 数量 + 收起按钮 + 关闭
// ──────────────────────────────────────────────────────
@Composable
private fun TocHeaderBar(
    headingsCount: Int,
    hasParents: Boolean,
    isAllExpanded: Boolean,
    onToggleAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.40f),
        shape = RoundedCornerShape(topStart = 22.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 图标
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .size(38.dp)
                        .padding(9.dp),
                )
            }
            Spacer(modifier = Modifier.width(11.dp))

            // 标题
            Text(
                text = stringResource(R.string.toc),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // 数量徽章
            if (headingsCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.toc_count, headingsCount),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 全部收起 / 展开按钮（与标题同行，横向排列）
            if (hasParents) {
                Row(
                    modifier = Modifier
                        .clickable(onClick = onToggleAll)
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isAllExpanded) Icons.Outlined.ExpandLess
                        else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isAllExpanded) stringResource(R.string.collapse_all)
                        else stringResource(R.string.expand_all),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // 关闭按钮
            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────
//  数据结构 & 辅助函数
// ──────────────────────────────────────────────────────
private data class TocNode(
    val heading: MarkdownHeading,
    val children: MutableList<TocNode> = mutableListOf(),
)

private fun buildTocTree(headings: List<MarkdownHeading>): List<TocNode> {
    val roots = mutableListOf<TocNode>()
    val stack = mutableListOf<TocNode>()
    for (heading in headings) {
        val node = TocNode(heading)
        while (stack.isNotEmpty() && stack.last().heading.level >= heading.level) {
            stack.removeAt(stack.lastIndex)
        }
        if (stack.isEmpty()) roots.add(node) else stack.last().children.add(node)
        stack.add(node)
    }
    return roots
}

private fun collectParentIds(nodes: List<TocNode>): Set<Int> {
    val ids = mutableSetOf<Int>()
    for (node in nodes) {
        if (node.children.isNotEmpty()) {
            ids.add(node.heading.lineIndex)
            ids.addAll(collectParentIds(node.children))
        }
    }
    return ids
}

private fun isHeadingCurrent(heading: MarkdownHeading, cursorLine: Int, headings: List<MarkdownHeading>): Boolean {
    var current: MarkdownHeading? = null
    for (h in headings.sortedBy { it.lineIndex }) {
        if (h.lineIndex <= cursorLine - 1) current = h else break
    }
    return current == heading
}

// ──────────────────────────────────────────────────────
//  递归树节点渲染
// ──────────────────────────────────────────────────────
@Composable
private fun TocTreeItem(
    node: TocNode,
    collapsedNodes: Set<Int>,
    onToggleCollapse: (Int) -> Unit,
    cursorLine: Int,
    headings: List<MarkdownHeading>,
    onHeadingClick: (Int) -> Unit,
    depth: Int,
) {
    val hasChildren = node.children.isNotEmpty()
    val isCollapsed = node.heading.lineIndex in collapsedNodes
    val isCurrent = isHeadingCurrent(node.heading, cursorLine, headings)

    TocItemCard(
        heading = node.heading,
        palette = levelPalettes[(node.heading.level - 1) % levelPalettes.size],
        isCurrent = isCurrent,
        hasChildren = hasChildren,
        isCollapsed = isCollapsed,
        depth = depth,
        onHeadingClick = { onHeadingClick(node.heading.lineIndex) },
        onToggleCollapse = { onToggleCollapse(node.heading.lineIndex) },
    )

    AnimatedVisibility(
        visible = !isCollapsed,
        enter = expandVertically(animationSpec = tween(220)) + fadeIn(tween(180)),
        exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(tween(150)),
    ) {
        Column {
            node.children.forEach { child ->
                TocTreeItem(
                    node = child,
                    collapsedNodes = collapsedNodes,
                    onToggleCollapse = onToggleCollapse,
                    cursorLine = cursorLine,
                    headings = headings,
                    onHeadingClick = onHeadingClick,
                    depth = depth + 1,
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────
//  单个目录项卡片 —— 每级独特色彩
// ──────────────────────────────────────────────────────
@Composable
private fun TocItemCard(
    heading: MarkdownHeading,
    palette: LevelPalette,
    isCurrent: Boolean,
    hasChildren: Boolean,
    isCollapsed: Boolean,
    depth: Int,
    onHeadingClick: () -> Unit,
    onToggleCollapse: () -> Unit,
) {
    val bgColor = palette.bg
    val borderColor = palette.border
    val borderWidth = 0.75f.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 18).dp, top = 3.dp, bottom = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color = bgColor, shape = RoundedCornerShape(12.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .drawBehind {
                // 左侧色条 —— 统一样式
                val barWidth = 2f.dp.toPx()
                drawRoundRect(
                    color = palette.base.copy(alpha = 0.50f),
                    topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(barWidth, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width.coerceAtMost(6f)),
                )
            }
            .clickable(onClick = onHeadingClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // H 徽章
            Surface(
                color = palette.badgeBg,
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text = "H${heading.level}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = palette.base,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                )
            }

            Spacer(modifier = Modifier.width(9.dp))

            // 标题文字
            Text(
                text = heading.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = when (heading.level) {
                        1 -> FontWeight.Bold
                        2 -> FontWeight.SemiBold
                        else -> FontWeight.Normal
                    },
                    fontSize = when (heading.level) {
                        1 -> 15.sp
                        2 -> 14.sp
                        else -> 13.sp
                    },
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
            )

            // 右侧折叠箭头
            if (hasChildren) {
                val rotation by animateFloatAsState(
                    targetValue = if (isCollapsed) 0f else 180f,
                    animationSpec = tween(250),
                    label = "arrowRot",
                )
                IconButton(
                    onClick = onToggleCollapse,
                    modifier = Modifier.size(26.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = if (isCollapsed) stringResource(R.string.expand)
                        else stringResource(R.string.collapse),
                        tint = palette.base.copy(alpha = 0.65f),
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(rotation),
                    )
                }
            }
        }
    }
}
