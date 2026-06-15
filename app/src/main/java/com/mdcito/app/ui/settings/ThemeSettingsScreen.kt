package com.mdcito.app.ui.settings

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mdcito.app.R
import com.mdcito.app.data.locale.LanguageHelper
import com.mdcito.app.ui.components.MdcitoCardDefaults
import com.mdcito.app.ui.theme.LocalIsDarkTheme
import com.mdcito.app.ui.theme.MdcitoColors

@Composable
fun ThemeSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    LanguageHelper.LocalLocaleVersion.current
    val themeMode by viewModel.themeMode.collectAsState()
    val themeColorIndex by viewModel.themeColorIndex.collectAsState()
    val lightScheme by viewModel.lightScheme.collectAsState()
    val darkScheme by viewModel.darkScheme.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()

    val isDark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> LocalIsDarkTheme.current
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        SettingsTopBar(title = stringResource(R.string.theme_settings), onNavigateBack = onNavigateBack)

        Spacer(modifier = Modifier.height(12.dp))

        SettingsGroupTitle(stringResource(R.string.theme_mode))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeModeOption(stringResource(R.string.light_mode), "LIGHT", Icons.Outlined.LightMode, themeMode, Modifier.weight(1f)) { viewModel.setThemeMode(it) }
            ThemeModeOption(stringResource(R.string.dark_mode), "DARK", Icons.Outlined.DarkMode, themeMode, Modifier.weight(1f)) { viewModel.setThemeMode(it) }
            ThemeModeOption(stringResource(R.string.follow_system), "SYSTEM", Icons.Outlined.SettingsBrightness, themeMode, Modifier.weight(1f)) { viewModel.setThemeMode(it) }
        }

        Spacer(modifier = Modifier.height(20.dp))

        SettingsGroupTitle(stringResource(R.string.theme_color))

        SettingsItemCard {
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .then(if (dynamicColor) Modifier.alpha(0.4f) else Modifier),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        MdcitoColors.ThemeColors.all.take(6).forEachIndexed { index, color ->
                            val isSelected = index == themeColorIndex
                            ThemeColorCircle(color, isSelected, !dynamicColor) {
                                viewModel.setThemeColorIndex(index)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        MdcitoColors.ThemeColors.all.drop(6).forEachIndexed { index, color ->
                            val actualIndex = index + 6
                            val isSelected = actualIndex == themeColorIndex
                            ThemeColorCircle(color, isSelected, !dynamicColor) {
                                viewModel.setThemeColorIndex(actualIndex)
                            }
                        }
                    }
                }
                if (dynamicColor) {
                    val context = LocalContext.current
                    val hint = stringResource(R.string.dynamic_color_enabled_hint)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .matchParentSize()
                            .clickable {
                                Toast.makeText(context, hint, Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        SettingsGroupTitle(stringResource(R.string.dynamic_theme))

        val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        SwitchSetting(
            stringResource(R.string.dynamic_theme),
            if (supportsDynamicColor) stringResource(R.string.dynamic_theme_desc) else stringResource(R.string.dynamic_color_not_supported),
            dynamicColor,
            enabled = supportsDynamicColor,
        ) {
            viewModel.setDynamicColor(it)
        }

        DynamicColorPreview(dynamicColor, isDark)

        Spacer(modifier = Modifier.height(20.dp))

        SettingsGroupTitle(if (isDark) stringResource(R.string.dark_scheme) else stringResource(R.string.light_scheme))

        Box {
            Column(
                modifier = Modifier.then(if (dynamicColor) Modifier.alpha(0.4f) else Modifier)
            ) {
                if (isDark) {
                    SchemeOption(stringResource(R.string.scheme_warm_dark), stringResource(R.string.scheme_warm_dark_desc), "warm_dark", darkScheme, MdcitoColors.Scheme.Dark.warmDark, !dynamicColor) { viewModel.setDarkScheme(it) }
                    Spacer(modifier = Modifier.height(8.dp))
                    SchemeOption(stringResource(R.string.scheme_cool_dark), stringResource(R.string.scheme_cool_dark_desc), "cool_dark", darkScheme, MdcitoColors.Scheme.Dark.coolDark, !dynamicColor) { viewModel.setDarkScheme(it) }
                    Spacer(modifier = Modifier.height(8.dp))
                    SchemeOption(stringResource(R.string.scheme_oled), stringResource(R.string.scheme_oled_desc), "oled", darkScheme, MdcitoColors.Scheme.Dark.oled, !dynamicColor) { viewModel.setDarkScheme(it) }
                    Spacer(modifier = Modifier.height(8.dp))
                    SchemeOption(stringResource(R.string.scheme_midnight), stringResource(R.string.scheme_midnight_desc), "midnight", darkScheme, MdcitoColors.Scheme.Dark.midnight, !dynamicColor) { viewModel.setDarkScheme(it) }
                } else {
                    SchemeOption(stringResource(R.string.scheme_warm), stringResource(R.string.scheme_warm_desc), "warm", lightScheme, MdcitoColors.Scheme.Light.warm, !dynamicColor) { viewModel.setLightScheme(it) }
                    Spacer(modifier = Modifier.height(8.dp))
                    SchemeOption(stringResource(R.string.scheme_cool), stringResource(R.string.scheme_cool_desc), "cool", lightScheme, MdcitoColors.Scheme.Light.cool, !dynamicColor) { viewModel.setLightScheme(it) }
                    Spacer(modifier = Modifier.height(8.dp))
                    SchemeOption(stringResource(R.string.scheme_paper), stringResource(R.string.scheme_paper_desc), "paper", lightScheme, MdcitoColors.Scheme.Light.paper, !dynamicColor) { viewModel.setLightScheme(it) }
                    Spacer(modifier = Modifier.height(8.dp))
                    SchemeOption(stringResource(R.string.scheme_fresh), stringResource(R.string.scheme_fresh_desc), "fresh", lightScheme, MdcitoColors.Scheme.Light.fresh, !dynamicColor) { viewModel.setLightScheme(it) }
                }
            }
            if (dynamicColor) {
                val context = LocalContext.current
                val hint = stringResource(R.string.dynamic_color_enabled_hint)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .matchParentSize()
                        .clickable {
                            Toast.makeText(context, hint, Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun ThemeColorCircle(
    color: Color,
    isSelected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ThemeModeOption(
    label: String,
    mode: String,
    icon: ImageVector,
    currentMode: String,
    modifier: Modifier = Modifier,
    onModeChange: (String) -> Unit,
) {
    val isSelected = currentMode == mode
    Card(
        onClick = { onModeChange(mode) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W400,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SchemeOption(
    label: String,
    description: String,
    value: String,
    currentValue: String,
    schemeColors: MdcitoColors.SchemeColors,
    enabled: Boolean = true,
    onSelect: (String) -> Unit,
) {
    SettingsItemCard(onClick = { if (enabled) onSelect(value) }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = MdcitoCardDefaults.CompactContentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(end = 10.dp),
            ) {
                Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(schemeColors.background))
                Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(schemeColors.surface))
                Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(schemeColors.surfaceContainerHigh))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.W500)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (currentValue == value) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DynamicColorPreview(
    dynamicColor: Boolean,
    isDark: Boolean,
) {
    val context = LocalContext.current
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // API 级别不兼容提示
    if (dynamicColor && !supportsDynamicColor) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.dynamic_color_not_supported),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        return
    }

    if (!dynamicColor || !supportsDynamicColor) return

    // 获取当前动态色方案（使用实际主题模式而非系统暗色模式）
    val colorScheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

    // 提取种子色（从壁纸主色获取）
    val seedColor = remember(colorScheme) {
        colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
        border = BorderStroke(0.5.dp, colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行：预览标题 + 种子色
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.dynamic_color_preview),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant,
                )
                // 种子色指示器
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(seedColor)
                            .border(1.dp, colorScheme.outline.copy(alpha = 0.3f), CircleShape),
                    )
                    Text(
                        text = stringResource(R.string.dynamic_color_seed),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 实时应用效果预览
            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.dynamic_color_effect_preview),
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 模拟按钮效果
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.primary)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Primary",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onPrimary,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Container",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onPrimaryContainer,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.secondaryContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Secondary",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSecondaryContainer,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 模拟卡片效果
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.surface)
                    .border(0.5.dp, colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = colorScheme.onTertiaryContainer,
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Surface Card",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSurface,
                        )
                        Text(
                            text = "onSurface text",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 主色调行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ColorSwatch("Primary", colorScheme.primary, colorScheme.onPrimary)
                ColorSwatch("P.Container", colorScheme.primaryContainer, colorScheme.onPrimaryContainer)
                ColorSwatch("Secondary", colorScheme.secondary, colorScheme.onSecondary)
                ColorSwatch("S.Container", colorScheme.secondaryContainer, colorScheme.onSecondaryContainer)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 辅助色调行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ColorSwatch("Tertiary", colorScheme.tertiary, colorScheme.onTertiary)
                ColorSwatch("Surface", colorScheme.surface, colorScheme.onSurface)
                ColorSwatch("S.Variant", colorScheme.surfaceVariant, colorScheme.onSurfaceVariant)
                ColorSwatch("Background", colorScheme.background, colorScheme.onBackground)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 跳转系统壁纸设置按钮
            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_SET_WALLPAPER).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // 某些设备可能不支持此 Intent
                        }
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Wallpaper,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.dynamic_color_wallpaper_settings),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(label: String, color: Color, onColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(text = label.take(3), style = MaterialTheme.typography.labelSmall, color = onColor)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
