package com.mdcito.app.ui.components.glass

// =============================================================================
// 磨砂玻璃设计 - 玻璃质感芯片组件 (Frosted Glass Chip)
// =============================================================================
// 磨砂玻璃风格的芯片组件，常用于功能切换按钮。
// 设计要素：半透明黑色背景 + 白色细边框 + 选中态高亮 + 颜色动画过渡。
// =============================================================================

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 磨砂玻璃芯片组件
 *
 * @param selected 是否处于选中状态 (影响背景、边框、内容颜色)
 * @param text 芯片文本
 * @param icon 芯片图标
 * @param showIcon 是否显示图标
 * @param showText 是否显示文本 (默认 true)
 * @param iconContentDescription 图标无障碍描述
 * @param onClick 点击回调
 */
@Composable
fun GlassyChip(
    selected: Boolean,
    text: String,
    icon: ImageVector,
    showIcon: Boolean,
    showText: Boolean = true,
    iconContentDescription: String? = null,
    onClick: () -> Unit,
) {
    val accentColor = Color(0xFF00E5FF)

    val backgroundColor by animateColorAsState(
        targetValue = if (selected) Color.Black.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.25f),
        animationSpec = tween(300),
        label = "chipBg",
    )

    val borderColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.15f),
        animationSpec = tween(300),
        label = "chipBorder",
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) accentColor else Color.White.copy(alpha = 0.8f),
        animationSpec = tween(300),
        label = "chipContent",
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showIcon) {
                Icon(
                    imageVector = icon,
                    contentDescription = iconContentDescription,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp),
                )
                if (showText) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
            if (showText) {
                Text(
                    text = text,
                    color = contentColor,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                )
            }
        }
    }
}
