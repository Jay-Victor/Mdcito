package com.mdcito.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.mdcito.app.ui.components.glass.liquidGlass
import com.mdcito.app.ui.components.glass.waterGlass
import com.mdcito.app.ui.theme.LocalIsDarkTheme
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
            // 磨砂玻璃导航栏 — 真实背景模糊
            val intensityFactor = (navBarGlassIntensity / 300f).coerceIn(0f, 1f)
            val overlayAlphaBoost = intensityFactor * 0.30f
            val containerColor = if (isDarkTheme) Color(0xFF282C38) else Color(0xFFF9F7F4)

            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        enabled = true,
                        shape = navBarShape,
                        containerColor = containerColor,
                        shadowElevation = if (isDarkTheme) 6.dp else 3.dp,
                        borderWidth = 1.dp,
                        blurRadius = (10f + 20f * intensityFactor).dp,
                        overlayAlphaBoost = overlayAlphaBoost,
                        enableLens = false,
                    )
                    .clip(navBarShape)
            ) {
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

                // 内容（深色模式下覆盖内容颜色为白色）
                val navContentColor = MdcitoCardDefaults.glassContentColor()
                CompositionLocalProvider(LocalContentColor provides navContentColor) {
                    contentRow()
                }
            }
        }
        "liquid_glass" -> {
            // 液态玻璃导航栏 — 水玻璃（真实流体玻璃 + 折射色散）
            // 产品"液态玻璃"= 水玻璃, 仅 .waterGlass(), 不堆叠 liquidGlass 以保留折射/色散
            val liquidNavBarShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            val containerColor = if (isDarkTheme) Color(0xFF1C2030) else Color(0xFFF4EDE6)

            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .waterGlass(
                        enabled = true,
                        shape = liquidNavBarShape,
                        containerColor = containerColor,
                        shadowElevation = if (isDarkTheme) 14.dp else 10.dp,
                        borderWidth = 0.7.dp,
                        overlayAlphaBoost = 0f,
                    )
                    .clip(liquidNavBarShape)
            ) {
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
