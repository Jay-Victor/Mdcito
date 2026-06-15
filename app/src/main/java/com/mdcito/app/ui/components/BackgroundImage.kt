package com.mdcito.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * 可复用的背景图片组件
 * 支持高斯模糊和亮度调节
 *
 * @param imageUri 图片 URI 字符串，null 时不渲染
 * @param blurEnabled 是否启用模糊
 * @param blurIntensity 模糊强度 (1-25 dp)
 * @param brightness 亮度百分比 (20-150, 100 为原始亮度)
 */
@Composable
fun BackgroundImage(
    imageUri: String?,
    blurEnabled: Boolean,
    blurIntensity: Int,
    brightness: Int,
    modifier: Modifier = Modifier,
) {
    if (imageUri == null) return

    // 构建亮度 ColorFilter
    val brightnessFilter = remember(brightness) {
        val b = brightness / 100f
        val matrix = floatArrayOf(
            b, 0f, 0f, 0f, 0f,
            0f, b, 0f, 0f, 0f,
            0f, 0f, b, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
        ColorMatrix(matrix)
    }

    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = imageUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.colorMatrix(brightnessFilter),
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (blurEnabled && blurIntensity > 0) {
                        Modifier.blur(
                            radiusX = blurIntensity.dp,
                            radiusY = blurIntensity.dp,
                        )
                    } else {
                        Modifier
                    },
                ),
        )
    }
}
