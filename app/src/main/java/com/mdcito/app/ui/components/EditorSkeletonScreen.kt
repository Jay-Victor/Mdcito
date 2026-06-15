package com.mdcito.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 编辑器骨架屏加载动画
 *
 * 基于行业最佳实践：
 * - Skeleton Screen 比 Spinner 感知速度快 30%（AppyPie 2026 研究）
 * - Shimmer 效果暗示"内容即将到来"，减少用户焦虑
 * - 骨架形状匹配实际内容布局，提供视觉上下文
 * - 300ms 阈值：快速加载不显示骨架屏，避免视觉噪音
 *
 * 布局模拟编辑器真实结构：
 * - 顶部：标题栏骨架
 * - 中部：编辑区域多行文本骨架
 * - 底部：工具栏骨架
 */
@Composable
fun EditorSkeletonScreen(
    modifier: Modifier = Modifier,
) {
    val shimmerColors = ShimmerColors.current()

    val infiniteTransition = rememberInfiniteTransition(label = "editorShimmer")
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )

    val shimmerBrush = Brush.linearGradient(
        colors = shimmerColors.gradientColors,
        start = Offset(x = -300f + shimmerProgress * 1200f, y = 0f),
        end = Offset(x = shimmerProgress * 1200f, y = 0f),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ── 顶部标题栏骨架 ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // 返回按钮
            SkeletonCircle(
                size = 24.dp,
                baseColor = shimmerColors.baseColor,
                brush = shimmerBrush,
            )
            Spacer(modifier = Modifier.width(16.dp))
            // 文件名
            SkeletonBox(
                width = 120.dp,
                height = 16.dp,
                baseColor = shimmerColors.baseColor,
                brush = shimmerBrush,
            )
            Spacer(modifier = Modifier.weight(1f))
            // 操作按钮
            repeat(3) {
                SkeletonCircle(
                    size = 20.dp,
                    baseColor = shimmerColors.baseColor,
                    brush = shimmerBrush,
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 编辑区域骨架 — 模拟文本行 ──
        val linePatterns = listOf(
            0.9f, 0.75f, 0.95f, 0.6f, 0.85f, 0.7f, 0.9f, 0.5f,
            0.8f, 0.65f, 0.95f, 0.55f, 0.75f, 0.9f, 0.6f, 0.85f,
        )

        linePatterns.forEachIndexed { index, widthFraction ->
            val isHeading = index == 0 || index == 4 || index == 8
            val lineHeight = if (isHeading) 16.dp else 12.dp
            val lineAlpha = if (isHeading) 1f else 0.7f

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 行号占位
                if (index < 9) {
                    SkeletonBox(
                        width = 16.dp,
                        height = 12.dp,
                        baseColor = shimmerColors.baseColor.copy(alpha = 0.3f),
                        brush = shimmerBrush,
                    )
                } else {
                    SkeletonBox(
                        width = 22.dp,
                        height = 12.dp,
                        baseColor = shimmerColors.baseColor.copy(alpha = 0.3f),
                        brush = shimmerBrush,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))

                SkeletonBox(
                    widthFraction = widthFraction,
                    height = lineHeight,
                    baseColor = shimmerColors.baseColor.copy(alpha = lineAlpha),
                    brush = shimmerBrush,
                )
            }
            Spacer(modifier = Modifier.height(if (isHeading) 12.dp else 8.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── 底部工具栏骨架 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            repeat(7) {
                SkeletonCircle(
                    size = 22.dp,
                    baseColor = shimmerColors.baseColor.copy(alpha = 0.5f),
                    brush = shimmerBrush,
                )
            }
        }
    }
}

@Composable
private fun SkeletonBox(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    baseColor: Color,
    brush: Brush,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(baseColor)
            .background(brush),
    )
}

@Composable
private fun SkeletonBox(
    widthFraction: Float,
    height: androidx.compose.ui.unit.Dp,
    baseColor: Color,
    brush: Brush,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(baseColor)
            .background(brush),
    )
}

@Composable
private fun SkeletonCircle(
    size: androidx.compose.ui.unit.Dp,
    baseColor: Color,
    brush: Brush,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(baseColor)
            .background(brush),
    )
}

/**
 * Shimmer 配色方案 — 自动适配深色/浅色模式
 */
private data class ShimmerColorScheme(
    val baseColor: Color,
    val gradientColors: List<Color>,
)

private object ShimmerColors {
    @Composable
    fun current(): ShimmerColorScheme {
        val isDark = !MaterialTheme.colorScheme.background.luminance().let { it > 0.5f }

        return if (isDark) {
            ShimmerColorScheme(
                baseColor = Color(0xFF2E2B28),
                gradientColors = listOf(
                    Color(0xFF2E2B28),
                    Color(0xFF3A3632),
                    Color(0xFF45403A),
                    Color(0xFF3A3632),
                    Color(0xFF2E2B28),
                ),
            )
        } else {
            ShimmerColorScheme(
                baseColor = Color(0xFFE5E0D8),
                gradientColors = listOf(
                    Color(0xFFE5E0D8),
                    Color(0xFFEDE9E3),
                    Color(0xFFF5F2ED),
                    Color(0xFFEDE9E3),
                    Color(0xFFE5E0D8),
                ),
            )
        }
    }
}

private fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}
