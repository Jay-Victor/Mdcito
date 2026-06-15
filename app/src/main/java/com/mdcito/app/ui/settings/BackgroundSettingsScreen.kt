@file:Suppress("DEPRECATION")

package com.mdcito.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.mdcito.app.R
import com.mdcito.app.data.locale.LanguageHelper
import com.mdcito.app.ui.components.MdcitoCardDefaults

@Composable
fun BackgroundSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    LanguageHelper.LocalLocaleVersion.current

    val backgroundImageUri by viewModel.backgroundImageUri.collectAsState()
    val backgroundBlur by viewModel.backgroundBlur.collectAsState()
    val backgroundBlurIntensity by viewModel.backgroundBlurIntensity.collectAsState()
    val backgroundBrightness by viewModel.backgroundBrightness.collectAsState()

    val editorBackgroundImageUri by viewModel.editorBackgroundImageUri.collectAsState()
    val editorBackgroundBlur by viewModel.editorBackgroundBlur.collectAsState()
    val editorBackgroundBlurIntensity by viewModel.editorBackgroundBlurIntensity.collectAsState()
    val editorBackgroundBrightness by viewModel.editorBackgroundBrightness.collectAsState()

    // 裁剪目标：跟踪当前正在裁剪的是哪个背景
    var croppingTarget by remember { mutableStateOf<String?>(null) }

    // 图片裁剪启动器 — CanHub 内置图片选择 + 裁剪，一步完成
    @Suppress("DEPRECATION")
    val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uri = result.uriContent?.toString()
            when (croppingTarget) {
                "app" -> viewModel.setBackgroundImageUri(uri)
                "editor" -> viewModel.setEditorBackgroundImageUri(uri)
            }
        }
        croppingTarget = null
    }

    // 启动裁剪（含图片选择）的辅助函数
    @Suppress("DEPRECATION")
    val launchCrop: (String) -> Unit = { target ->
        croppingTarget = target
        cropImageLauncher.launch(
            CropImageContractOptions(
                uri = null, // null 表示使用内置图片选择器
                cropImageOptions = CropImageOptions(
                    // 内置选择器只显示相册，不显示相机
                    imageSourceIncludeGallery = true,
                    imageSourceIncludeCamera = false,
                    // 裁剪界面设置
                    guidelines = CropImageView.Guidelines.ON,
                    autoZoomEnabled = true,
                    multiTouchEnabled = true,
                    fixAspectRatio = false,
                    cropShape = CropImageView.CropShape.RECTANGLE,
                ),
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        SettingsTopBar(
            title = stringResource(R.string.background_settings),
            onNavigateBack = onNavigateBack,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ===== 软件背景板块 =====
        SettingsGroupTitle(stringResource(R.string.app_background))

        BackgroundImageSection(
            imageUri = backgroundImageUri,
            blurEnabled = backgroundBlur,
            blurIntensity = backgroundBlurIntensity,
            brightness = backgroundBrightness,
            onSelectImage = { launchCrop("app") },
            onRemoveImage = { viewModel.setBackgroundImageUri(null) },
            onBlurToggle = { viewModel.setBackgroundBlur(it) },
            onBlurIntensityChange = { viewModel.setBackgroundBlurIntensity(it) },
            onBrightnessChange = { viewModel.setBackgroundBrightness(it) },
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ===== 编辑器背景板块 =====
        SettingsGroupTitle(stringResource(R.string.editor_background))

        BackgroundImageSection(
            imageUri = editorBackgroundImageUri,
            blurEnabled = editorBackgroundBlur,
            blurIntensity = editorBackgroundBlurIntensity,
            brightness = editorBackgroundBrightness,
            onSelectImage = { launchCrop("editor") },
            onRemoveImage = { viewModel.setEditorBackgroundImageUri(null) },
            onBlurToggle = { viewModel.setEditorBackgroundBlur(it) },
            onBlurIntensityChange = { viewModel.setEditorBackgroundBlurIntensity(it) },
            onBrightnessChange = { viewModel.setEditorBackgroundBrightness(it) },
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun BackgroundImageSection(
    imageUri: String?,
    blurEnabled: Boolean,
    blurIntensity: Int,
    brightness: Int,
    onSelectImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onBlurToggle: (Boolean) -> Unit,
    onBlurIntensityChange: (Int) -> Unit,
    onBrightnessChange: (Int) -> Unit,
) {
    // 选择/更换图片按钮
    SettingsActionItem(
        icon = Icons.Outlined.AddPhotoAlternate,
        title = if (imageUri != null) stringResource(R.string.change_image) else stringResource(R.string.select_image),
        subtitle = if (imageUri != null) stringResource(R.string.crop_image) else stringResource(R.string.app_background_desc),
        onClick = {
            onSelectImage()
        },
    )

    // 图片预览
    if (imageUri != null) {
        Spacer(modifier = Modifier.height(8.dp))

        BackgroundPreviewCard(
            imageUri = imageUri,
            blurEnabled = blurEnabled,
            blurIntensity = blurIntensity,
            brightness = brightness,
        )

        // 移除图片按钮
        Spacer(modifier = Modifier.height(6.dp))
        SettingsActionItem(
            icon = Icons.Outlined.Delete,
            title = stringResource(R.string.remove_image),
            subtitle = "",
            onClick = {
                onRemoveImage()
            },
        )
    } else {
        Spacer(modifier = Modifier.height(8.dp))
        SettingsItemCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.no_background_image),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 高斯模糊开关
    SwitchSetting(
        stringResource(R.string.blur_enabled),
        stringResource(R.string.blur_enabled_desc),
        blurEnabled,
    ) {
        onBlurToggle(it)
    }

    // 模糊强度滑块
    if (blurEnabled) {
        Spacer(modifier = Modifier.height(4.dp))
        SettingsItemCard {
            Column(modifier = Modifier.padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.blur_intensity),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "$blurIntensity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(
                    value = blurIntensity.toFloat(),
                    onValueChange = { onBlurIntensityChange(it.toInt()) },
                    valueRange = 1f..25f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 亮度调节滑块
    SettingsItemCard {
        Column(modifier = Modifier.padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.BrightnessMedium,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.brightness),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = "$brightness%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = brightness.toFloat(),
                onValueChange = { onBrightnessChange(it.toInt()) },
                valueRange = 20f..150f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * 背景图片效果预览卡片
 * 实时展示模糊和亮度效果
 */
@Composable
private fun BackgroundPreviewCard(
    imageUri: String,
    blurEnabled: Boolean,
    blurIntensity: Int,
    brightness: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
    ) {
        // 标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.background_preview),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // 效果标签
            val effects = buildList {
                if (blurEnabled) add(stringResource(R.string.blur_enabled))
                if (brightness != 100) add(stringResource(R.string.brightness))
            }
            if (effects.isNotEmpty()) {
                Text(
                    text = effects.joinToString(" + "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 预览图片区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            // 构建亮度 ColorFilter
            val brightnessFilter = remember(brightness) {
                val b = brightness / 100f
                // 使用 ColorMatrix 调节亮度
                val matrix = floatArrayOf(
                    b, 0f, 0f, 0f, 0f,
                    0f, b, 0f, 0f, 0f,
                    0f, 0f, b, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                )
                androidx.compose.ui.graphics.ColorMatrix(matrix)
            }

            AsyncImage(
                model = imageUri,
                contentDescription = stringResource(R.string.image_preview),
                contentScale = ContentScale.Crop,
                colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(brightnessFilter),
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

            // 叠加渐变遮罩增强可读性提示
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f),
                            ),
                        ),
                    ),
            )

            // 示例文字
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    text = "Mdcito",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                    ),
                    color = Color.White,
                )
                Text(
                    text = "Markdown Editor",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }
    }
}
