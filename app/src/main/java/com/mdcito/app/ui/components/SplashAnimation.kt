package com.mdcito.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mdcito.app.R
import com.mdcito.app.ui.theme.MdcitoColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 开场加载动画 — "墨韵流动" (Ink Flow) 增强版
 *
 * 丰富的装饰元素：
 * - 浮动粒子系统：大小不一的圆点缓慢漂浮
 * - 几何圆环：品牌色圆环旋转展开
 * - 渐变光晕：多层径向渐变营造深度感
 * - 流动曲线：贝塞尔曲线装饰
 * - 品牌名逐字母渐入 + 渐变色
 * - 装饰线条展开
 * - 标语渐入
 * - 整体渐出过渡
 *
 * 总时长约 1.5 秒，遵循行业 1.5 秒法则。
 * 所有动画属性使用 graphicsLayer / Canvas 实现 GPU 加速渲染。
 * 支持 reduceMotion 无障碍模式。
 */

// 浮动粒子数据
private data class FloatingParticle(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val radius: Float,
    val delay: Long,
    val duration: Int,
)

@Composable
fun SplashAnimation(
    isDarkTheme: Boolean,
    reduceMotion: Boolean = false,
    onAnimationFinish: () -> Unit,
) {
    val primaryColor = if (isDarkTheme) MdcitoColors.Dark.primary else MdcitoColors.Light.primary
    val backgroundColor = if (isDarkTheme) MdcitoColors.Dark.background else MdcitoColors.Light.background
    val onBackgroundColor = if (isDarkTheme) MdcitoColors.Dark.onBackground else MdcitoColors.Light.onBackground
    val surfaceVariant = if (isDarkTheme) MdcitoColors.Dark.surfaceContainerHigh else MdcitoColors.Light.surfaceContainerHigh

    // ===== 动画状态 =====
    val exitAlpha = remember { Animatable(1f) }

    // 逐字母动画
    val letterAlphas = remember { List(6) { Animatable(0f) } }
    val letterOffsets = remember { List(6) { Animatable(16f) } }

    // 装饰线条
    val lineScaleX = remember { Animatable(0f) }
    val lineAlpha = remember { Animatable(0f) }

    // 标语
    val taglineAlpha = remember { Animatable(0f) }

    // 几何圆环
    val ringScale = remember { Animatable(0.3f) }
    val ringAlpha = remember { Animatable(0f) }
    val ringRotation = remember { Animatable(0f) }

    // 外层圆环
    val outerRingScale = remember { Animatable(0.2f) }
    val outerRingAlpha = remember { Animatable(0f) }
    val outerRingRotation = remember { Animatable(45f) }

    // 光晕
    val glowScale = remember { Animatable(0.2f) }
    val glowAlpha = remember { Animatable(0f) }

    // 流动曲线
    val curveProgress = remember { Animatable(0f) }
    val curveAlpha = remember { Animatable(0f) }

    // 粒子透明度
    val particleAlpha = remember { Animatable(0f) }

    // 底部渐变条
    val barProgress = remember { Animatable(0f) }
    val barAlpha = remember { Animatable(0f) }

    // 生成浮动粒子
    val particles = remember {
        listOf(
            FloatingParticle(0.15f, 0.25f, 0.18f, 0.20f, 3f, 0L, 1200),
            FloatingParticle(0.82f, 0.18f, 0.78f, 0.14f, 2.5f, 100L, 1000),
            FloatingParticle(0.90f, 0.55f, 0.88f, 0.50f, 2f, 200L, 1100),
            FloatingParticle(0.10f, 0.70f, 0.12f, 0.66f, 3.5f, 150L, 1300),
            FloatingParticle(0.75f, 0.80f, 0.72f, 0.76f, 2f, 300L, 900),
            FloatingParticle(0.25f, 0.85f, 0.28f, 0.82f, 1.5f, 250L, 1100),
            FloatingParticle(0.50f, 0.12f, 0.48f, 0.08f, 2.5f, 50L, 1200),
            FloatingParticle(0.65f, 0.90f, 0.62f, 0.86f, 3f, 350L, 1000),
            FloatingParticle(0.35f, 0.15f, 0.38f, 0.12f, 1.8f, 180L, 1150),
            FloatingParticle(0.88f, 0.35f, 0.85f, 0.30f, 2.2f, 280L, 1050),
            FloatingParticle(0.08f, 0.45f, 0.10f, 0.40f, 2.8f, 120L, 1250),
            FloatingParticle(0.42f, 0.92f, 0.45f, 0.88f, 2f, 320L, 950),
        )
    }

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            letterAlphas.forEach { it.snapTo(1f) }
            letterOffsets.forEach { it.snapTo(0f) }
            lineScaleX.snapTo(1f)
            lineAlpha.snapTo(0.6f)
            taglineAlpha.snapTo(0.7f)
            ringScale.snapTo(1f)
            ringAlpha.snapTo(0.25f)
            ringRotation.snapTo(0f)
            outerRingScale.snapTo(1f)
            outerRingAlpha.snapTo(0.15f)
            outerRingRotation.snapTo(0f)
            glowScale.snapTo(1f)
            glowAlpha.snapTo(0.15f)
            curveProgress.snapTo(1f)
            curveAlpha.snapTo(0.3f)
            particleAlpha.snapTo(0.4f)
            barProgress.snapTo(1f)
            barAlpha.snapTo(0.5f)
            delay(600)
            exitAlpha.animateTo(0f, tween(200))
            onAnimationFinish()
            return@LaunchedEffect
        }

        // ===== Phase 1: 背景装饰渐入 (t=0) =====
        launch { glowAlpha.animateTo(0.18f, tween(800, easing = LinearOutSlowInEasing)) }
        launch { glowScale.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }
        launch { ringAlpha.animateTo(0.25f, tween(600, easing = LinearOutSlowInEasing)) }
        launch { ringScale.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
        launch { outerRingAlpha.animateTo(0.15f, tween(700, easing = LinearOutSlowInEasing)) }
        launch { outerRingScale.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        launch { particleAlpha.animateTo(0.4f, tween(800, easing = LinearOutSlowInEasing)) }

        // ===== Phase 2: 圆环旋转 + 流动曲线 (t=200ms) =====
        delay(200)
        launch { ringRotation.animateTo(-30f, tween(1200, easing = FastOutSlowInEasing)) }
        launch { outerRingRotation.animateTo(-15f, tween(1200, easing = FastOutSlowInEasing)) }
        launch { curveAlpha.animateTo(0.3f, tween(400)) }
        launch { curveProgress.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }

        // ===== Phase 3: 品牌名逐字母渐入 (t=300ms) =====
        delay(100)
        "Mdcito".forEachIndexed { index, _ ->
            launch {
                delay(index * 80L)
                letterAlphas[index].animateTo(1f, tween(250, easing = LinearOutSlowInEasing))
            }
            launch {
                delay(index * 80L)
                letterOffsets[index].animateTo(0f, tween(300, easing = FastOutSlowInEasing))
            }
        }

        // ===== Phase 4: 装饰线条 + 底部渐变条 (t=600ms) =====
        delay(300)
        launch { lineAlpha.animateTo(0.6f, tween(300)) }
        launch { lineScaleX.animateTo(1f, tween(400, easing = FastOutSlowInEasing)) }
        launch { barAlpha.animateTo(0.5f, tween(300)) }
        launch { barProgress.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }

        // ===== Phase 5: 标语渐入 (t=800ms) =====
        delay(200)
        launch { taglineAlpha.animateTo(0.7f, tween(400, easing = LinearOutSlowInEasing)) }

        // ===== Phase 6: 整体渐出 (t=1000ms) =====
        delay(200)
        exitAlpha.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
        onAnimationFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = exitAlpha.value }
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        // ===== 1. 背景光晕 — 多层径向渐变 =====
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2 - 40.dp.toPx())
            val maxRadius = sqrt(size.width * size.width + size.height * size.height) / 2

            // 外层大光晕
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.12f * glowAlpha.value),
                        primaryColor.copy(alpha = 0.04f * glowAlpha.value),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = maxRadius * glowScale.value,
                ),
                radius = maxRadius * glowScale.value,
                center = center,
            )

            // 内层聚焦光晕
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.20f * glowAlpha.value),
                        primaryColor.copy(alpha = 0.06f * glowAlpha.value),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = 200.dp.toPx() * glowScale.value,
                ),
                radius = 200.dp.toPx() * glowScale.value,
                center = center,
            )
        }

        // ===== 2. 浮动粒子系统 =====
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                val progress = curveProgress.value
                val x = lerp(p.startX, p.endX, progress) * size.width
                val y = lerp(p.startY, p.endY, progress) * size.height
                drawCircle(
                    color = primaryColor.copy(alpha = particleAlpha.value * 0.6f),
                    radius = p.radius.dp.toPx(),
                    center = Offset(x, y),
                )
            }
        }

        // ===== 3. 几何圆环装饰 =====
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2 - 20.dp.toPx())

            // 内层圆环 — 虚线风格
            val innerRadius = 100.dp.toPx() * ringScale.value
            drawArc(
                color = primaryColor.copy(alpha = ringAlpha.value),
                startAngle = ringRotation.value,
                sweepAngle = 280f,
                useCenter = false,
                topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                size = androidx.compose.ui.geometry.Size(innerRadius * 2, innerRadius * 2),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
            )

            // 外层圆环 — 点状风格
            val outerRadius = 140.dp.toPx() * outerRingScale.value
            val dotCount = 36
            for (i in 0 until dotCount) {
                val angle = Math.toRadians((outerRingRotation.value + i * (360.0 / dotCount)).toDouble())
                val dx = center.x + outerRadius * cos(angle).toFloat()
                val dy = center.y + outerRadius * sin(angle).toFloat()
                val dotAlpha = if (i % 3 == 0) outerRingAlpha.value else outerRingAlpha.value * 0.3f
                drawCircle(
                    color = primaryColor.copy(alpha = dotAlpha),
                    radius = 1.5.dp.toPx(),
                    center = Offset(dx, dy),
                )
            }
        }

        // ===== 4. 流动曲线装饰 =====
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val progress = curveProgress.value

            // 左侧曲线
            val leftPath = Path().apply {
                moveTo(0f, h * 0.3f)
                cubicTo(
                    w * 0.15f * progress, h * 0.25f,
                    w * 0.1f * progress, h * 0.45f,
                    0f, h * 0.5f,
                )
            }
            drawPath(
                path = leftPath,
                color = primaryColor.copy(alpha = curveAlpha.value * 0.5f),
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round),
            )

            // 右侧曲线
            val rightPath = Path().apply {
                moveTo(w, h * 0.5f)
                cubicTo(
                    w - w * 0.12f * progress, h * 0.55f,
                    w - w * 0.08f * progress, h * 0.7f,
                    w, h * 0.75f,
                )
            }
            drawPath(
                path = rightPath,
                color = primaryColor.copy(alpha = curveAlpha.value * 0.4f),
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        // ===== 5. 中心内容 =====
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // 品牌名 — 逐字母动画 + 渐变色
            Row(horizontalArrangement = Arrangement.Center) {
                "Mdcito".forEachIndexed { index, char ->
                    val brush = Brush.linearGradient(
                        colors = listOf(
                            primaryColor,
                            primaryColor.copy(alpha = 0.7f),
                            onBackgroundColor,
                        ),
                    )
                    Text(
                        text = char.toString(),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        style = androidx.compose.ui.text.TextStyle(
                            brush = brush,
                        ),
                        modifier = Modifier.graphicsLayer {
                            translationY = letterOffsets[index].value
                            alpha = letterAlphas[index].value
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 装饰线条
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 2.dp)
                    .graphicsLayer {
                        scaleX = lineScaleX.value
                        alpha = lineAlpha.value
                    }
                    .background(primaryColor.copy(alpha = 0.5f)),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 标语
            Text(
                text = stringResource(R.string.focus_writing),
                color = onBackgroundColor.copy(alpha = 0.5f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 6.sp,
                modifier = Modifier.graphicsLayer { alpha = taglineAlpha.value },
            )
        }

        // ===== 6. 底部渐变进度条 =====
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barWidth = size.width * 0.3f * barProgress.value
            val barX = (size.width - barWidth) / 2
            val barY = size.height - 80.dp.toPx()

            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryColor.copy(alpha = 0.3f * barAlpha.value),
                        primaryColor.copy(alpha = 0.5f * barAlpha.value),
                        primaryColor.copy(alpha = 0.3f * barAlpha.value),
                        Color.Transparent,
                    ),
                ),
                topLeft = Offset(barX, barY),
                size = androidx.compose.ui.geometry.Size(barWidth, 2.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()),
            )
        }
    }
}

private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}
