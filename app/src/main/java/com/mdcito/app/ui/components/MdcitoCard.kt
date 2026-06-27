package com.mdcito.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.mdcito.app.R
import com.mdcito.app.ui.components.glass.liquidGlass
import com.mdcito.app.ui.components.glass.waterGlass
import com.mdcito.app.ui.theme.LocalIsDarkTheme

enum class CardStyle { MINIMAL, FROSTED_GLASS, LIQUID_GLASS }

val LocalCardStyle = compositionLocalOf { CardStyle.MINIMAL }
val LocalCardGlassIntensity = compositionLocalOf { 50 }
val LocalCardTransparency = compositionLocalOf { 100 }



object MdcitoCardDefaults {
    val CornerSize = 12.dp
    val ContentPadding = 16.dp
    val CompactContentPadding = 14.dp
    val CardSpacing = 8.dp
    val SectionSpacing = 20.dp

    @Composable
    fun cardColors() = androidx.compose.material3.CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
    )

    @Composable
    fun containerColor(alpha: Float = 1f) =
        MaterialTheme.colorScheme.surface.copy(alpha = alpha)

    @Composable
    fun borderColor(alpha: Float = 0.15f) =
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha)

    @Composable
    fun iconTint() = MaterialTheme.colorScheme.onSurfaceVariant

    @Composable
    fun primaryIconTint() = MaterialTheme.colorScheme.primary

    @Composable
    fun subtleTextColor() = MaterialTheme.colorScheme.onSurfaceVariant

    @Composable
    fun surfaceShape() = RoundedCornerShape(CornerSize)

    // ═══ 玻璃感知文字颜色（日式简约：深色模式下使用白色，确保在玻璃光影下清晰可读）═══

    /**
     * 玻璃卡片内主要文字颜色。
     * 深色模式下使用白色，日式简约风格。
     */
    @Composable
    fun glassContentColor(): Color {
        val isDark = LocalIsDarkTheme.current
        val cardStyle = LocalCardStyle.current
        if (!isDark) return MaterialTheme.colorScheme.onSurface
        return when (cardStyle) {
            CardStyle.LIQUID_GLASS -> Color.White
            CardStyle.FROSTED_GLASS -> Color.White
            else -> MaterialTheme.colorScheme.onSurface
        }
    }

    /**
     * 玻璃卡片内次要文字颜色。
     * 深色模式下使用高亮度白色（0.85 alpha），日式简约风格。
     */
    @Composable
    fun glassSubtleContentColor(): Color {
        val isDark = LocalIsDarkTheme.current
        val cardStyle = LocalCardStyle.current
        if (!isDark) return MaterialTheme.colorScheme.onSurfaceVariant
        return when (cardStyle) {
            CardStyle.LIQUID_GLASS -> Color.White.copy(alpha = 0.85f)
            CardStyle.FROSTED_GLASS -> Color.White.copy(alpha = 0.85f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    /**
     * 玻璃卡片内图标颜色。
     * 深色模式下使用白色（0.85 alpha），与次要文字保持统一。
     */
    @Composable
    fun glassIconTint(): Color = glassSubtleContentColor()

    /**
     * 玻璃卡片内极淡辅助元素颜色（如箭头、拖动手柄）。
     * 深色模式下使用白色（0.55 alpha），日式简约风格。
     */
    @Composable
    fun glassFaintContentColor(): Color {
        val isDark = LocalIsDarkTheme.current
        val cardStyle = LocalCardStyle.current
        if (!isDark) return MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        return when (cardStyle) {
            CardStyle.LIQUID_GLASS -> Color.White.copy(alpha = 0.55f)
            CardStyle.FROSTED_GLASS -> Color.White.copy(alpha = 0.55f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        }
    }
}

@Composable
fun MdcitoCard(
    onClick: (() -> Unit)? = null,
    cardStyle: CardStyle = CardStyle.MINIMAL,
    glassIntensity: Int = 5,
    transparency: Int = 100,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    when (cardStyle) {
        CardStyle.MINIMAL -> MinimalCard(
            onClick = onClick,
            modifier = modifier,
            cornerRadius = cornerRadius,
            transparency = transparency,
            content = content,
        )
        CardStyle.FROSTED_GLASS -> FrostedGlassCard(
            onClick = onClick,
            modifier = modifier,
            cornerRadius = cornerRadius,
            glassIntensity = glassIntensity,
            content = content,
        )
        CardStyle.LIQUID_GLASS -> LiquidGlassCard(
            onClick = onClick,
            modifier = modifier,
            cornerRadius = cornerRadius,
            glassIntensity = glassIntensity,
            content = content,
        )
    }
}

@Composable
private fun MinimalCard(
    onClick: (() -> Unit)?,
    modifier: Modifier,
    cornerRadius: Dp,
    transparency: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val cardColor = MaterialTheme.colorScheme.surface.copy(alpha = 1.0f - (transparency / 100f))
    val borderColor = if (isDark) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }
    val shape = RoundedCornerShape(cornerRadius)

    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = shape,
            color = cardColor,
            border = BorderStroke(1.dp, borderColor),
            tonalElevation = 1.dp,
            modifier = modifier,
            content = { Column(content = content) },
        )
    } else {
        Surface(
            shape = shape,
            color = cardColor,
            border = BorderStroke(1.dp, borderColor),
            tonalElevation = 0.dp,
            modifier = modifier,
            content = { Column(content = content) },
        )
    }
}

/**
 * 磨砂玻璃卡片 — 真实背景模糊 + 折射
 *
 * 基于 kyant backdrop 的 .liquidGlass() 修饰符，启用 enableLens=false
 * 即经典的磨砂玻璃效果（模糊 + 振动饱和 + 边缘高光 + 投影 + 表面色调）。
 *
 * glassIntensity (0-300) 控制叠加色调浓度（overlayAlphaBoost）和投影高度，
 * 越大玻璃越"厚"。
 *
 * 架构：
 * - .liquidGlass(enableLens = false) 真实背景模糊 + 视觉效果
 * - 深色模式: 内容可读性增强层
 * - 内容层
 */
@Composable
private fun FrostedGlassCard(
    onClick: (() -> Unit)?,
    modifier: Modifier,
    cornerRadius: Dp,
    glassIntensity: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val shape = RoundedCornerShape(cornerRadius)

    // ═══ glassIntensity (0-300) → overlayAlphaBoost (0-0.30) ═══
    val intensityFactor = (glassIntensity / 300f).coerceIn(0f, 1f)
    val overlayAlphaBoost = intensityFactor * 0.30f

    // ═══ 容器基础色（用于明暗判断和色调生成）═══
    val containerColor = if (isDark) Color(0xFF282C38) else Color(0xFFF9F7F4)

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .liquidGlass(
                enabled = true,
                shape = shape,
                containerColor = containerColor,
                shadowElevation = if (isDark) 6.dp else 3.dp,
                borderWidth = 1.dp,
                blurRadius = (10f + 20f * intensityFactor).dp,  // 10-30dp，受强度控制
                overlayAlphaBoost = overlayAlphaBoost,
                enableLens = false,  // 磨砂玻璃：仅模糊，无透镜折射
            )
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        role = Role.Button,
                    ) { onClick() }
                } else Modifier
            ),
    ) {
        // ═══ 深色模式内容可读性增强（日式简约：清晰层次，确保文字在玻璃光影下可读）═══
        if (isDark) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Color(0xFF1E2028).copy(alpha = 0.22f),
                        shape,
                    )
            )
        }

        // ═══ 内容层（深色模式下覆盖内容颜色为白色）═══
        val contentColor = MdcitoCardDefaults.glassContentColor()
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
        ) {
            Column(
                modifier = Modifier.padding(0.dp),
                content = content,
            )
        }
    }
}

/**
 * 液态玻璃卡片 — 水玻璃（真实流体玻璃 + 折射色散）
 *
 * 产品中的"液态玻璃"即技能 liquid-glass-design 中的"水玻璃"(Implementation B,
 * 基于 fletchmckee liquid 库)。仅使用 .waterGlass() 修饰符，呈现纯净的水玻璃流体效果：
 *   - frost (霜化): 磨砂不透明度
 *   - curve (曲率): 边缘曲率，模拟水表面张力
 *   - refraction (折射): 光线偏折
 *   - dispersion (色散): 彩虹分光
 *   - saturation (饱和度): 色彩增强
 *   - contrast (对比度): 明暗增强
 *
 * 注意: 不再堆叠 .liquidGlass()。原因: 在 Compose 绘制顺序中,
 * .liquidGlass() 的 drawBackdrop 会在 .waterGlass() 的 liquid() 流体输出之上
 * 绘制一层背景采样图，覆盖水玻璃的折射/色散效果，导致"液态玻璃效果未实现"。
 *
 * glassIntensity (0-300) 控制叠加色调浓度 (overlayAlphaBoost)，越大水玻璃越"浓"。
 *
 * 架构：
 * - .waterGlass() 真实水玻璃流体效果
 * - 深色模式: 内容可读性增强层
 * - 内容层
 */
@Composable
private fun LiquidGlassCard(
    onClick: (() -> Unit)?,
    modifier: Modifier,
    cornerRadius: Dp,
    glassIntensity: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    // 超大圆角 — 胶囊形液态轮廓
    val liquidCornerRadius = maxOf(cornerRadius, 28.dp)
    val shape = RoundedCornerShape(liquidCornerRadius)

    // ═══ 容器基础色（用于明暗判断和色调生成）═══
    val containerColor = if (isDark) Color(0xFF1C2030) else Color(0xFFF4EDE6)

    // ═══ glassIntensity (0-300) → overlayAlphaBoost (0-0.30) ═══
    val intensityFactor = (glassIntensity / 300f).coerceIn(0f, 1f)
    val overlayAlphaBoost = intensityFactor * 0.30f

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            // ── 仅 waterGlass（水玻璃流体层）──
            // 产品"液态玻璃"= 水玻璃, 不堆叠 liquidGlass 以保留折射/色散效果
            .waterGlass(
                enabled = true,
                shape = shape,
                containerColor = containerColor,
                shadowElevation = if (isDark) 14.dp else 10.dp,
                borderWidth = 0.7.dp,
                overlayAlphaBoost = overlayAlphaBoost,
            )
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        role = Role.Button,
                    ) { onClick() }
                } else Modifier
            ),
    ) {
        // ═══ 深色模式内容可读性增强（日式简约：清晰层次，确保文字在光影效果下可读）═══
        if (isDark) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Color(0xFF141824).copy(alpha = 0.20f),
                        shape,
                    )
            )
        }

        // ═══ 内容层（深色模式下覆盖内容颜色为白色）═══
        val contentColor = MdcitoCardDefaults.glassContentColor()
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
        ) {
            Column(
                modifier = Modifier.padding(0.dp),
                content = content,
            )
        }
    }
}

@Composable
fun MdcitoSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    val borderColor = if (isDark) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.height(44.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = stringResource(R.string.search),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.clear),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onQueryChange("") },
                )
            }
        }
    }
}
