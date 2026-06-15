package com.mdcito.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mdcito.app.R
import com.mdcito.app.data.locale.LanguageHelper
import com.mdcito.app.data.font.FontService
import com.mdcito.app.markdown.MarkdownDialect
import com.mdcito.app.ui.components.MdcitoCardDefaults

@Composable
fun EditorSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    LanguageHelper.LocalLocaleVersion.current
    val autoSave by viewModel.autoSave.collectAsState()
    val autoSaveInterval by viewModel.autoSaveInterval.collectAsState()
    val autoSaveIntervalUnit by viewModel.autoSaveIntervalUnit.collectAsState()
    val showLineNumbers by viewModel.showLineNumbers.collectAsState()
    val highlightCurrentLine by viewModel.highlightCurrentLine.collectAsState()
    val highlightLineColor by viewModel.highlightLineColor.collectAsState()
    val autoIndent by viewModel.autoIndent.collectAsState()
    val indentType by viewModel.indentType.collectAsState()
    val indentSize by viewModel.indentSize.collectAsState()
    val bracketMatching by viewModel.bracketMatching.collectAsState()
    val spellCheck by viewModel.spellCheck.collectAsState()
    val versionSnapshot by viewModel.versionSnapshot.collectAsState()
    val saveOnExitPrompt by viewModel.saveOnExitPrompt.collectAsState()
    val syncScroll by viewModel.syncScroll.collectAsState()

    // 字体样式设置
    val fontSize by viewModel.fontSize.collectAsState()
    val lineHeight by viewModel.lineHeight.collectAsState()
    val letterSpacing by viewModel.letterSpacing.collectAsState()
    val editorFont by viewModel.editorFont.collectAsState()
    val markdownDialect by viewModel.markdownDialect.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        SettingsTopBar(title = stringResource(R.string.editor_settings), onNavigateBack = onNavigateBack)

        Spacer(modifier = Modifier.height(12.dp))

        // ── 字体样式组 ──
        SettingsGroupTitle(stringResource(R.string.font_style_group))

        FontSizeSection(
            currentSize = fontSize,
            onSizeChange = { viewModel.setFontSize(it) },
            fontKey = editorFont,
            fontService = viewModel.fontService,
        )

        Spacer(modifier = Modifier.height(8.dp))

        LineHeightSection(
            currentLineHeight = lineHeight,
            onLineHeightChange = { viewModel.setLineHeight(it) },
            fontSize = fontSize,
            fontKey = editorFont,
            fontService = viewModel.fontService,
        )

        Spacer(modifier = Modifier.height(8.dp))

        LetterSpacingSection(
            currentLetterSpacing = letterSpacing,
            onLetterSpacingChange = { viewModel.setLetterSpacing(it) },
            fontSize = fontSize,
            fontKey = editorFont,
            fontService = viewModel.fontService,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Markdown 方言组 ──
        SettingsGroupTitle(stringResource(R.string.markdown_dialect_group))

        MarkdownDialectSection(
            currentDialect = markdownDialect,
            onDialectChange = { viewModel.setMarkdownDialect(it) },
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── 保存组 ──
        SettingsGroupTitle(stringResource(R.string.save_group))

        // 自动保存开关
        SwitchSetting(
            title = stringResource(R.string.auto_save),
            subtitle = stringResource(R.string.auto_save_desc),
            checked = autoSave,
        ) { viewModel.setAutoSave(it) }

        // 自动保存间隔与状态卡片（动态显示）
        AnimatedVisibility(
            visible = autoSave,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            AutoSaveIntervalAndStatusCard(
                interval = autoSaveInterval,
                unit = autoSaveIntervalUnit,
                onIntervalChange = { viewModel.setAutoSaveInterval(it) },
                onUnitChange = { viewModel.setAutoSaveIntervalUnit(it) },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 退出时保存提示
        SwitchSetting(
            stringResource(R.string.save_on_exit_prompt),
            stringResource(R.string.save_on_exit_desc),
            saveOnExitPrompt,
        ) { viewModel.setSaveOnExitPrompt(it) }
        Spacer(modifier = Modifier.height(8.dp))

        // 版本快照开关
        SwitchSetting(
            stringResource(R.string.version_snapshot),
            stringResource(R.string.version_snapshot_desc),
            versionSnapshot,
        ) { viewModel.setVersionSnapshot(it) }
        Spacer(modifier = Modifier.height(8.dp))

        Spacer(modifier = Modifier.height(20.dp))

        // ── 编辑组 ──
        SettingsGroupTitle(stringResource(R.string.edit_group))

        SwitchSetting(stringResource(R.string.show_line_numbers), stringResource(R.string.show_line_numbers_desc), showLineNumbers) {
            viewModel.setShowLineNumbers(it)
        }
        Spacer(modifier = Modifier.height(8.dp))
        SwitchSetting(stringResource(R.string.highlight_current_line), stringResource(R.string.highlight_current_line_desc), highlightCurrentLine) {
            viewModel.setHighlightCurrentLine(it)
        }
        AnimatedVisibility(
            visible = highlightCurrentLine,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                HighlightLineColorSection(
                    currentColor = highlightLineColor,
                    onColorChange = { viewModel.setHighlightLineColor(it) },
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        SwitchSetting(stringResource(R.string.auto_indent), stringResource(R.string.auto_indent_desc), autoIndent) {
            viewModel.setAutoIndent(it)
        }
        AnimatedVisibility(
            visible = autoIndent,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                IndentConfigSection(
                    currentIndentType = indentType,
                    currentIndentSize = indentSize,
                    onIndentTypeChange = { viewModel.setIndentType(it) },
                    onIndentSizeChange = { viewModel.setIndentSize(it) },
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        SwitchSetting(stringResource(R.string.bracket_match), stringResource(R.string.bracket_match_desc), bracketMatching) {
            viewModel.setBracketMatching(it)
        }
        Spacer(modifier = Modifier.height(8.dp))
        SwitchSetting(stringResource(R.string.spell_check), stringResource(R.string.spell_check_desc), spellCheck) {
            viewModel.setSpellCheck(it)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── 预览组 ──
        SettingsGroupTitle(stringResource(R.string.preview_group))

        SwitchSetting(stringResource(R.string.sync_scroll), stringResource(R.string.sync_scroll_desc), syncScroll) {
            viewModel.setSyncScroll(it)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

/**
 * 字体大小设置板块
 * 参照 UiFontSizeSection 的设计规范
 */
@Composable
private fun FontSizeSection(
    currentSize: Int,
    onSizeChange: (Int) -> Unit,
    fontKey: String,
    fontService: FontService,
) {
    val fontFamily = remember(fontKey) { fontService.getComposeFontFamily(fontKey) }

    SettingsItemCard {
        Column(modifier = Modifier.padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.editor_font_size),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "${currentSize}sp",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.W600,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "A",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.W500,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = currentSize.toFloat(),
                    onValueChange = { onSizeChange(it.toInt()) },
                    valueRange = 10f..28f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "A",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.W500,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "10",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
                Text(
                    text = "28",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 预览区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            ),
                        ),
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        RoundedCornerShape(10.dp),
                    )
                    .padding(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.preview_text),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = currentSize.sp,
                        fontFamily = fontFamily,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.editor_font_size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    Text(
                        text = "${currentSize}sp",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = currentSize.sp,
                            fontFamily = fontFamily,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/**
 * 行间距设置板块
 */
@Composable
private fun LineHeightSection(
    currentLineHeight: Float,
    onLineHeightChange: (Float) -> Unit,
    fontSize: Int,
    fontKey: String,
    fontService: FontService,
) {
    val fontFamily = remember(fontKey) { fontService.getComposeFontFamily(fontKey) }

    SettingsItemCard {
        Column(modifier = Modifier.padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.line_height),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = String.format("%.1f", currentLineHeight),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.W600,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Slider(
                value = currentLineHeight,
                onValueChange = { onLineHeightChange(it) },
                valueRange = 1.0f..3.0f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "1.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
                Text(
                    text = "3.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 预览区域：多行文本展示行间距效果
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            ),
                        ),
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        RoundedCornerShape(10.dp),
                    )
                    .padding(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.line_height_preview_text),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = fontSize.sp,
                        fontFamily = fontFamily,
                        lineHeight = (fontSize * currentLineHeight).sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.line_height),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    Text(
                        text = String.format("%.1f", currentLineHeight),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = fontSize.sp,
                            fontFamily = fontFamily,
                            lineHeight = (fontSize * currentLineHeight).sp,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/**
 * 字间距设置板块
 */
@Composable
private fun LetterSpacingSection(
    currentLetterSpacing: Float,
    onLetterSpacingChange: (Float) -> Unit,
    fontSize: Int,
    fontKey: String,
    fontService: FontService,
) {
    val fontFamily = remember(fontKey) { fontService.getComposeFontFamily(fontKey) }

    SettingsItemCard {
        Column(modifier = Modifier.padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.letter_spacing),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = String.format("%.1f", currentLetterSpacing),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.W600,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Slider(
                value = currentLetterSpacing,
                onValueChange = { onLetterSpacingChange(it) },
                valueRange = -2.0f..5.0f,
                steps = 69,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "-2.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
                Text(
                    text = "5.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 预览区域：展示字间距效果
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            ),
                        ),
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        RoundedCornerShape(10.dp),
                    )
                    .padding(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.preview_text),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = fontSize.sp,
                        fontFamily = fontFamily,
                        letterSpacing = currentLetterSpacing.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.letter_spacing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    Text(
                        text = String.format("%.1f", currentLetterSpacing),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = fontSize.sp,
                            fontFamily = fontFamily,
                            letterSpacing = currentLetterSpacing.sp,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/**
 * Markdown 方言选择板块（仅功能选项和操作控件，无预览）
 */
@Composable
private fun MarkdownDialectSection(
    currentDialect: String,
    onDialectChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val dialectOptions = listOf(
        Triple(MarkdownDialect.COMMONMARK.key, stringResource(R.string.dialect_commonmark), stringResource(R.string.dialect_commonmark_desc)),
        Triple(MarkdownDialect.GFM.key, stringResource(R.string.dialect_gfm), stringResource(R.string.dialect_gfm_desc)),
        Triple(MarkdownDialect.JETBRAINS.key, stringResource(R.string.dialect_jetbrains), stringResource(R.string.dialect_jetbrains_desc)),
    )

    val currentDisplayName = dialectOptions.find { it.first == currentDialect }?.second ?: currentDialect

    Column {
        SettingsItemCard(onClick = { expanded = !expanded }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = MdcitoCardDefaults.CompactContentPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.markdown_dialect), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(R.string.markdown_dialect_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = currentDisplayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (expanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(tween(200)),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(tween(150)),
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(vertical = 6.dp),
            ) {
                dialectOptions.forEachIndexed { index, (key, name, desc) ->
                    val isSelected = currentDialect == key

                    val selectedBgColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                        animationSpec = tween(200),
                        label = "dialect_bg",
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(selectedBgColor)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp),
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable {
                                onDialectChange(key)
                                expanded = false
                            }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isSelected) {
                                            Modifier.background(MaterialTheme.colorScheme.primary)
                                        } else {
                                            Modifier.border(
                                                1.5.dp,
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                CircleShape,
                                            )
                                        }
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 10.dp),
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W400,
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    if (index < dialectOptions.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp)
                                .height(0.5.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoSaveIntervalAndStatusCard(
    interval: Int,
    unit: String,
    onIntervalChange: (Int) -> Unit,
    onUnitChange: (String) -> Unit,
) {
    var editingInterval by remember(interval) { mutableStateOf(interval.toString()) }
    var inputRejected by remember { mutableStateOf(false) }
    val maxInterval = if (unit == "seconds") 300 else 60
    val minInterval = 1
    val isEmpty = editingInterval.isEmpty()
    val hasError = isEmpty || inputRejected

    // 输入被拒绝后自动清除错误状态
    LaunchedEffect(inputRejected) {
        if (inputRejected) {
            delay(2000)
            inputRejected = false
        }
    }

    SettingsItemCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            // 标题行：保存间隔 + 状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.save_interval),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = if (unit == "minutes") stringResource(R.string.interval_minutes_value, interval)
                    else stringResource(R.string.interval_seconds_value, interval),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.W600,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.auto_save_status_enabled),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 间隔输入 + 单位选择
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = editingInterval,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }
                        // 空输入允许（用户正在删除）
                        if (filtered.isEmpty()) {
                            editingInterval = filtered
                            inputRejected = false
                            return@OutlinedTextField
                        }
                        val value = filtered.toIntOrNull()
                        if (value != null) {
                            if (value in minInterval..maxInterval) {
                                editingInterval = filtered
                                inputRejected = false
                                onIntervalChange(value)
                            } else {
                                // 超出范围，拒绝输入并显示错误
                                inputRejected = true
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    isError = hasError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

                FilterChip(
                    selected = unit == "seconds",
                    onClick = {
                        onUnitChange("seconds")
                        val currentVal = editingInterval.toIntOrNull() ?: 5
                        val clamped = currentVal.coerceIn(1, 300)
                        editingInterval = clamped.toString()
                        inputRejected = false
                        onIntervalChange(clamped)
                    },
                    label = { Text(stringResource(R.string.unit_seconds)) },
                )
                FilterChip(
                    selected = unit == "minutes",
                    onClick = {
                        onUnitChange("minutes")
                        val currentVal = editingInterval.toIntOrNull() ?: 1
                        val clamped = currentVal.coerceIn(1, 60)
                        editingInterval = clamped.toString()
                        inputRejected = false
                        onIntervalChange(clamped)
                    },
                    label = { Text(stringResource(R.string.unit_minutes)) },
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when {
                    isEmpty -> stringResource(
                        R.string.save_interval_out_of_range,
                        minInterval,
                        maxInterval,
                    )
                    inputRejected -> stringResource(
                        R.string.save_interval_out_of_range,
                        minInterval,
                        maxInterval,
                    )
                    else -> stringResource(
                        R.string.save_interval_range_hint,
                        minInterval,
                        maxInterval,
                        if (unit == "seconds") stringResource(R.string.unit_seconds) else stringResource(R.string.unit_minutes),
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (hasError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatVersionTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}

/**
 * 高亮行颜色选择板块
 */
@Composable
private fun HighlightLineColorSection(
    currentColor: String,
    onColorChange: (String) -> Unit,
) {
    val presetColors = listOf(
        "#4C6EF5" to stringResource(R.string.color_blue),
        "#F59F00" to stringResource(R.string.color_amber),
        "#37B24D" to stringResource(R.string.color_green),
        "#F03E3E" to stringResource(R.string.color_red),
        "#7048E8" to stringResource(R.string.color_purple),
        "#1098AD" to stringResource(R.string.color_teal),
    )

    SettingsItemCard {
        Column(modifier = Modifier.padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 14.dp)) {
            Text(
                text = stringResource(R.string.highlight_line_color),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.highlight_line_color_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                for ((colorHex, colorName) in presetColors) {
                    val isSelected = currentColor.equals(colorHex, ignoreCase = true)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(8.dp),
                                    )
                                } else Modifier
                            )
                            .clickable { onColorChange(colorHex) }
                            .padding(vertical = 8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(parseColor(colorHex))
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            CircleShape,
                                        )
                                    } else Modifier
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
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = colorName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // 预览
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        parseColor(currentColor).copy(alpha = 0.15f)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.highlight_line_preview),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun parseColor(hex: String): Color {
    return try {
        val colorInt = android.graphics.Color.parseColor(hex)
        Color(colorInt)
    } catch (_: Exception) {
        Color(0xFF4C6EF5)
    }
}

/**
 * 缩进配置板块
 */
@Composable
private fun IndentConfigSection(
    currentIndentType: String,
    currentIndentSize: Int,
    onIndentTypeChange: (String) -> Unit,
    onIndentSizeChange: (Int) -> Unit,
) {
    SettingsItemCard {
        Column(modifier = Modifier.padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 14.dp)) {
            Text(
                text = stringResource(R.string.indent_config),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.indent_config_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 缩进类型选择
            Text(
                text = stringResource(R.string.indent_type),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W500,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = currentIndentType == "spaces",
                    onClick = { onIndentTypeChange("spaces") },
                    label = { Text(stringResource(R.string.indent_spaces)) },
                )
                FilterChip(
                    selected = currentIndentType == "tabs",
                    onClick = { onIndentTypeChange("tabs") },
                    label = { Text(stringResource(R.string.indent_tabs)) },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 缩进大小（仅在空格模式下可调）
            if (currentIndentType == "spaces") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.indent_size),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.W500,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "$currentIndentSize",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.W600,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("2", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = currentIndentSize.toFloat(),
                        onValueChange = { onIndentSizeChange(it.toInt()) },
                        valueRange = 2f..8f,
                        steps = 2,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("8", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // 预览
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            ),
                        ),
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        RoundedCornerShape(8.dp),
                    )
                    .padding(12.dp),
            ) {
                val indentStr = if (currentIndentType == "tabs") "\t" else " ".repeat(currentIndentSize)
                Text(
                    text = "function example() {\n${indentStr}return true;\n}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
