package com.mdcito.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mdcito.app.R
import com.mdcito.app.data.locale.LanguageHelper
import com.mdcito.app.ui.components.CardStyle
import com.mdcito.app.ui.components.MdcitoCard
import com.mdcito.app.ui.components.MdcitoCardDefaults
import com.mdcito.app.ui.components.glass.GlassThemeProvisioning
import com.mdcito.app.ui.components.glass.liquidGlass
import com.mdcito.app.ui.components.glass.waterGlass
import com.mdcito.app.ui.theme.LocalIsDarkTheme

@Composable
fun CardStyleSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    LanguageHelper.LocalLocaleVersion.current
    val cardStyle by viewModel.cardStyle.collectAsState()
    // ── 每种风格独立的调节参数 ──
    val cardMinimalTransparency by viewModel.cardMinimalTransparency.collectAsState()
    val cardFrostedIntensity by viewModel.cardFrostedIntensity.collectAsState()
    val cardLiquidIntensity by viewModel.cardLiquidIntensity.collectAsState()
    val navBarStyle by viewModel.navBarStyle.collectAsState()
    val navBarMinimalTransparency by viewModel.navBarMinimalTransparency.collectAsState()
    val navBarFrostedIntensity by viewModel.navBarFrostedIntensity.collectAsState()
    val navBarLiquidIntensity by viewModel.navBarLiquidIntensity.collectAsState()

    // 根据当前 cardStyle 解析预览用的值
    val previewGlassIntensity = when (cardStyle) {
        "frosted_glass" -> cardFrostedIntensity
        "liquid_glass" -> cardLiquidIntensity
        else -> 0
    }
    val previewTransparency = when (cardStyle) {
        "minimal" -> cardMinimalTransparency
        else -> 100
    }
    val previewNavBarGlassIntensity = when (navBarStyle) {
        "frosted_glass" -> navBarFrostedIntensity
        "liquid_glass" -> navBarLiquidIntensity
        else -> 0
    }
    val previewNavBarTransparency = when (navBarStyle) {
        "minimal" -> navBarMinimalTransparency
        else -> 100
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        SettingsTopBar(title = stringResource(R.string.card_style_settings), onNavigateBack = onNavigateBack)

        Spacer(modifier = Modifier.height(12.dp))

        // ===== 卡片样式 Section =====
        SettingsGroupTitle(stringResource(R.string.card_style))

        CardStyleOption(
            label = stringResource(R.string.card_style_minimal),
            description = stringResource(R.string.card_style_minimal_desc),
            value = "minimal",
            currentValue = cardStyle,
            onSelect = { viewModel.setCardStyle(it) },
        )
        if (cardStyle == "minimal") {
            Spacer(modifier = Modifier.height(4.dp))
            TransparencySlider(
                value = cardMinimalTransparency,
                onValueChange = { viewModel.setCardMinimalTransparency(it) },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        CardStyleOption(
            label = stringResource(R.string.card_style_frosted),
            description = stringResource(R.string.card_style_frosted_desc),
            value = "frosted_glass",
            currentValue = cardStyle,
            onSelect = { viewModel.setCardStyle(it) },
        )
        if (cardStyle == "frosted_glass") {
            Spacer(modifier = Modifier.height(4.dp))
            GlassIntensitySlider(
                value = cardFrostedIntensity,
                onValueChange = { viewModel.setCardFrostedIntensity(it) },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        CardStyleOption(
            label = stringResource(R.string.card_style_liquid),
            description = stringResource(R.string.card_style_liquid_desc),
            value = "liquid_glass",
            currentValue = cardStyle,
            onSelect = { viewModel.setCardStyle(it) },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Card Preview
        CardPreview(
            cardStyle = cardStyle,
            cardGlassIntensity = previewGlassIntensity,
            cardTransparency = previewTransparency,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ===== 状态栏样式 Section =====
        SettingsGroupTitle(stringResource(R.string.status_bar_style))

        CardStyleOption(
            label = stringResource(R.string.status_bar_style_minimal),
            description = stringResource(R.string.status_bar_style_minimal_desc),
            value = "minimal",
            currentValue = navBarStyle,
            onSelect = { viewModel.setNavBarStyle(it) },
        )
        if (navBarStyle == "minimal") {
            Spacer(modifier = Modifier.height(4.dp))
            TransparencySlider(
                value = navBarMinimalTransparency,
                onValueChange = { viewModel.setNavBarMinimalTransparency(it) },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        CardStyleOption(
            label = stringResource(R.string.status_bar_style_frosted),
            description = stringResource(R.string.status_bar_style_frosted_desc),
            value = "frosted_glass",
            currentValue = navBarStyle,
            onSelect = { viewModel.setNavBarStyle(it) },
        )
        if (navBarStyle == "frosted_glass") {
            Spacer(modifier = Modifier.height(4.dp))
            GlassIntensitySlider(
                value = navBarFrostedIntensity,
                onValueChange = { viewModel.setNavBarFrostedIntensity(it) },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        CardStyleOption(
            label = stringResource(R.string.status_bar_style_liquid),
            description = stringResource(R.string.status_bar_style_liquid_desc),
            value = "liquid_glass",
            currentValue = navBarStyle,
            onSelect = { viewModel.setNavBarStyle(it) },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Status Bar Preview
        StatusBarPreview(
            navBarStyle = navBarStyle,
            navBarGlassIntensity = previewNavBarGlassIntensity,
            navBarTransparency = previewNavBarTransparency,
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun CardStyleOption(
    label: String,
    description: String,
    value: String,
    currentValue: String,
    onSelect: (String) -> Unit,
) {
    SettingsItemCard(onClick = { onSelect(value) }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = MdcitoCardDefaults.CompactContentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Palette,
                contentDescription = label,
                tint = MdcitoCardDefaults.iconTint(),
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.W500)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MdcitoCardDefaults.glassSubtleContentColor(),
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

@Composable
private fun TransparencySlider(
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    SettingsItemCard {
        Column(modifier = Modifier.padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = stringResource(R.string.transparency_level), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${value}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun GlassIntensitySlider(
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    SettingsItemCard {
        Column(modifier = Modifier.padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = stringResource(R.string.glass_intensity), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "$value",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = 0f..300f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CardPreview(
    cardStyle: String,
    cardGlassIntensity: Int,
    cardTransparency: Int,
) {
    val style = when (cardStyle) {
        "frosted_glass" -> CardStyle.FROSTED_GLASS
        "liquid_glass" -> CardStyle.LIQUID_GLASS
        else -> CardStyle.MINIMAL
    }
    val styleName = when (cardStyle) {
        "frosted_glass" -> stringResource(R.string.card_style_frosted)
        "liquid_glass" -> stringResource(R.string.card_style_liquid)
        else -> stringResource(R.string.card_style_minimal)
    }

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
                text = stringResource(R.string.card_style_preview),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = styleName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        val isDarkTheme = LocalIsDarkTheme.current
        // ── 卡片预览的彩色背景容器（与状态栏预览版块一致的彩色背景块）──
        // 固定高度 + clip 包裹 GlassThemeProvisioning：
        //   1. 使彩色渐变作为可见的背景块（卡片四周露出渐变）
        //   2. 为 BoxWithConstraints 提供有界高度约束，避免可滚动 Column 中
        //      fillMaxSize 高度无界导致背景层塌缩为 0、玻璃采样区为空
        //   3. MdcitoCard 的 .liquidGlass() / .waterGlass() 采样此渐变
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(10.dp)),
        ) {
            GlassThemeProvisioning(
                darkTheme = isDarkTheme,
                backgroundContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF6B6BFF),
                                            Color(0xFF4ECDC4),
                                            Color(0xFFFF6B9D),
                                        ),
                                        start = Offset.Zero,
                                        end = Offset(size.width, size.height),
                                    ),
                                )
                            },
                    )
                },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    MdcitoCard(
                        cardStyle = style,
                        glassIntensity = cardGlassIntensity,
                        transparency = cardTransparency,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Text(
                                text = "Mdcito",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.W600,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.card_preview_title),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MdcitoCardDefaults.glassSubtleContentColor(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = stringResource(R.string.card_preview_files_count, 3),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MdcitoCardDefaults.glassSubtleContentColor(),
                                )
                                Text(
                                    text = "2.4 MB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MdcitoCardDefaults.glassSubtleContentColor(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBarPreview(
    navBarStyle: String,
    navBarGlassIntensity: Int,
    navBarTransparency: Int,
) {
    val styleName = when (navBarStyle) {
        "frosted_glass" -> stringResource(R.string.status_bar_style_frosted)
        "liquid_glass" -> stringResource(R.string.status_bar_style_liquid)
        else -> stringResource(R.string.status_bar_style_minimal)
    }

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
                text = stringResource(R.string.status_bar_preview),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = styleName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        val isDarkTheme = LocalIsDarkTheme.current
        // ── 嵌套 GlassThemeProvisioning：以彩色渐变作为本预览的 backgroundContent， ──
        // ── 使 PreviewNavBar 的 .liquidGlass() / .waterGlass() 能采样到此渐变 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(10.dp)),
        ) {
            GlassThemeProvisioning(
                darkTheme = isDarkTheme,
                backgroundContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF6B6BFF),
                                            Color(0xFF4ECDC4),
                                            Color(0xFFFF6B9D),
                                        ),
                                        start = Offset.Zero,
                                        end = Offset(size.width, size.height),
                                    ),
                                )
                            },
                    )
                },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    PreviewNavBar(
                        navBarStyle = navBarStyle,
                        navBarGlassIntensity = navBarGlassIntensity,
                        navBarTransparency = navBarTransparency,
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewNavBar(
    navBarStyle: String,
    navBarGlassIntensity: Int,
    navBarTransparency: Int,
) {
    val isDarkTheme = LocalIsDarkTheme.current

    val navItems = listOf(
        Icons.Outlined.Home to stringResource(R.string.nav_label_home),
        Icons.Outlined.FolderOpen to stringResource(R.string.nav_label_files),
        Icons.Outlined.Schedule to stringResource(R.string.nav_label_history),
        Icons.Outlined.Settings to stringResource(R.string.nav_label_settings),
    )

    // 选中第 0 项作为预览高亮（与实际 MdcitoBottomNavItem 的布局保持一致：
    // 图标 24dp、文字 12sp、选中项 Box(56×32dp) + secondaryContainer 背景）
    val contentRow: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navItems.forEachIndexed { index, (icon, label) ->
                val isSelected = index == 0
                val unselectedColor = LocalContentColor.current
                val iconColor = if (isSelected) MaterialTheme.colorScheme.primary else unselectedColor
                val textColor = if (isSelected) MaterialTheme.colorScheme.primary else unselectedColor

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    // 预览容器宽度有限（≈312dp），若用固定 padding(horizontal=12.dp)
                    // 4 项 × 80dp = 320dp > 312dp 会被圆角裁剪。
                    // 改用 weight(1f) 让每项平均分配宽度，确保内容完整显示且间距均匀。
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(width = 56.dp, height = 32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.size(width = 56.dp, height = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = iconColor,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    Text(
                        text = label,
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.W700 else FontWeight.W400,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }

    when (navBarStyle) {
        "frosted_glass" -> {
            // 磨砂玻璃预览 — 真实背景模糊（.liquidGlass(enableLens = false)）
            // 与实际 MdcitoBottomNavBar 一致：顶部圆角 16dp、深色模式叠加层
            val frostedShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            val intensityFactor = (navBarGlassIntensity / 300f).coerceIn(0f, 1f)
            val overlayAlphaBoost = intensityFactor * 0.30f
            val containerColor = if (isDarkTheme) Color(0xFF282C38) else Color(0xFFF9F7F4)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        enabled = true,
                        shape = frostedShape,
                        containerColor = containerColor,
                        shadowElevation = if (isDarkTheme) 6.dp else 3.dp,
                        borderWidth = 1.dp,
                        blurRadius = (10f + 20f * intensityFactor).dp,
                        overlayAlphaBoost = overlayAlphaBoost,
                        enableLens = false,
                    )
                    .clip(frostedShape),
            ) {
                // 深色模式内容可读性增强（与实际 MdcitoBottomNavBar 一致）
                if (isDarkTheme) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Color(0xFF1E2028).copy(alpha = 0.22f),
                                frostedShape,
                            )
                    )
                }
                val previewContentColor = MdcitoCardDefaults.glassContentColor()
                CompositionLocalProvider(LocalContentColor provides previewContentColor) {
                    contentRow()
                }
            }
        }
        "liquid_glass" -> {
            // 液态水玻璃预览 — 水玻璃（真实流体玻璃 + 折射色散）
            // 产品"液态水玻璃"= 水玻璃, 仅 .waterGlass(), 不堆叠 liquidGlass 以保留折射/色散
            // 与实际 MdcitoBottomNavBar 一致：顶部圆角 28dp、深色模式叠加层
            val liquidShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            val containerColor = if (isDarkTheme) Color(0xFF1C2030) else Color(0xFFF4EDE6)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .waterGlass(
                        enabled = true,
                        shape = liquidShape,
                        containerColor = containerColor,
                        shadowElevation = if (isDarkTheme) 14.dp else 10.dp,
                        borderWidth = 0.7.dp,
                        overlayAlphaBoost = 0f,
                    )
                    .clip(liquidShape),
            ) {
                // 深色模式内容可读性增强（与实际 MdcitoBottomNavBar 一致）
                if (isDarkTheme) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Color(0xFF141824).copy(alpha = 0.20f),
                                liquidShape,
                            )
                    )
                }
                val previewContentColor = MdcitoCardDefaults.glassContentColor()
                CompositionLocalProvider(LocalContentColor provides previewContentColor) {
                    contentRow()
                }
            }
        }
        else -> {
            // minimal 预览（与实际一致：顶部圆角 16dp）
            val minimalShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(minimalShape)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 1.0f - (navBarTransparency / 100f)),
                    ),
            ) {
                contentRow()
            }
        }
    }
}
