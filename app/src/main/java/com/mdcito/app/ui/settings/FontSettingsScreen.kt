package com.mdcito.app.ui.settings

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mdcito.app.R
import com.mdcito.app.data.font.BuiltinFontConfig
import com.mdcito.app.data.font.FontService
import com.mdcito.app.data.locale.LanguageHelper
import com.mdcito.app.ui.components.MdcitoCardDefaults
import kotlinx.coroutines.launch

@Composable
fun FontSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    // 读取语言版本号，确保语言切换时本页面所有 stringResource() 强制重组
    LanguageHelper.LocalLocaleVersion.current

    val uiFont by viewModel.uiFont.collectAsState()
    val uiFontSize by viewModel.uiFontSize.collectAsState()
    val editorFont by viewModel.editorFont.collectAsState()
    val codeFont by viewModel.codeFont.collectAsState()
    val fontService = viewModel.fontService
    val installedFonts by fontService.installedFonts.collectAsState()
    val downloadProgress by fontService.downloadProgress.collectAsState()

    val scope = rememberCoroutineScope()

    val allFontOptions = buildFontOptions(installedFonts)

    val editorFontOptions = buildEditorFontOptions(installedFonts)

    val codeFontOptions = buildCodeFontOptions(installedFonts)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        SettingsTopBar(title = stringResource(R.string.font_settings), onNavigateBack = onNavigateBack)

        Spacer(modifier = Modifier.height(12.dp))

        SettingsGroupTitle(stringResource(R.string.language_settings))

        LanguageSelectorSection()

        Spacer(modifier = Modifier.height(20.dp))

        SettingsGroupTitle(stringResource(R.string.ui_font))

        FontSelector(
            title = stringResource(R.string.ui_font),
            subtitle = stringResource(R.string.ui_font_subtitle),
            currentValue = uiFont,
            options = allFontOptions,
            installedFonts = installedFonts,
            downloadProgress = downloadProgress,
            fontService = fontService,
            onSelect = { fontKey, config ->
                handleFontSelection(
                    fontKey = fontKey,
                    config = config,
                    fontService = fontService,
                    scope = scope,
                    onSelected = { viewModel.setUiFont(it) },
                )
            },
            onRemove = { fontFamily ->
                fontService.removeFont(fontFamily)
                if (uiFont == fontFamily) viewModel.setUiFont("system")
                if (editorFont == fontFamily) viewModel.setEditorFont("system")
                if (codeFont == fontFamily) viewModel.setCodeFont("system")
            },
        )

        FontPreviewCard(
            title = stringResource(R.string.ui_font_preview),
            lines = listOf(stringResource(R.string.markdown_editor), stringResource(R.string.font_preview_hello_world)),
            fontKey = uiFont,
            fontService = fontService,
        )

        Spacer(modifier = Modifier.height(20.dp))

        SettingsGroupTitle(stringResource(R.string.editor_font))

        FontSelector(
            title = stringResource(R.string.editor_font),
            subtitle = stringResource(R.string.editor_font_subtitle),
            currentValue = editorFont,
            options = editorFontOptions,
            installedFonts = installedFonts,
            downloadProgress = downloadProgress,
            fontService = fontService,
            onSelect = { fontKey, config ->
                handleFontSelection(
                    fontKey = fontKey,
                    config = config,
                    fontService = fontService,
                    scope = scope,
                    onSelected = { viewModel.setEditorFont(it) },
                )
            },
            onRemove = { fontFamily ->
                fontService.removeFont(fontFamily)
                if (uiFont == fontFamily) viewModel.setUiFont("system")
                if (editorFont == fontFamily) viewModel.setEditorFont("system")
                if (codeFont == fontFamily) viewModel.setCodeFont("system")
            },
        )

        FontPreviewCard(
            title = stringResource(R.string.editor_font_preview),
            lines = listOf(
                "The quick brown fox jumps",
                "over the lazy dog. 0123456789",
            ),
            fontKey = editorFont,
            fontService = fontService,
        )

        Spacer(modifier = Modifier.height(20.dp))

        SettingsGroupTitle(stringResource(R.string.code_font))

        FontSelector(
            title = stringResource(R.string.code_font),
            subtitle = stringResource(R.string.code_font_subtitle),
            currentValue = codeFont,
            options = codeFontOptions,
            installedFonts = installedFonts,
            downloadProgress = downloadProgress,
            fontService = fontService,
            onSelect = { fontKey, config ->
                handleFontSelection(
                    fontKey = fontKey,
                    config = config,
                    fontService = fontService,
                    scope = scope,
                    onSelected = { viewModel.setCodeFont(it) },
                )
            },
            onRemove = { fontFamily ->
                fontService.removeFont(fontFamily)
                if (uiFont == fontFamily) viewModel.setUiFont("system")
                if (editorFont == fontFamily) viewModel.setEditorFont("system")
                if (codeFont == fontFamily) viewModel.setCodeFont("system")
            },
        )

        FontPreviewCard(
            title = stringResource(R.string.code_font_preview),
            lines = listOf(
                "fun main() {",
                "  println(\"Hello, World!\")",
                "}",
            ),
            fontKey = codeFont,
            fontService = fontService,
        )

        Spacer(modifier = Modifier.height(24.dp))

        SettingsGroupTitle(stringResource(R.string.install_font))

        InstallFontSection(
            fontService = fontService,
            onFontRemoved = { fontFamily ->
                if (uiFont == fontFamily) viewModel.setUiFont("system")
                if (editorFont == fontFamily) viewModel.setEditorFont("system")
                if (codeFont == fontFamily) viewModel.setCodeFont("system")
            },
        )

        Spacer(modifier = Modifier.height(24.dp))

        SettingsGroupTitle(stringResource(R.string.ui_font_size))

        UiFontSizeSection(
            currentSize = uiFontSize,
            onSizeChange = { viewModel.setUiFontSize(it) },
            fontKey = uiFont,
            fontService = fontService,
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun LanguageSelectorSection() {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var currentTag by remember { mutableStateOf(LanguageHelper.getCurrentLanguageTag()) }

    Column {
        SettingsItemCard(onClick = { expanded = !expanded }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = MdcitoCardDefaults.CompactContentPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.language_settings), style = MaterialTheme.typography.bodyLarge)
                    Text(text = stringResource(R.string.language_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val currentName = LanguageHelper.supportedLanguages.find {
                    it.tag == currentTag
                }?.displayName ?: LanguageHelper.supportedLanguages.first().displayName
                Text(text = currentName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
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
                LanguageHelper.supportedLanguages.forEachIndexed { index, option ->
                    val isSelected = currentTag == option.tag

                    val selectedBgColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                        animationSpec = tween(200),
                        label = "lang_bg",
                    )

                    Row(
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
                                LanguageHelper.setLanguage(context as Activity, option.tag)
                                currentTag = option.tag
                                expanded = false
                            }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
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

                        Text(
                            text = option.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W400,
                            modifier = Modifier.padding(vertical = 10.dp),
                        )
                    }

                    if (index < LanguageHelper.supportedLanguages.lastIndex) {
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

        LanguagePreviewCard(currentTag = currentTag)
    }
}

private data class LanguagePreviewData(
    val titleResId: Int,
    val languageNameResId: Int,
    val languageNameFallback: String = "",
    val lines: List<String>,
)

private val languagePreviewTexts = mapOf(
    "" to LanguagePreviewData(
        titleResId = R.string.preview,
        languageNameResId = R.string.follow_system,
        lines = listOf(
            "这是一段预览文本，用于展示当前语言的显示效果。",
            "系统将根据设备语言自动选择显示语言。",
        ),
    ),
    "zh-CN" to LanguagePreviewData(
        titleResId = R.string.preview,
        languageNameResId = 0,
        languageNameFallback = "简体中文",
        lines = listOf(
            "这是一段预览文本，用于展示当前语言的显示效果。",
            "Markdown 是一种轻量级标记语言，广泛应用于文档编写。",
            "你好，世界！欢迎使用 Mdcito。",
        ),
    ),
    "zh-TW" to LanguagePreviewData(
        titleResId = R.string.preview,
        languageNameResId = 0,
        languageNameFallback = "繁體中文",
        lines = listOf(
            "這是一段預覽文字，用於展示目前語言的顯示效果。",
            "Markdown 是一種輕量級標記語言，廣泛應用於文件編寫。",
            "你好，世界！歡迎使用 Mdcito。",
        ),
    ),
    "en" to LanguagePreviewData(
        titleResId = R.string.preview,
        languageNameResId = 0,
        languageNameFallback = "English",
        lines = listOf(
            "This is a preview text to demonstrate the current language display.",
            "Markdown is a lightweight markup language widely used for documentation.",
            "Hello, World! Welcome to Mdcito.",
        ),
    ),
    "ja" to LanguagePreviewData(
        titleResId = R.string.preview,
        languageNameResId = 0,
        languageNameFallback = "日本語",
        lines = listOf(
            "これは現在の言語の表示効果を示すプレビューテキストです。",
            "Markdown は軽量マークアップ言語で、ドキュメント作成に広く使用されています。",
            "こんにちは、世界！Mdcito へようこそ。",
        ),
    ),
    "ko" to LanguagePreviewData(
        titleResId = R.string.preview,
        languageNameResId = 0,
        languageNameFallback = "한국어",
        lines = listOf(
            "현재 언어 표시 효과를 보여주는 미리보기 텍스트입니다.",
            "Markdown은 문서 작성에 널리 사용되는 경량 마크업 언어입니다.",
            "안녕하세요, 세계! Mdcito에 오신 것을 환영합니다.",
        ),
    ),
    "de" to LanguagePreviewData(
        titleResId = R.string.preview,
        languageNameResId = 0,
        languageNameFallback = "Deutsch",
        lines = listOf(
            "Dies ist ein Vorschautext zur Demonstration der aktuellen Sprachanzeige.",
            "Markdown ist eine leichtgewichtige Auszeichnungssprache, die weit verbreitet ist.",
            "Hallo, Welt! Willkommen bei Mdcito.",
        ),
    ),
    "fr" to LanguagePreviewData(
        titleResId = R.string.preview,
        languageNameResId = 0,
        languageNameFallback = "Français",
        lines = listOf(
            "Ceci est un texte d'aperçu pour démontrer l'affichage de la langue actuelle.",
            "Markdown est un langage de balisage léger largement utilisé pour la documentation.",
            "Bonjour le monde ! Bienvenue sur Mdcito.",
        ),
    ),
)

@Composable
private fun LanguagePreviewCard(currentTag: String) {
    val previewData = languagePreviewTexts[currentTag] ?: languagePreviewTexts[""]!!
    val titleText = stringResource(previewData.titleResId)
    val languageNameText = if (previewData.languageNameResId != 0) {
        stringResource(previewData.languageNameResId)
    } else {
        previewData.languageNameFallback
    }

    Spacer(modifier = Modifier.height(8.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    ),
                ),
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                RoundedCornerShape(12.dp),
            )
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = languageNameText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        previewData.lines.forEachIndexed { index, line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 22.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (index < previewData.lines.lastIndex) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun buildFontOptions(installedFonts: List<com.mdcito.app.data.font.InstalledFont>): List<FontOption> {
    val options = mutableListOf(FontOption(stringResource(R.string.system_default), "system"))
    for (builtin in FontService.builtinFonts) {
        val isInstalled = installedFonts.any { it.fontFamily == builtin.fontFamily }
        options.add(FontOption(builtin.name, builtin.fontFamily, needDownload = !isInstalled, downloadConfig = if (!isInstalled) builtin else null))
    }
    for (installed in installedFonts.filter { !it.isBuiltin }) {
        options.add(FontOption(installed.name, installed.fontFamily))
    }
    return options
}

@Composable
private fun buildEditorFontOptions(installedFonts: List<com.mdcito.app.data.font.InstalledFont>): List<FontOption> {
    val options = mutableListOf(
        FontOption(stringResource(R.string.system_default), "system"),
        FontOption(stringResource(R.string.sans_serif), "sans_serif"),
        FontOption(stringResource(R.string.serif), "serif"),
        FontOption(stringResource(R.string.monospace), "monospace"),
    )
    for (builtin in FontService.builtinFonts) {
        val isInstalled = installedFonts.any { it.fontFamily == builtin.fontFamily }
        options.add(FontOption(builtin.name, builtin.fontFamily, needDownload = !isInstalled, downloadConfig = if (!isInstalled) builtin else null))
    }
    for (installed in installedFonts.filter { !it.isBuiltin }) {
        options.add(FontOption(installed.name, installed.fontFamily))
    }
    return options
}

@Composable
private fun buildCodeFontOptions(installedFonts: List<com.mdcito.app.data.font.InstalledFont>): List<FontOption> {
    val options = mutableListOf(
        FontOption(stringResource(R.string.system_default), "system"),
        FontOption(stringResource(R.string.monospace), "monospace"),
    )
    for (builtin in FontService.builtinFonts) {
        val isInstalled = installedFonts.any { it.fontFamily == builtin.fontFamily }
        options.add(FontOption(builtin.name, builtin.fontFamily, needDownload = !isInstalled, downloadConfig = if (!isInstalled) builtin else null))
    }
    for (installed in installedFonts.filter { !it.isBuiltin }) {
        options.add(FontOption(installed.name, installed.fontFamily))
    }
    return options
}

private fun handleFontSelection(
    fontKey: String,
    config: BuiltinFontConfig?,
    fontService: FontService,
    scope: kotlinx.coroutines.CoroutineScope,
    onSelected: (String) -> Unit,
) {
    if (config != null) {
        scope.launch {
            val result = fontService.downloadFont(config)
            if (result.isSuccess) {
                onSelected(fontKey)
            }
        }
    } else {
        onSelected(fontKey)
    }
}

data class FontOption(
    val name: String,
    val key: String,
    val needDownload: Boolean = false,
    val downloadConfig: BuiltinFontConfig? = null,
)

@Composable
private fun FontSelector(
    title: String,
    subtitle: String,
    currentValue: String,
    options: List<FontOption>,
    installedFonts: List<com.mdcito.app.data.font.InstalledFont>,
    downloadProgress: Map<String, Float>,
    fontService: FontService,
    onSelect: (String, BuiltinFontConfig?) -> Unit,
    onRemove: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        SettingsItemCard(onClick = { expanded = !expanded }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = MdcitoCardDefaults.CompactContentPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.bodyLarge)
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val display = options.find { it.key == currentValue }?.name ?: currentValue
                Text(text = display, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
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
                options.forEachIndexed { index, option ->
                    val isSelected = currentValue == option.key
                    val isDownloading = downloadProgress.containsKey(option.key)
                    val progress = downloadProgress[option.key]

                    val selectedBgColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                        animationSpec = tween(200),
                        label = "bg",
                    )

                    Row(
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
                            .clickable(enabled = !isDownloading) {
                                onSelect(option.key, option.downloadConfig)
                                if (option.downloadConfig == null) expanded = false
                            }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
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
                            val optionFontFamily = remember(option.key) {
                                fontService.getComposeFontFamily(option.key)
                            }
                            Text(
                                text = option.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = optionFontFamily),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W400,
                            )
                            if (option.needDownload) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = option.downloadConfig?.description ?: stringResource(R.string.need_download),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (isDownloading && progress != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                            }
                        }

                        if (isDownloading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else if (option.needDownload && !isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.CloudDownload,
                                contentDescription = stringResource(R.string.download),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    if (index < options.lastIndex) {
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
private fun FontPreviewCard(
    title: String,
    lines: List<String>,
    fontKey: String,
    fontService: FontService,
) {
    val fontFamily = remember(fontKey) { fontService.getComposeFontFamily(fontKey) }
    val systemDefault = stringResource(R.string.system_default)
    val sansSerif = stringResource(R.string.sans_serif)
    val serifLabel = stringResource(R.string.serif)
    val monospaceLabel = stringResource(R.string.monospace)
    val fontName = when (fontKey) {
        "system" -> systemDefault
        "sans_serif" -> sansSerif
        "serif" -> serifLabel
        "monospace" -> monospaceLabel
        else -> {
            val installed = fontService.installedFonts.value.find { it.fontFamily == fontKey }
            installed?.name ?: fontKey
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    ),
                ),
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                RoundedCornerShape(12.dp),
            )
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = fontName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        lines.forEachIndexed { index, line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = fontFamily,
                    lineHeight = 22.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (index < lines.lastIndex) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun InstallFontSection(
    fontService: FontService,
    onFontRemoved: (String) -> Unit = {},
) {
    // 读取语言版本号，确保语言切换时本板块强制重组
    LanguageHelper.LocalLocaleVersion.current

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var installing by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            installing = true
            scope.launch {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                val fileName = cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) it.getString(nameIndex) else "custom_font.ttf"
                    } else "custom_font.ttf"
                } ?: "custom_font.ttf"

                fontService.installFontFromUri(uri, fileName)
                installing = false
            }
        }
    }

    SettingsItemCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 14.dp),
        ) {
            Text(
                text = stringResource(R.string.import_font_file),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.font_file_support),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { launcher.launch("font/*") },
                enabled = !installing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (installing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.installing))
                } else {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.select_font_file))
                }
            }
        }
    }

    val installedFonts by fontService.installedFonts.collectAsState()
    val customFonts = installedFonts.filter { !it.isBuiltin }

    if (customFonts.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.installed_custom_fonts),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )
        customFonts.forEach { font ->
            SettingsItemCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FontDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = font.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        fontService.removeFont(font.fontFamily)
                        onFontRemoved(font.fontFamily)
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun UiFontSizeSection(
    currentSize: Int,
    onSizeChange: (Int) -> Unit,
    fontKey: String,
    fontService: FontService,
) {
    // 读取语言版本号，确保语言切换时本板块强制重组
    LanguageHelper.LocalLocaleVersion.current

    val fontFamily = remember(fontKey) { fontService.getComposeFontFamily(fontKey) }

    SettingsItemCard {
        Column(modifier = Modifier.padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.font_size),
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
                    valueRange = 12f..22f,
                    steps = 9,
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
                    text = "12",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
                Text(
                    text = "22",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                        lineHeight = (currentSize * 1.5f).sp,
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
                        text = stringResource(R.string.ui_size_hint),
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
