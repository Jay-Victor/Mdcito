package com.mdcito.app.ui.components

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.mdcito.app.R
import com.mdcito.app.ui.theme.LocalIsDarkTheme
import kotlin.math.max

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

/**
 * 生成磨砂玻璃噪点纹理 — 细腻颗粒感（128×128 随机灰度平铺）
 */
@Composable
internal fun rememberNoiseBitmap(): ImageBitmap {
    return remember {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val random = java.util.Random(42)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val v = random.nextInt(256)
                bitmap.setPixel(x, y, (255 shl 24) or (v shl 16) or (v shl 8) or v)
            }
        }
        bitmap.asImageBitmap()
    }
}

/**
 * 生成磨砂玻璃凹凸浮雕纹理 — 方向光照浮雕
 *
 * 先生成平滑噪声高度图，再用方向光照（左上光源）将其转换为浮雕效果：
 * - 高度向光源方向升高 → 亮（高光）
 * - 高度向光源方向降低 → 暗（阴影）
 */
@Composable
internal fun rememberEmbossBitmap(): ImageBitmap {
    return remember {
        val size = 128
        // Step 1: 生成平滑噪声高度图（多频率叠加，模拟 Perlin 噪声）
        val heightMap = FloatArray(size * size)
        val random = java.util.Random(42)

        // 多频率噪声叠加，产生自然的凹凸分布
        val frequencies = floatArrayOf(4f, 8f, 16f, 32f)
        val amplitudes = floatArrayOf(0.5f, 0.25f, 0.15f, 0.1f)

        for (y in 0 until size) {
            for (x in 0 until size) {
                var h = 0f
                for (f in frequencies.indices) {
                    val fx = x.toFloat() / size * frequencies[f]
                    val fy = y.toFloat() / size * frequencies[f]
                    // 使用 sin/cos 组合产生平滑伪随机值
                    h += amplitudes[f] * (
                        Math.sin(fx * 1.7 + random.nextFloat() * 0.3).toFloat() *
                        Math.cos(fy * 2.3 + random.nextFloat() * 0.3).toFloat() * 0.5f + 0.5f
                    )
                }
                heightMap[y * size + x] = h.coerceIn(0f, 1f)
            }
        }

        // Step 2: 方向光照浮雕 — 光源从左上方照射
        // 对每个像素，计算高度梯度在光照方向上的分量
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val lightDirX = -1f // 光源方向 X（负 = 左侧）
        val lightDirY = -1f // 光源方向 Y（负 = 上方）
        val lightStrength = 2.5f // 浮雕强度

        for (y in 1 until size - 1) {
            for (x in 1 until size - 1) {
                // 计算高度梯度（Sobel 简化版）
                val dx = heightMap[y * size + (x + 1)] - heightMap[y * size + (x - 1)]
                val dy = heightMap[(y + 1) * size + x] - heightMap[(y - 1) * size + x]

                // 点积：梯度在光照方向上的投影
                val dot = dx * lightDirX + dy * lightDirY

                // 映射到 0-255：0.5 为中性灰，>0.5 为高光，<0.5 为阴影
                val emboss = ((0.5f + dot * lightStrength) * 255f).coerceIn(0f, 255f).toInt()

                bitmap.setPixel(x, y, (255 shl 24) or (emboss shl 16) or (emboss shl 8) or emboss)
            }
        }

        // 边缘填充中性灰
        for (i in 0 until size) {
            val mid = (255 shl 24) or (128 shl 16) or (128 shl 8) or 128
            bitmap.setPixel(i, 0, mid)
            bitmap.setPixel(i, size - 1, mid)
            bitmap.setPixel(0, i, mid)
            bitmap.setPixel(size - 1, i, mid)
        }

        bitmap.asImageBitmap()
    }
}

/**
 * 设置窗口背景模糊半径
 * 用于磨砂/液态玻璃卡片 — 模糊窗口背景，使透过半透明卡片看到的背景呈磨砂效果
 */
@Composable
fun applyWindowBlur(blurRadius: Int = 20) {
    val view = LocalView.current
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val activity = view.context as? Activity
        SideEffect {
            activity?.window?.setBackgroundBlurRadius(blurRadius)
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
 * 磨砂玻璃卡片
 *
 * 架构（5 层）：
 * - Layer 1: 半透明基底（垂直渐变）
 * - Layer 2: 径向中心聚光 + 顶部漫反射
 * - Layer 3: 噪点纹理（受"纹理强度"控制，范围 0-300）
 * - Layer 4: 微弱内阴影
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

    // ═══ 纹理强度：只控制噪点纹理，范围 0-300 ═══
    val intensityFactor = (glassIntensity / 300f).coerceIn(0.01f, 1f)
    val noiseBitmap = rememberNoiseBitmap()

    // ═══ 核心颜色方案 ═══
    val frostedColor = if (isDark) Color(0xFF282C38) else Color(0xFFF9F7F4)

    // ═══ 不透明度（固定值，不再受 blurLevel 控制）═══
    val baseAlpha = if (isDark) 0.65f else 0.70f

    // ═══ 边框 ═══
    val borderColor = if (isDark) {
        Color(0xFF8A8E9C).copy(alpha = 0.50f)
    } else {
        Color.White.copy(alpha = 0.50f)
    }

    // ═══ 交互状态：正确的 interactionSource 确保点击可靠触发 ═══
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isDark) 3.dp else 1.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (isDark) 0.10f else 0.03f),
                spotColor = Color.Black.copy(alpha = if (isDark) 0.05f else 0.01f),
            )
            .clip(shape)
            .border(BorderStroke(1.5.dp, borderColor), shape)
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
                        )
                    ),
                    shape,
                )
        )

        // ═══ Layer 2: 径向中心聚光 + 顶部漫反射 ═══
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    val r = cornerRadius.toPx()

                    // 径向中心聚光 — 凸面漫反射
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

                    // 顶部漫反射
                    val diffuseAlpha = if (isDark) 0.05f else 0.08f
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
                        cornerRadius = CornerRadius(cornerRadius.toPx()),
                    )
                }
        )

        // ═══ Layer 3: 噪点纹理（受"纹理强度"控制，范围 0-300）═══
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    val noiseAlpha = if (isDark) {
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
                    val r = cornerRadius.toPx()
                    val innerShadowAlpha = if (isDark) 0.08f else 0.05f
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

        // ═══ 深色模式内容可读性增强（日式简约：清晰层次，确保文字在光影效果下可读）═══
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

        // ═══ Layer 5: 清晰内容（完全不受背景模糊影响）═══
        // 深色模式下覆盖默认内容颜色为白色（日式简约：确保文字在玻璃光影下清晰可读）
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
 * 液态玻璃卡片
 *
 * 在半透明磨砂基础上融合液态流动性与光学折射，
 * 深色模式下降低高光强度，确保文字可读性（日式简约：内容优先）。
 *
 * 架构（合并绘制，减少 overdraw）：
 * - 基底：半透明磨砂 + 背景色彩渗透（色散折射）
 * - 光影：4层（液面阴影 + 镜面高光 + 棱线反光 + 内表面反射）
 * - 边缘：软渐变边缘 + 液态晕染，无描边
 * - 深色模式: 内容可读性增强层
 * - 内容层
 */
@Composable
private fun LiquidGlassCard(
    onClick: (() -> Unit)?,
    modifier: Modifier,
    cornerRadius: Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    // 超大圆角 — 胶囊形液态轮廓（半径 ≈ 高度的1/4）
    val liquidCornerRadius = maxOf(cornerRadius, 28.dp)
    val shape = RoundedCornerShape(liquidCornerRadius)

    // ═══ 基底色与不透明度 ═══
    // 降低不透明度以配合 Window Blur 实现背景色彩渗透
    // 无 Window Blur 时仍保持足够遮盖力
    val bgColor = if (isDark) Color(0xFF1C2030) else Color(0xFFF4EDE6)
    val baseAlpha = if (isDark) 0.62f else 0.68f

    // ═══ 交互状态：正确的 interactionSource 确保点击可靠触发 ═══
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            // ── 外投影（4层光影之第4层）──
            .shadow(
                elevation = if (isDark) 20.dp else 14.dp,
                shape = shape,
                clip = false,
                ambientColor = Color(0xFF0A0612).copy(alpha = if (isDark) 0.25f else 0.06f),
                spotColor = Color(0xFF0A0612).copy(alpha = if (isDark) 0.18f else 0.04f),
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
        // ═══ 合并绘制层：基底 + 色彩渗透 + 4层光影 + 软渐变边缘 ═══
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    val r = liquidCornerRadius.toPx()
                    val w = size.width
                    val h = size.height

                    // ━━━━━━ 基底质感 ━━━━━━

                    // ── 1. 半透明磨砂基底（垂直渐变，凸面顶部更透）──
                    // 顶部更透 = 凸面最高点（更薄），底部更厚 = 凸面边缘
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
                    // 模拟凸透镜的色散效果 — 中心暖色、边缘冷色偏移
                    val bleedAlpha = if (isDark) 0.12f else 0.09f
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
                    // 对角线二次折射
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

                    // ━━━━━━ 4层光影系统 ━━━━━━

                    // ── 3a. 液面阴影（凸起体积基石）──
                    // 底部暗化 + 对角暗角 + 径向暗角（环境光遮蔽）
                    val shadowAlpha = if (isDark) 0.28f else 0.14f
                    // 垂直暗化 — 凸面底部远离光源
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
                    // 对角暗角
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
                    // 径向暗角（环境光遮蔽 AO）
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

                    // ── 3b. 镜面高光（平面→立体的转换器，最关键！）──
                    // 不规则的亮白色高光，模拟光源在弧形玻璃表面的镜面反射
                    // 温润果冻质感：使用暖白色（0xFFFFFAF4）而非纯白
                    // 深色模式下降低高光强度（日式简约：内容优先，玻璃效果不与文字争夺视觉注意力）
                    val specAlpha = if (isDark) 0.40f else 0.78f
                    // 主高光 — 偏左上方的大面积柔光
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
                    // 次高光 — 右上方小亮点
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

                    // ── 3c. 棱线反光（切割面线性反光）──
                    // 模拟玻璃边缘切割面的线性反光，强化硬质质感
                    // 液态金属质感：锐利的线性高光与柔和的镜面高光形成对比
                    // 深色模式下降低棱线反光（日式简约：克制装饰，突出内容）
                    val edgeAlpha = if (isDark) 0.25f else 0.55f
                    // 顶部棱线
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
                    // 左侧棱线
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

                    // ── 3d. 内表面反射（玻璃内表面微弱反射）──
                    val innerAlpha = if (isDark) 0.10f else 0.25f
                    // 顶部内反射
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
                    // 左侧内反射
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

                    // ━━━━━━ 软渐变边缘（液态晕染，无描边）━━━━━━

                    // ── 4a. 边缘暗晕 — 模拟液体与空气接触的模糊界面 ──
                    val haloAlpha = if (isDark) 0.15f else 0.12f
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

                    // ── 4b. 边缘亮环 — 液态气泡边缘光 ──
                    val rimAlpha = if (isDark) 0.14f else 0.30f
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

        // ═══ 内容层 ═══
        // 深色模式下覆盖默认内容颜色为白色（日式简约：确保文字在玻璃光影下清晰可读）
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
