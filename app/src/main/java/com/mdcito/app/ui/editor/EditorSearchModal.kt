@file:OptIn(ExperimentalMaterial3Api::class)

package com.mdcito.app.ui.editor

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.rememberTooltipState
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.mdcito.app.R

@Composable
fun EditorSearchModal(
    visible: Boolean,
    viewModel: EditorViewModel,
    onDismiss: () -> Unit,
) {
    val searchState by viewModel.searchState.collectAsState()
    var showReplace by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it }),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            contentAlignment = Alignment.TopCenter,
        ) {
            Surface(
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // 搜索输入行
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (searchState.query.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_placeholder),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                )
                            }
                            BasicTextField(
                                value = searchState.query,
                                onValueChange = {
                                    viewModel.updateSearchQuery(it)
                                    if (it.isNotEmpty()) {
                                        showHistory = false
                                        showResults = true
                                    } else {
                                        showResults = false
                                    }
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                            )
                        }
                        if (searchState.query.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    viewModel.updateSearchQuery("")
                                    showResults = false
                                },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        // 历史图标按钮（始终显示，无历史时置灰）
                        IconButton(
                            onClick = {
                                showHistory = !showHistory
                                if (showHistory) showResults = false
                            },
                            enabled = searchState.searchHistory.isNotEmpty(),
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = when {
                                    showHistory -> MaterialTheme.colorScheme.primary
                                    searchState.searchHistory.isNotEmpty() -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                },
                            )
                        }
                        // 匹配计数
                        if (searchState.query.isNotEmpty()) {
                            if (searchState.hasMatches) {
                                Text(
                                    text = "${searchState.currentMatchDisplay}/${searchState.matchCount}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 2.dp),
                                )
                            } else {
                                Text(
                                    text = "0/0",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(horizontal = 2.dp),
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.goToPreviousMatch() },
                            enabled = searchState.hasMatches,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowUp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (searchState.hasMatches) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            )
                        }
                        IconButton(
                            onClick = { viewModel.goToNextMatch() },
                            enabled = searchState.hasMatches,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (searchState.hasMatches) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 筛选行
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = TooltipAnchorPosition.Above),
                            tooltip = { PlainTooltip { Text(stringResource(R.string.case_sensitive_tooltip)) } },
                            state = rememberTooltipState(),
                        ) {
                            FilterChip(
                                selected = searchState.isCaseSensitive,
                                onClick = { viewModel.toggleCaseSensitive() },
                                label = {
                                    Text(
                                        "Aa",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Serif,
                                    )
                                },
                                modifier = Modifier.height(28.dp),
                                leadingIcon = if (searchState.isCaseSensitive) {
                                    {
                                        Icon(
                                            imageVector = Icons.Outlined.Search,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                                    enabled = true,
                                    selected = searchState.isCaseSensitive,
                                ),
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = TooltipAnchorPosition.Above),
                            tooltip = { PlainTooltip { Text(stringResource(R.string.whole_word_tooltip)) } },
                            state = rememberTooltipState(),
                        ) {
                            FilterChip(
                                selected = searchState.isWholeWord,
                                onClick = { viewModel.toggleWholeWord() },
                                label = {
                                    Text(
                                        stringResource(R.string.whole_word),
                                        fontSize = 12.sp,
                                        fontWeight = if (searchState.isWholeWord) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                modifier = Modifier.height(28.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                                    enabled = true,
                                    selected = searchState.isWholeWord,
                                ),
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = TooltipAnchorPosition.Above),
                            tooltip = { PlainTooltip { Text(stringResource(R.string.regex_tooltip)) } },
                            state = rememberTooltipState(),
                        ) {
                            FilterChip(
                                selected = searchState.isRegex,
                                onClick = { viewModel.toggleRegex() },
                                label = {
                                    Text(
                                        ".*",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                },
                                modifier = Modifier.height(28.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                                    enabled = true,
                                    selected = searchState.isRegex,
                                ),
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        // 结果列表切换按钮
                        if (searchState.hasMatches) {
                            TextButton(
                                onClick = {
                                    showResults = !showResults
                                    if (showResults) showHistory = false
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.search_results_count, searchState.matchCount),
                                    fontSize = 12.sp,
                                    color = if (showResults) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        TextButton(
                            onClick = { showReplace = !showReplace },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            Text(
                                text = if (showReplace) stringResource(R.string.collapse) else stringResource(R.string.replace),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    // 替换行
                    AnimatedVisibility(visible = showReplace) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    if (searchState.replaceQuery.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.replace_with),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                        )
                                    }
                                    BasicTextField(
                                        value = searchState.replaceQuery,
                                        onValueChange = { viewModel.updateReplaceQuery(it) },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.replaceCurrent() },
                                    enabled = searchState.hasMatches,
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                ) {
                                    Text(stringResource(R.string.replace), fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedButton(
                                    onClick = { viewModel.replaceAll() },
                                    enabled = searchState.hasMatches,
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                ) {
                                    Text(stringResource(R.string.replace_all), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // 搜索结果卡片列表
                    AnimatedVisibility(
                        visible = showResults && searchState.hasMatches,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                items(
                                    items = searchState.matchedLines,
                                    key = { it.lineNumber }
                                ) { matchedLine ->
                                    val isCurrentLine = matchedLine.matchIndices.contains(searchState.currentMatchIndex)
                                    SearchResultCard(
                                        matchedLine = matchedLine,
                                        isCurrentLine = isCurrentLine,
                                        onClick = {
                                            val firstMatchIndex = matchedLine.matchIndices.firstOrNull() ?: return@SearchResultCard
                                            viewModel.goToMatchAtIndex(firstMatchIndex)
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // 无结果提示
                    AnimatedVisibility(
                        visible = showResults && searchState.query.isNotEmpty() && !searchState.hasMatches,
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.no_search_results),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    // 搜索历史列表
                    AnimatedVisibility(
                        visible = showHistory && searchState.searchHistory.isNotEmpty(),
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.search_history_title),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                IconButton(
                                    onClick = { viewModel.clearSearchHistory() },
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteSweep,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                items(
                                    items = searchState.searchHistory,
                                    key = { it }
                                ) { historyQuery ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.selectHistoryItem(historyQuery)
                                                showHistory = false
                                                showResults = true
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.History,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = historyQuery,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteSearchHistoryItem(historyQuery) },
                                            modifier = Modifier.size(20.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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
}

@Composable
private fun SearchResultCard(
    matchedLine: MatchedLine,
    isCurrentLine: Boolean,
    onClick: () -> Unit,
) {
    val highlightColor = if (isCurrentLine) Color(0xFFFFD54F) else Color(0xFFFFF9C4)
    val backgroundColor = if (isCurrentLine) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // 行号
            Text(
                text = "${matchedLine.lineNumber}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.width(28.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            // 行内容（高亮关键词）
            Text(
                text = buildHighlightedLineContent(matchedLine.lineContent, matchedLine.matchRangesInLine, highlightColor, MaterialTheme.colorScheme.onSurface),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun buildHighlightedLineContent(
    lineContent: String,
    matchRanges: List<IntRange>,
    highlightColor: Color,
    textColor: Color,
) = buildAnnotatedString {
    var pos = 0
    for (range in matchRanges) {
        if (range.first > pos) {
            append(lineContent.substring(pos, range.first))
        }
        withStyle(
            SpanStyle(
                background = highlightColor,
                color = textColor,
                fontWeight = FontWeight.Medium,
            )
        ) {
            val end = minOf(range.last + 1, lineContent.length)
            val start = minOf(range.first, lineContent.length)
            if (start < end) {
                append(lineContent.substring(start, end))
            }
        }
        pos = range.last + 1
    }
    if (pos < lineContent.length) {
        append(lineContent.substring(pos))
    }
}

@Composable
fun SearchHighlightText(
    text: String,
    matches: List<IntRange>,
    currentMatchIndex: Int,
    modifier: Modifier = Modifier,
) {
    val currentMatch = matches.getOrNull(currentMatchIndex)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val annotated = buildAnnotatedString {
        var pos = 0
        for ((index, range) in matches.withIndex()) {
            if (range.first > pos) {
                append(text.substring(pos, range.first))
            }
            val isCurrent = index == currentMatchIndex
            withStyle(
                SpanStyle(
                    background = if (isCurrent)
                        Color(0xFFFFD54F)
                    else
                        Color(0xFFFFF9C4),
                    color = onSurfaceColor,
                )
            ) {
                val end = minOf(range.last + 1, text.length)
                val start = minOf(range.first, text.length)
                if (start < end) {
                    append(text.substring(start, end))
                }
            }
            pos = range.last + 1
        }
        if (pos < text.length) {
            append(text.substring(pos))
        }
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier,
    )
}
