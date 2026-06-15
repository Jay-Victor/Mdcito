package com.mdcito.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun MdcitoShimmerBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp),
) {
    val density = LocalDensity.current
    val shimmerDistance = with(density) { 400.dp.toPx() }
    val gradientSpread = with(density) { 150.dp.toPx() }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = shimmerDistance,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )

    val shimmerColor = Color(0xFFB0B0B0)
    val shimmerColorDark = Color(0xFF909090)

    val brush = Brush.linearGradient(
        colors = listOf(
            shimmerColor.copy(alpha = 0.2f),
            shimmerColorDark.copy(alpha = 0.4f),
            shimmerColor.copy(alpha = 0.2f),
        ),
        start = Offset(translateAnim - gradientSpread, translateAnim - gradientSpread),
        end = Offset(translateAnim, translateAnim),
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerColor.copy(alpha = 0.15f))
            .then(
                Modifier.background(brush)
            ),
    )
}

@Composable
fun MdcitoFileCardSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        MdcitoShimmerBox(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(10.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            MdcitoShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            MdcitoShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(12.dp),
            )
        }
        MdcitoShimmerBox(
            modifier = Modifier.size(24.dp),
            shape = CircleShape,
        )
    }
}

@Composable
fun MdcitoFileListSkeleton(count: Int = 5) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(List(count) { it }) {
            MdcitoCard(
                cardStyle = LocalCardStyle.current,
                glassIntensity = LocalCardGlassIntensity.current,
                transparency = LocalCardTransparency.current,
            ) {
                MdcitoFileCardSkeleton()
            }
        }
    }
}

@Composable
fun MdcitoSettingsSkeleton() {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MdcitoShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        repeat(6) {
            MdcitoShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            )
        }
    }
}

@Composable
fun MdcitoHomeSkeleton() {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MdcitoShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(28.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(4) {
                MdcitoShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp),
                )
            }
        }
        MdcitoShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        )
        repeat(3) {
            MdcitoShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
            )
        }
    }
}
