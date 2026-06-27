package com.mdcito.app.ui.components.glass

// =============================================================================
// 磨砂玻璃设计 - 动态发光边框 (Animated Glow Border)
// =============================================================================
// 围绕胶囊形输入框的动态发光边框效果。
// 多层渐变光晕 + 流动相位 + 局部凸起脉冲 + HSV 饱和度提升。
//
// 注意：8 层渐变 + PathMeasure 每帧绘制，GPU 开销较大。
// 仅用于 hero 元素 (每屏 1-2 个)，不可用于列表项。
// =============================================================================

import android.graphics.Path as AndroidPath
import android.graphics.PathMeasure
import android.graphics.RectF
import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * 动态发光边框
 *
 * @param modifier 修饰符 (应使用 matchParentSize 与父容器对齐)
 * @param glowPadding 光晕内边距 (光晕会向外扩展此距离)
 * @param colors 渐变色列表 (将沿边框流动循环)
 */
@Composable
fun AnimatedGlowBorder(
    modifier: Modifier,
    glowPadding: Dp,
    colors: List<Color>,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow_border")
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "glow_flow",
    )

    val bulgePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "glow_bulge",
    )

    val anchors = listOf(0.08f, 0.22f, 0.35f, 0.50f, 0.64f, 0.78f, 0.92f)
    var activeAnchorIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val random = Random(System.currentTimeMillis())
        while (true) {
            delay(1800)
            activeAnchorIndex = random.nextInt(anchors.size)
        }
    }

    fun boostColor(color: Color, saturationBoost: Float, valueBoost: Float): Color {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(color.toArgb(), hsv)
        hsv[1] = (hsv[1] * saturationBoost).coerceIn(0f, 1f)
        hsv[2] = (hsv[2] * valueBoost).coerceIn(0f, 1f)
        return Color(AndroidColor.HSVToColor(hsv)).copy(alpha = color.alpha)
    }

    Canvas(
        modifier = modifier.drawWithCache {
            val paddingPx = glowPadding.toPx()
            val innerRect = Rect(
                left = paddingPx,
                top = paddingPx,
                right = size.width - paddingPx,
                bottom = size.height - paddingPx,
            )
            val baseRadius = innerRect.height / 2f
            val outerSpread = paddingPx * 1.2f
            val layers = 8
            val boostedColors = colors.map { boostColor(it, saturationBoost = 1.45f, valueBoost = 1.35f) }

            val roundRectPath = AndroidPath().apply {
                addRoundRect(
                    RectF(innerRect.left, innerRect.top, innerRect.right, innerRect.bottom),
                    baseRadius,
                    baseRadius,
                    AndroidPath.Direction.CW,
                )
            }
            val pathMeasure = PathMeasure(roundRectPath, true)
            val pathLength = pathMeasure.length

            fun colorAt(t: Float): Color {
                if (colors.isEmpty()) return Color.White
                if (colors.size == 1) return colors.first()
                val value = ((t % 1f) + 1f) % 1f
                val scaled = value * (colors.size - 1)
                val index = floor(scaled).toInt().coerceIn(0, colors.size - 2)
                val localT = scaled - index
                return lerp(colors[index], colors[index + 1], localT)
            }

            onDrawBehind {
                val center = Offset(size.width / 2f, size.height / 2f)
                val angle = flowPhase * 2f * PI.toFloat()
                val gradientStart =
                    Offset(center.x + cos(angle) * size.width, center.y + sin(angle) * size.height)
                val gradientEnd =
                    Offset(center.x - cos(angle) * size.width, center.y - sin(angle) * size.height)

                for (i in 0 until layers) {
                    val progress = i / (layers - 1f)
                    val spread = outerSpread * (1f - progress)
                    val radius = baseRadius + spread
                    val alpha = 0.03f + 0.30f * (progress * progress)
                    val whiteFactor = progress * progress * 0.55f

                    val layerColors = boostedColors.map { color ->
                        lerp(color, Color.White, whiteFactor).copy(alpha = alpha)
                    }

                    val rect = Rect(
                        left = innerRect.left - spread,
                        top = innerRect.top - spread,
                        right = innerRect.right + spread,
                        bottom = innerRect.bottom + spread,
                    )

                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = layerColors,
                            start = gradientStart,
                            end = gradientEnd,
                        ),
                        topLeft = Offset(rect.left, rect.top),
                        size = Size(rect.width, rect.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                    )
                }

                if (anchors.isNotEmpty()) {
                    val t = anchors[activeAnchorIndex.coerceIn(0, anchors.lastIndex)]
                    val pulse = kotlin.math.sin(PI.toFloat() * bulgePhase)
                    val alpha = 0.12f + 0.30f * pulse
                    val baseBulge = 3.0.dp.toPx()
                    val bulgeAmp = 7.0.dp.toPx()
                    val baseOut = 2.0.dp.toPx()
                    val radius = baseBulge + bulgeAmp * pulse

                    val pos = FloatArray(2)
                    val tan = FloatArray(2)
                    pathMeasure.getPosTan(pathLength * t, pos, tan)
                    val tanX = tan[0]
                    val tanY = tan[1]
                    val len = sqrt(tanX * tanX + tanY * tanY).coerceAtLeast(0.0001f)
                    val normal = Offset(-tanY / len, tanX / len)
                    val centerPos = Offset(pos[0], pos[1]) + normal * (baseOut + bulgeAmp * 0.35f * pulse)

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                boostColor(colorAt(t), saturationBoost = 1.55f, valueBoost = 1.45f)
                                    .copy(alpha = alpha),
                                Color.Transparent,
                            ),
                            center = centerPos,
                            radius = radius,
                        ),
                        radius = radius,
                        center = centerPos,
                    )
                }
            }
        },
    ) {}
}
