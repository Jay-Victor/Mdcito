package com.mdcito.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.mdcito.app.ui.theme.LocalIsDarkTheme
import kotlin.math.max
import com.mdcito.app.R
import com.mdcito.app.ui.navigation.Route
import com.mdcito.app.ui.navigation.bottomNavRoutes
import timber.log.Timber

data class BottomNavItem(
    val route: Route,
    val icon: ImageVector,
    val label: String,
)

@Composable
fun MdcitoBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    navBarStyle: String = "minimal",
    navBarTransparency: Int = 100,
    navBarGlassIntensity: Int = 5,
) {
    val items = listOf(
        BottomNavItem(Route.Home, Icons.Outlined.Home, stringResource(R.string.home)),
        BottomNavItem(Route.Files, Icons.Outlined.FolderOpen, stringResource(R.string.files)),
        BottomNavItem(Route.History, Icons.Outlined.Schedule, stringResource(R.string.history)),
        BottomNavItem(Route.Settings, Icons.Outlined.Settings, stringResource(R.string.settings)),
    )

    val isDarkTheme = LocalIsDarkTheme.current

    val contentRow: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route.route
                MdcitoBottomNavItem(
                    item = item,
                    isSelected = isSelected,
                    onClick = {
                        val tabName = when (item.route) {
                            Route.Home -> "首页"
                            Route.Files -> "文件"
                            Route.History -> "历史"
                            Route.Settings -> "设置"
                            else -> item.route.route
                        }
                        Timber.tag("Navigation").d("切换到$tabName")
                        onNavigate(item.route.route)
                    },
                )
            }
        }
    }

    val navBarShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

    when (navBarStyle) {
        "frosted_glass" -> {
            // 磨砂玻璃导航栏
            val intensityFactor = (navBarGlassIntensity / 300f).coerceIn(0.01f, 1f)
            val noiseBitmap = rememberNoiseBitmap()

            val frostedColor = if (isDarkTheme) Color(0xFF282C38) else Color(0xFFF9F7F4)

            // 不透明度（固定值）
            val baseAlpha = if (isDarkTheme) 0.65f else 0.70f

            // 边框颜色
            val borderColor = if (isDarkTheme) {
                Color(0xFF8A8E9C).copy(alpha = 0.50f)
            } else {
                Color.White.copy(alpha = 0.50f)
            }

            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = if (isDarkTheme) 3.dp else 1.dp,
                        shape = navBarShape,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.10f else 0.03f),
                        spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.05f else 0.01f),
                    )
                    .clip(navBarShape)
                    .border(BorderStroke(1.5.dp, borderColor), navBarShape)
            ) {
                // ═══ Layer 1: 半透明基底（垂直渐变）═══
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
                            navBarShape,
                        )
                )

                // ═══ Layer 2: 径向中心聚光 + 顶部漫反射 ═══
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            val r = 16.dp.toPx()

                            // 径向中心聚光
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

                            // 微弱顶部漫反射高光
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

                // ═══ Layer 3: 噪点纹理（受 navBarGlassIntensity 控制，范围 0-300）═══
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

                // ═══ Layer 4: 微弱内阴影（右下方向）═══
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            val r = 16.dp.toPx()
                            val innerShadowAlpha = if (isDarkTheme) 0.08f else 0.05f
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color(0xFF000812).copy(alpha = innerShadowAlpha),
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, size.height),
                                ),
                                cornerRadius = CornerRadius(r),
                            )
                        }
                )

                // 深色模式内容可读性增强
                if (isDarkTheme) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Color(0xFF1E2028).copy(alpha = 0.22f),
                                navBarShape,
                            )
                    )
                }

                // Layer 5: 内容（深色模式下覆盖内容颜色为白色）
                val navContentColor = MdcitoCardDefaults.glassContentColor()
                CompositionLocalProvider(LocalContentColor provides navContentColor) {
                    contentRow()
                }
            }
        }
        "liquid_glass" -> {
            // 液态玻璃导航栏
            val liquidNavBarShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

            val bgColor = if (isDarkTheme) Color(0xFF1C2030) else Color(0xFFF4EDE6)
            val baseAlpha = if (isDarkTheme) 0.62f else 0.68f

            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = if (isDarkTheme) 20.dp else 14.dp,
                        shape = liquidNavBarShape,
                        clip = false,
                        ambientColor = Color(0xFF0A0612).copy(alpha = if (isDarkTheme) 0.25f else 0.06f),
                        spotColor = Color(0xFF0A0612).copy(alpha = if (isDarkTheme) 0.18f else 0.04f),
                    )
                    .clip(liquidNavBarShape)
            ) {
                // ═══ 合并绘制层：基底 + 色彩渗透 + 4层光影 + 软渐变边缘 ═══
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            val r = 28.dp.toPx()
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

                            // ── 2. 背景色彩渗透（折射色散）──
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

                // 深色模式内容可读性增强
                if (isDarkTheme) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Color(0xFF141824).copy(alpha = 0.20f),
                                liquidNavBarShape,
                            )
                    )
                }

                // 深色模式下覆盖内容颜色为白色
                val navContentColor = MdcitoCardDefaults.glassContentColor()
                CompositionLocalProvider(LocalContentColor provides navContentColor) {
                    contentRow()
                }
            }
        }
        else -> {
            // minimal (日式简约)
            val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 1.0f - (navBarTransparency / 100f))
            val hasTransparency = navBarTransparency > 0
            Surface(
                modifier = modifier.fillMaxWidth(),
                shape = navBarShape,
                color = surfaceColor,
                // 日式简约：透明模式下不使用阴影（避免分层感），仅用极淡顶线分隔
                shadowElevation = if (hasTransparency) 0.dp else 3.dp,
            ) {
                Column {
                    // 透明模式下添加微妙顶部分隔线（日式简约：克制、不抢眼）
                    if (hasTransparency) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                ),
                        )
                    }
                    contentRow()
                }
            }
        }
    }
}

@Composable
private fun MdcitoBottomNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val unselectedColor = LocalContentColor.current
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else unselectedColor,
        animationSpec = tween(300),
        label = "navIconColor",
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else unselectedColor,
        animationSpec = tween(300),
        label = "navTextColor",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
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
                    imageVector = item.icon,
                    contentDescription = item.label,
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
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Text(
            text = item.label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.W700 else FontWeight.W400,
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
