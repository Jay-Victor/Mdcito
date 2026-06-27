package com.mdcito.app.ui.components.glass

// =============================================================================
// 液态玻璃设计 - 核心液态玻璃修饰符 (Liquid Glass Modifier)
// =============================================================================
// 基于 com.kyant.backdrop 库实现"液态玻璃"效果：
//   1. 振动饱和 (vibrancy) - 增强背景色彩饱和度
//   2. 高斯模糊 (blur) - 形成磨砂基底
//   3. 透镜折射 (lens) - 【液态玻璃核心】含色差，模拟光线穿过玻璃
//   4. 边缘高光 (Highlight) - 玻璃边缘的光线反射
//   5. 投影 (Shadow) - 玻璃物体的立体投影
//   6. 表面色调 (onDrawSurface) - 半透明色调叠加
//
// 关键依赖：GlassThemeProvisioning.kt 必须在主题层级装配 Backdrop，
// 否则该修饰符将自动降级为 shadow+border+gloss 静态效果。
// =============================================================================

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

/**
 * 全局液态玻璃 Backdrop 提供者
 *
 * 在 GlassThemeProvisioning 中通过 rememberLayerBackdrop() 创建并绑定，
 * 子组件通过 LocalLiquidGlassBackdrop.current 获取使用。
 */
val LocalLiquidGlassBackdrop = compositionLocalOf<Backdrop?> { null }

/**
 * 液态玻璃最低 API 要求: Android 13 (Tiramisu, API 33)
 */
private const val LiquidGlassMinApi = Build.VERSION_CODES.TIRAMISU

/**
 * 检测当前设备是否支持真实液态玻璃效果
 */
fun isLiquidGlassSupported(): Boolean = Build.VERSION.SDK_INT >= LiquidGlassMinApi

/**
 * 液态玻璃修饰符 -【核心 API】
 *
 * 将任意 Compose 组件转换为液态玻璃质感。
 *
 * @param enabled 是否启用液态玻璃 (false 时直接返回原 Modifier)
 * @param shape 玻璃形状 (CornerBasedShape)
 * @param containerColor 容器基础色 - 用于判断明暗并生成色调叠加
 * @param shadowElevation 投影高度 (dp)，推荐 10-18dp
 * @param borderWidth 边框宽度 (dp)，推荐 0.3-1dp
 * @param blurRadius 模糊半径 (dp)，控制背景模糊强度，推荐 10-30dp
 * @param overlayAlphaBoost 叠加透明度增强 [0, 0.48]
 * @param enableLens 【液态玻璃核心】是否启用透镜折射效果 (关闭则退化为磨砂玻璃)
 */
@Composable
fun Modifier.liquidGlass(
    enabled: Boolean,
    shape: CornerBasedShape = RoundedCornerShape(0.dp),
    containerColor: Color,
    shadowElevation: Dp = 14.dp,
    borderWidth: Dp = 1.dp,
    blurRadius: Dp = 10.dp,
    overlayAlphaBoost: Float = 0f,
    enableLens: Boolean = true,
): Modifier {
    if (!enabled) return this

    val backdrop = if (isLiquidGlassSupported()) LocalLiquidGlassBackdrop.current else null
    val isLightGlass = containerColor.luminance() >= 0.5f

    // =====================================================================
    // 降级路径: API < 33 或 Backdrop 未初始化时，使用静态效果模拟
    // =====================================================================
    if (backdrop == null) {
        val fallbackBorderColor =
            if (isLightGlass) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.16f)
        val fallbackShadow = shadowElevation.coerceAtLeast(10.dp)
        val fallbackSurfaceTint =
            if (isLightGlass) containerColor.copy(alpha = 0.16f) else containerColor.copy(alpha = 0.24f)
        val fallbackGloss =
            if (isLightGlass) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.06f)

        return this
            .shadow(
                elevation = fallbackShadow,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (isLightGlass) 0.10f else 0.18f),
                spotColor = Color.Black.copy(alpha = if (isLightGlass) 0.10f else 0.18f),
            )
            .border(width = borderWidth.coerceAtLeast(0.6.dp), color = fallbackBorderColor, shape = shape)
            .background(color = fallbackSurfaceTint, shape = shape)
            .drawWithContent {
                drawContent()
                drawRect(fallbackGloss)
            }
    }

    // =====================================================================
    // 真实液态玻璃路径: API >= 33 且 Backdrop 可用
    // =====================================================================
    val baseTintAlpha = if (isLightGlass) 0.16f else 0.23f
    val surfaceTint =
        containerColor.copy(alpha = (baseTintAlpha + overlayAlphaBoost).coerceIn(0f, 0.48f))

    val edgeWidth = borderWidth.coerceAtLeast(0.2.dp)
    val shadowRadius = shadowElevation.coerceAtLeast(12.dp)
    val shadowColor =
        if (isLightGlass) Color.Black.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.18f)

    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(blurRadius.toPx())
            if (enableLens) {
                lens(
                    refractionHeight = 12.dp.toPx(),
                    refractionAmount = 18.dp.toPx(),
                    chromaticAberration = true,
                )
            }
        },
        highlight = {
            Highlight(
                width = edgeWidth,
                blurRadius = edgeWidth * 2.4f,
                alpha = if (isLightGlass) 0.62f else 0.50f,
            )
        },
        shadow = {
            Shadow(
                radius = shadowRadius,
                color = shadowColor,
            )
        },
        onDrawSurface = {
            drawRect(surfaceTint)
        },
    )
}
