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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
import com.mdcito.app.ui.theme.LocalIsDarkTheme
import com.mdcito.app.ui.components.rememberNoiseBitmap
import kotlin.math.max

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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
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
                }
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            MdcitoCard(
                cardStyle = style,
                glassIntensity = cardGlassIntensity,
                transparency = cardTransparency,
                modifier = Modifier.fillMaxWidth(),
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(10.dp))
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
                }
                .padding(12.dp),
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

@Composable
private fun PreviewNavBar(
    navBarStyle: String,
    navBarGlassIntensity: Int,
    navBarTransparency: Int,
) {
    val isDarkTheme = LocalIsDarkTheme.current
    val shape = RoundedCornerShape(10.dp)

    val navItems = listOf(
        Icons.Outlined.Home to stringResource(R.string.nav_label_home),
        Icons.Outlined.FolderOpen to stringResource(R.string.nav_label_files),
        Icons.Outlined.Schedule to stringResource(R.string.nav_label_history),
        Icons.Outlined.Settings to stringResource(R.string.nav_label_settings),
    )

    val contentRow: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navItems.forEachIndexed { index, (icon, label) ->
                val unselectedColor = LocalContentColor.current
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (index == 0) MaterialTheme.colorScheme.primary
                        else unselectedColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = if (index == 0) MaterialTheme.colorScheme.primary
                        else unselectedColor,
                        fontWeight = if (index == 0) FontWeight.W700 else FontWeight.W400,
                    )
                }
            }
        }
    }

    when (navBarStyle) {
        "frosted_glass" -> {
            // 磨砂玻璃预览
            val intensityFactor = (navBarGlassIntensity / 300f).coerceIn(0.01f, 1f)
            val noiseBitmap = rememberNoiseBitmap()  // Bitmap for texture layer

            val frostedColor = if (isDarkTheme) Color(0xFF282C38) else Color(0xFFF9F7F4)

            val baseAlpha = if (isDarkTheme) 0.65f else 0.70f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape),
            ) {
                // Layer 1: 垂直渐变基底
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    frostedColor.copy(alpha = baseAlpha * 0.95f),
                                    frostedColor.copy(alpha = baseAlpha * 1.05f),
                                    frostedColor.copy(alpha = baseAlpha),
                                ),
                            ),
                            shape,
                        )
                )

                // Layer 2: 径向中心聚光 + 微弱顶部漫反射高光
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            val r = 10.dp.toPx()

                            // 2a. 径向中心聚光
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = (baseAlpha * 0.12f).coerceAtMost(0.15f)),
                                        Color.White.copy(alpha = (baseAlpha * 0.04f).coerceAtMost(0.06f)),
                                        Color.Transparent,
                                    ),
                                    center = Offset(size.width * 0.50f, size.height * 0.48f),
                                    radius = max(size.width, size.height) * 0.55f,
                                ),
                                cornerRadius = CornerRadius(r),
                            )

                            // 2b. 微弱顶部漫反射高光
                            val diffuseAlpha = if (isDarkTheme) 0.05f else 0.08f
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = diffuseAlpha),
                                        Color.White.copy(alpha = diffuseAlpha * 0.25f),
                                        Color.Transparent,
                                    ),
                                    startY = 0f,
                                    endY = size.height * 0.25f,
                                ),
                                cornerRadius = CornerRadius(r),
                            )
                        }
                )

                // Layer 3: 细腻噪点纹理（磨砂颗粒感）
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            val noiseAlpha = if (isDarkTheme) {
                                0.025f + 0.225f * intensityFactor    // dark: 0.025 → 0.250
                            } else {
                                0.035f + 0.295f * intensityFactor    // light: 0.035 → 0.330
                            }
                            val bw = noiseBitmap.width.toFloat()
                            val bh = noiseBitmap.height.toFloat()
                            var y = 0f
                            while (y < size.height) {
                                var x = 0f
                                while (x < size.width) {
                                    drawImage(image = noiseBitmap, topLeft = Offset(x, y), alpha = noiseAlpha)
                                    x += bw
                                }
                                y += bh
                            }
                        }
                )

                // ═══ Layer 4: 厚度感层 — 内阴影 + 边缘受光亮环 ═══
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            val r = 10.dp.toPx()
                            val w = size.width
                            val h = size.height

                            // 4a. 内阴影 Inner Shadow（右下方向）
                            val innerShadowAlpha = if (isDarkTheme) 0.12f else 0.08f
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color(0xFF000812).copy(alpha = innerShadowAlpha),
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(w, h),
                                ),
                                cornerRadius = CornerRadius(r),
                            )

                            // 4b. 边缘受光亮环 Rim Light
                            val rimAlpha = if (isDarkTheme) 0.10f else 0.14f
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color.White.copy(alpha = rimAlpha * 0.35f),
                                        Color.White.copy(alpha = rimAlpha),
                                    ),
                                    center = Offset(w / 2f, h / 2f),
                                    radius = max(w, h) * 0.90f,
                                ),
                                cornerRadius = CornerRadius(r),
                            )
                            // 方向叠加
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = rimAlpha * 0.50f),
                                        Color.Transparent,
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(w, h),
                                ),
                                cornerRadius = CornerRadius(r),
                            )

                            // 4c. 底部暗角
                            val bottomAlpha = if (isDarkTheme) 0.05f else 0.03f
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = bottomAlpha),
                                    ),
                                    startY = h * 0.72f,
                                    endY = h,
                                ),
                                cornerRadius = CornerRadius(r),
                            )
                        }
                )

                // Layer 5: 内容（深色模式下覆盖内容颜色为白色）
                val previewContentColor = MdcitoCardDefaults.glassContentColor()
                CompositionLocalProvider(LocalContentColor provides previewContentColor) {
                    contentRow()
                }
            }
        }
        "liquid_glass" -> {
            // 液态玻璃预览
            val liquidShape = RoundedCornerShape(20.dp)

            val bgColor = if (isDarkTheme) Color(0xFF1C2030) else Color(0xFFF4EDE6)
            val baseAlpha = if (isDarkTheme) 0.62f else 0.68f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(liquidShape),
            ) {
                // ═══ 合并绘制层 ═══
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            val r = 20.dp.toPx()
                            val w = size.width
                            val h = size.height

                            // ── 1. 半透明磨砂基底 ──
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        bgColor.copy(alpha = baseAlpha * 0.72f),
                                        bgColor.copy(alpha = baseAlpha * 0.88f),
                                        bgColor.copy(alpha = baseAlpha),
                                    )
                                ),
                                cornerRadius = CornerRadius(r),
                            )

                            // ── 2. 背景色彩渗透 ──
                            val bleedAlpha = if (isDarkTheme) 0.12f else 0.09f
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFF2E8DC).copy(alpha = bleedAlpha * 0.5f),
                                        Color(0xFFE4D8F0).copy(alpha = bleedAlpha * 0.8f),
                                        Color(0xFFB8C8E8).copy(alpha = bleedAlpha * 0.6f),
                                        Color.Transparent,
                                    ),
                                    center = Offset(w * 0.46f, h * 0.42f),
                                    radius = max(w, h) * 0.56f,
                                ),
                                cornerRadius = CornerRadius(r),
                            )
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFF0E0).copy(alpha = bleedAlpha * 0.35f),
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color(0xFFD0D8FF).copy(alpha = bleedAlpha * 0.30f),
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(w, h),
                                ),
                                cornerRadius = CornerRadius(r),
                            )

                            // ── 3a. 液面阴影 ──
                            val shadowAlpha = if (isDarkTheme) 0.28f else 0.14f
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color(0xFF0A0614).copy(alpha = shadowAlpha * 0.45f),
                                        Color(0xFF0A0614).copy(alpha = shadowAlpha * 0.85f),
                                        Color(0xFF0A0614).copy(alpha = shadowAlpha * 1.2f),
                                    ),
                                    startY = h * 0.30f,
                                    endY = h,
                                ),
                                cornerRadius = CornerRadius(r),
                            )
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color(0xFF060308).copy(alpha = shadowAlpha * 0.75f),
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(w, h),
                                ),
                                cornerRadius = CornerRadius(r),
                            )
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color(0xFF080410).copy(alpha = shadowAlpha * 0.35f),
                                        Color(0xFF080410).copy(alpha = shadowAlpha * 0.65f),
                                    ),
                                    center = Offset(w * 0.44f, h * 0.38f),
                                    radius = max(w, h) * 0.68f,
                                ),
                                cornerRadius = CornerRadius(r),
                            )

                            // ── 3b. 镜面高光 ──
                            val specAlpha = if (isDarkTheme) 0.40f else 0.78f
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFFFFF).copy(alpha = specAlpha * 0.92f),
                                        Color(0xFFFFFAF4).copy(alpha = specAlpha * 0.60f),
                                        Color(0xFFF8EDE0).copy(alpha = specAlpha * 0.28f),
                                        Color(0xFFF0DDD0).copy(alpha = specAlpha * 0.08f),
                                        Color.Transparent,
                                    ),
                                    center = Offset(w * 0.30f, h * 0.22f),
                                    radius = max(w, h) * 0.52f,
                                ),
                                cornerRadius = CornerRadius(r),
                            )
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFFFFC).copy(alpha = specAlpha * 0.35f),
                                        Color(0xFFFFF6EE).copy(alpha = specAlpha * 0.12f),
                                        Color.Transparent,
                                    ),
                                    center = Offset(w * 0.76f, h * 0.15f),
                                    radius = max(w, h) * 0.24f,
                                ),
                                cornerRadius = CornerRadius(r),
                            )

                            // ── 3c. 棱线反光 ──
                            val edgeAlpha = if (isDarkTheme) 0.25f else 0.55f
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFFFFFF).copy(alpha = edgeAlpha),
                                        Color(0xFFFFFDF8).copy(alpha = edgeAlpha * 0.52f),
                                        Color(0xFFF8F0E4).copy(alpha = edgeAlpha * 0.14f),
                                        Color.Transparent,
                                    ),
                                    startY = 0f,
                                    endY = h * 0.08f,
                                ),
                                cornerRadius = CornerRadius(r),
                            )
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFFFFFFF).copy(alpha = edgeAlpha * 0.75f),
                                        Color(0xFFFFFDF8).copy(alpha = edgeAlpha * 0.28f),
                                        Color(0xFFF8F0E4).copy(alpha = edgeAlpha * 0.05f),
                                        Color.Transparent,
                                    ),
                                    startX = 0f,
                                    endX = w * 0.08f,
                                ),
                                cornerRadius = CornerRadius(r),
                            )

                            // ── 3d. 内表面反射 ──
                            val innerAlpha = if (isDarkTheme) 0.10f else 0.25f
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFFFFFF).copy(alpha = innerAlpha),
                                        Color(0xFFFFF8F0).copy(alpha = innerAlpha * 0.40f),
                                        Color.Transparent,
                                    ),
                                    startY = 0f,
                                    endY = h * 0.035f,
                                ),
                                cornerRadius = CornerRadius(r),
                            )
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFFFFFFF).copy(alpha = innerAlpha * 0.60f),
                                        Color(0xFFFFF8F0).copy(alpha = innerAlpha * 0.18f),
                                        Color.Transparent,
                                    ),
                                    startX = 0f,
                                    endX = w * 0.04f,
                                ),
                                cornerRadius = CornerRadius(r),
                            )

                            // ── 4a. 边缘暗晕 ──
                            val haloAlpha = if (isDarkTheme) 0.15f else 0.12f
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        bgColor.copy(alpha = haloAlpha * 0.3f),
                                        bgColor.copy(alpha = haloAlpha * 0.7f),
                                        bgColor.copy(alpha = haloAlpha),
                                    ),
                                    center = Offset(w * 0.5f, h * 0.5f),
                                    radius = max(w, h) * 0.52f,
                                ),
                                cornerRadius = CornerRadius(r),
                            )

                            // ── 4b. 边缘亮环 ──
                            val rimAlpha = if (isDarkTheme) 0.14f else 0.30f
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color(0xFFFFFFEE).copy(alpha = rimAlpha * 0.18f),
                                        Color(0xFFFFFFFC).copy(alpha = rimAlpha * 0.50f),
                                        Color(0xFFFFFFFF).copy(alpha = rimAlpha * 0.80f),
                                    ),
                                    center = Offset(w * 0.5f, h * 0.5f),
                                    radius = max(w, h) * 0.90f,
                                ),
                                cornerRadius = CornerRadius(r),
                            )
                        }
                )

                // 深色模式下覆盖内容颜色为白色
                val previewContentColor = MdcitoCardDefaults.glassContentColor()
                CompositionLocalProvider(LocalContentColor provides previewContentColor) {
                    contentRow()
                }
            }
        }
        else -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 1.0f - (navBarTransparency / 100f)),
                    ),
            ) {
                contentRow()
            }
        }
    }
}
