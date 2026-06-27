package com.mdcito.app.ui.components.glass

// =============================================================================
// 水玻璃设计 - 水玻璃修饰符 (Water Glass Modifier)
// =============================================================================
// 基于 io.github.fletchmckee.liquid 库实现"水玻璃"效果：
//   - frost (霜化): 控制磨砂程度
//   - curve (曲率): 玻璃边缘曲率，模拟水的表面张力
//   - refraction (折射): 光线偏折强度
//   - dispersion (色散): 彩虹分光强度
//   - saturation (饱和度): 色彩增强
//   - contrast (对比度): 明暗增强
//
// 与 LiquidGlass 的区别：
//   - LiquidGlass (kyant backdrop): 侧重透镜折射 (lens)，凸透镜感
//   - WaterGlass (fletchmckee liquid): 侧重流体感，参数更丰富，效果更"水润"
//   - 两者可叠加：先 waterGlass 后 liquidGlass
//
// 关键依赖：GlassThemeProvisioning.kt 必须在主题层级通过 liquefiable() 标记背景层，
// 否则该修饰符将自动降级为 shadow+border+gloss 静态效果。
// =============================================================================

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquid

/**
 * 全局水玻璃状态提供者
 *
 * 在 GlassThemeProvisioning 中通过 rememberLiquidState() 创建，
 * 子组件通过 LocalWaterGlassState.current 获取使用。
 * 仅当 isWaterGlassSupported() 返回 true 时此值非 null。
 */
val LocalWaterGlassState = compositionLocalOf<LiquidState?> { null }

/**
 * 水玻璃最低 API 要求: Android 13 (Tiramisu, API 33)
 */
private const val WaterGlassMinApi = Build.VERSION_CODES.TIRAMISU

/**
 * 检测当前设备是否支持真实水玻璃效果
 */
fun isWaterGlassSupported(): Boolean = Build.VERSION.SDK_INT >= WaterGlassMinApi

/**
 * 水玻璃修饰符 -【核心 API】
 *
 * 将任意 Compose 组件转换为水玻璃质感。
 * 需要在父级通过 liquefiable() 标记背景层，liquid() 才能正确捕获背景。
 *
 * @param enabled 是否启用水玻璃 (false 时直接返回原 Modifier)
 * @param shape 玻璃形状 (Shape)
 * @param containerColor 容器基础色 - 用于判断明暗并生成色调
 * @param shadowElevation 投影高度 (dp)，推荐 10-18dp
 * @param borderWidth 边框宽度 (dp)，推荐 0.7dp
 * @param overlayAlphaBoost 叠加透明度增强 [0, 0.56]
 *
 * 水玻璃内部参数 (根据明暗自动调整)：
 *   - frost: 浅色 6dp / 深色 8dp
 *   - curve: 浅色 0.40 / 深色 0.30
 *   - refraction: 浅色 0.12 / 深色 0.09
 *   - dispersion: 浅色 0.18 / 深色 0.13
 *   - saturation: 浅色 0.40 / 深色 0.32
 *   - contrast: 浅色 1.22 / 深色 1.40
 */
@Composable
fun Modifier.waterGlass(
    enabled: Boolean,
    shape: Shape = RoundedCornerShape(0.dp),
    containerColor: Color,
    shadowElevation: Dp = 14.dp,
    borderWidth: Dp = 1.dp,
    overlayAlphaBoost: Float = 0f,
): Modifier {
    if (!enabled) return this

    val liquidState = if (isWaterGlassSupported()) LocalWaterGlassState.current else null
    val isLightGlass = containerColor.luminance() >= 0.5f

    // =====================================================================
    // 降级路径: API < 33 或 LiquidState 未初始化
    // =====================================================================
    if (liquidState == null) {
        val fallbackTintAlpha = if (isLightGlass) 0.14f else 0.22f
        val fallbackBorder =
            if (isLightGlass) Color.White.copy(alpha = 0.26f) else Color.White.copy(alpha = 0.14f)
        val fallbackGloss =
            if (isLightGlass) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.05f)

        return this
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (isLightGlass) 0.10f else 0.18f),
                spotColor = Color.Black.copy(alpha = if (isLightGlass) 0.10f else 0.18f),
            )
            .border(width = borderWidth.coerceAtLeast(0.6.dp), color = fallbackBorder, shape = shape)
            .background(color = containerColor.copy(alpha = fallbackTintAlpha), shape = shape)
            .drawWithContent {
                drawContent()
                drawRect(fallbackGloss)
            }
    }

    // =====================================================================
    // 真实水玻璃路径: API >= 33 且 LiquidState 可用
    // =====================================================================
    val tintAlpha = if (isLightGlass) 0.09f else 0.16f
    val surfaceTint = containerColor.copy(alpha = (tintAlpha + overlayAlphaBoost).coerceIn(0f, 0.56f))

    val borderColor =
        if (isLightGlass) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.10f)

    val shadowColor =
        if (isLightGlass) Color.Black.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.18f)

    return this
        .shadow(
            elevation = shadowElevation,
            shape = shape,
            clip = false,
            ambientColor = shadowColor,
            spotColor = shadowColor,
        )
        .border(width = borderWidth, color = borderColor, shape = shape)
        .liquid(liquidState) {
            this.shape = shape
            this.frost = if (isLightGlass) 6.dp else 8.dp
            this.curve = if (isLightGlass) 0.40f else 0.30f
            this.refraction = if (isLightGlass) 0.12f else 0.09f
            this.dispersion = if (isLightGlass) 0.18f else 0.13f
            this.saturation = if (isLightGlass) 0.40f else 0.32f
            this.contrast = if (isLightGlass) 1.22f else 1.40f
            this.tint = surfaceTint
        }
}
