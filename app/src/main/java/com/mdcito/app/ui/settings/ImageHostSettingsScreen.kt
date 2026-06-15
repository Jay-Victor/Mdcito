package com.mdcito.app.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mdcito.app.R
import com.mdcito.app.data.locale.LanguageHelper
import com.mdcito.app.data.model.ProviderFieldDefs
import com.mdcito.app.data.model.RenameVariables
import com.mdcito.app.data.model.ResizePresets
import com.mdcito.app.data.model.WatermarkPositions
import com.mdcito.app.ui.components.MdcitoCardDefaults

private fun getFieldLabelResId(labelKey: String): Int = when (labelKey) {
    "field_repo_name" -> R.string.field_repo_name
    "field_branch" -> R.string.field_branch
    "field_upload_path" -> R.string.field_upload_path
    "field_token" -> R.string.field_token
    "field_custom_domain" -> R.string.field_custom_domain
    "field_bucket" -> R.string.field_bucket
    "field_upload_domain" -> R.string.field_upload_domain
    "field_access_key" -> R.string.field_access_key
    "field_secret_key" -> R.string.field_secret_key
    "field_api_token" -> R.string.field_api_token
    "field_access_key_id" -> R.string.field_access_key_id
    "field_access_key_secret" -> R.string.field_access_key_secret
    "field_region" -> R.string.field_region
    "field_qiniu_region" -> R.string.field_qiniu_region
    "field_secret_id" -> R.string.field_secret_id
    "field_client_id" -> R.string.field_client_id
    "field_endpoint" -> R.string.field_endpoint
    "field_method" -> R.string.field_method
    "field_headers" -> R.string.field_headers
    "field_response_rule" -> R.string.field_response_rule
    else -> R.string.field_token
}

// ── 分组卡片内分割线 ──
@Composable
private fun GroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = MdcitoCardDefaults.ContentPadding),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
    )
}

// ── 从属配置缩进容器 ──
@Composable
private fun IndentedContent(content: @Composable ColumnScope.() -> Unit) {
    Column(content = content)
}

// ── 不带卡片的开关行（用于嵌入外层卡片内，避免双重卡片嵌套） ──
@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MdcitoCardDefaults.ContentPadding,
                vertical = MdcitoCardDefaults.CompactContentPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MdcitoCardDefaults.glassSubtleContentColor(),
            )
        }
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = Color.White,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

// ── 不带卡片的内联下拉选择（用于嵌入外层卡片内） ──
@Composable
private fun InlineDropdownItem(
    title: String,
    subtitle: String,
    currentValue: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = MdcitoCardDefaults.ContentPadding)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.W500)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MdcitoCardDefaults.glassSubtleContentColor())
            }
            Text(text = currentValue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(tween(200)),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(tween(150)),
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(vertical = 4.dp),
            ) {
                options.forEachIndexed { index, (label, value) ->
                    val isSelected = currentValue == label
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                        animationSpec = tween(200),
                        label = "dropdown_bg",
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(bgColor)
                            .clickable { onSelect(value); expanded = false }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W400,
                        )
                        if (isSelected) {
                            Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (index < options.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .height(0.5.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        )
                    }
                }
            }
        }
    }
}

// ── 无卡片包装的子选项滑块 ──
@Composable
private fun SubOptionSlider(
    title: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    startLabel: String = "",
    endLabel: String = "",
) {
    Column(
        modifier = Modifier.padding(
            horizontal = MdcitoCardDefaults.ContentPadding,
            vertical = 8.dp,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W500,
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.W600,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (startLabel.isNotEmpty() || endLabel.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = startLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MdcitoCardDefaults.glassSubtleContentColor(),
                )
                Text(
                    text = endLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MdcitoCardDefaults.glassSubtleContentColor(),
                )
            }
        }
    }
}

@Composable
fun ImageHostSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImageHostViewModel = hiltViewModel(),
) {
    LanguageHelper.LocalLocaleVersion.current

    val context = LocalContext.current

    val imageHostEnabled by viewModel.imageHostEnabled.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val activeProfileId by viewModel.activeProfileId.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val processingSettings by viewModel.processingSettings.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isSwitching by viewModel.isSwitching.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()
    val deleteResult by viewModel.deleteResult.collectAsState()
    val editingConfig by viewModel.editingConfig.collectAsState()
    val editingProvider by viewModel.editingProvider.collectAsState()
    val editingName by viewModel.editingName.collectAsState()

    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(activeProfile) {
        viewModel.onActiveProfileChanged(activeProfile)
    }

    LaunchedEffect(saveResult) {
        saveResult?.let { result ->
            Toast.makeText(
                context,
                result.message.ifBlank { if (result.success) context.getString(R.string.saved) else context.getString(R.string.save_failed) },
                Toast.LENGTH_SHORT,
            ).show()
            viewModel.clearSaveResult()
        }
    }

    LaunchedEffect(deleteResult) {
        deleteResult?.let { result ->
            Toast.makeText(
                context,
                result.message.ifBlank { if (result.success) context.getString(R.string.profile_deleted) else context.getString(R.string.profile_delete_failed) },
                Toast.LENGTH_SHORT,
            ).show()
            viewModel.clearDeleteResult()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth > 600.dp) 24.dp else 16.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = horizontalPadding),
        ) {
            SettingsTopBar(
                title = stringResource(R.string.image_host_settings),
                onNavigateBack = onNavigateBack,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── 图片托管开关 ──
            SettingsGroupTitle(stringResource(R.string.image_host_toggle))

            SettingsItemCard {
                SwitchSetting(
                    title = stringResource(R.string.enable_image_host),
                    subtitle = stringResource(R.string.enable_image_host_desc),
                    checked = imageHostEnabled,
                ) {
                    viewModel.setImageHostEnabled(it)
                }
            }

            AnimatedVisibility(
                visible = imageHostEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(24.dp))

                    // ── 方案管理 ──
                    SettingsGroupTitle(stringResource(R.string.profile_management))

                    ProfileDropdownSelector(
                        profiles = profiles,
                        activeProfile = activeProfile,
                        onSelect = { viewModel.switchProfile(it) },
                        isSwitching = isSwitching,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 方案操作按钮卡片（5个按钮：创建/重命名/测试/保存/删除）
                    SettingsItemCard {
                        // 第一行：创建 / 重命名 / 测试上传
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = MdcitoCardDefaults.ContentPadding,
                                    vertical = 2.dp,
                                ),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = { viewModel.createProfile() }) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.create), maxLines = 1)
                            }
                            TextButton(onClick = { showRenameDialog = true }) {
                                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.rename), maxLines = 1)
                            }
                            TextButton(
                                onClick = { viewModel.testConnection() },
                                enabled = !isTesting,
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.test_upload), maxLines = 1)
                            }
                        }

                        // 第二行：保存配置 / 删除
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = MdcitoCardDefaults.ContentPadding,
                                    vertical = 2.dp,
                                ),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = { viewModel.saveCurrentProfile() },
                                enabled = !isSaving,
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.save_config), maxLines = 1)
                            }
                            if (profiles.size > 1 && activeProfile != null) {
                                Spacer(modifier = Modifier.width(16.dp))
                                TextButton(
                                    onClick = { showDeleteDialog = true },
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.delete), maxLines = 1, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        // 测试结果指示器
                        AnimatedVisibility(
                            visible = testResult != null,
                            enter = fadeIn(tween(200)) + expandVertically(),
                            exit = fadeOut(tween(200)) + shrinkVertically(),
                        ) {
                            if (testResult != null) {
                                GroupDivider()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = MdcitoCardDefaults.ContentPadding,
                                            vertical = 10.dp,
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = if (testResult!!.success) Icons.Outlined.Check else Icons.Outlined.Close,
                                        contentDescription = null,
                                        tint = if (testResult!!.success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = testResult!!.message.ifBlank {
                                            if (testResult!!.success) stringResource(R.string.test_passed) else stringResource(R.string.test_failed)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (testResult!!.success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── 服务商与配置 ──
                    SettingsGroupTitle(stringResource(R.string.image_host_provider))

                    ProviderDropdownSelector(
                        currentProvider = editingProvider,
                        onProviderChange = { viewModel.updateProvider(it) },
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsItemCard {
                        DynamicProviderConfigFields(
                            provider = editingProvider,
                            editingConfig = editingConfig,
                            onConfigFieldChange = { key, value -> viewModel.updateConfigField(key, value) },
                        )

                        GroupDivider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = MdcitoCardDefaults.ContentPadding,
                                    vertical = 4.dp,
                                ),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(
                                onClick = { viewModel.testConnection() },
                                enabled = !isTesting,
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.test_upload), maxLines = 1)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            TextButton(
                                onClick = { viewModel.saveCurrentProfile() },
                                enabled = !isSaving,
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.save_config), maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── 图片处理设置（独立卡片，子选项无卡片嵌套） ──
                    SettingsGroupTitle(stringResource(R.string.image_settings))

                    // 压缩卡片
                    SettingsItemCard {
                        SwitchRow(
                            title = stringResource(R.string.image_compress),
                            subtitle = stringResource(R.string.image_compress_desc),
                            checked = processingSettings.compressEnabled,
                        ) {
                            viewModel.setCompressEnabled(it)
                        }

                        AnimatedVisibility(
                            visible = processingSettings.compressEnabled,
                            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(tween(300)),
                            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(tween(200)),
                        ) {
                            GroupDivider()
                            IndentedContent {
                                SubOptionSlider(
                                    title = stringResource(R.string.compress_quality),
                                    valueText = "${processingSettings.compressQuality}%",
                                    value = processingSettings.compressQuality.toFloat(),
                                    onValueChange = { viewModel.setCompressQuality(it.toInt()) },
                                    valueRange = 0f..100f,
                                    startLabel = stringResource(R.string.low_quality),
                                    endLabel = stringResource(R.string.high_quality),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 缩放卡片
                    SettingsItemCard {
                        SwitchRow(
                            title = stringResource(R.string.image_scale),
                            subtitle = stringResource(R.string.image_scale_desc),
                            checked = processingSettings.resizeEnabled,
                        ) {
                            viewModel.setResizeEnabled(it)
                        }

                        AnimatedVisibility(
                            visible = processingSettings.resizeEnabled,
                            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(tween(300)),
                            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(tween(200)),
                        ) {
                            GroupDivider()
                            IndentedContent {
                                val presetOptions = ResizePresets.PRESETS.map { preset ->
                                    stringResource(preset.labelResId) to preset.key
                                }
                                val currentPresetLabel = ResizePresets.PRESETS.find {
                                    it.key == processingSettings.resizePreset
                                }?.let { stringResource(it.labelResId) } ?: stringResource(R.string.resize_custom_fallback)

                                InlineDropdownItem(
                                    title = stringResource(R.string.resize_preset),
                                    subtitle = stringResource(R.string.resize_preset_desc),
                                    currentValue = currentPresetLabel,
                                    options = presetOptions,
                                    onSelect = { viewModel.setResizePreset(it) },
                                )

                                if (processingSettings.resizePreset == "custom") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        OutlinedTextField(
                                            value = processingSettings.resizeMaxWidth.toString(),
                                            onValueChange = { value ->
                                                val width = value.toIntOrNull()
                                                if (width != null && width > 0) {
                                                    viewModel.setResizeMaxWidth(width)
                                                }
                                            },
                                            label = { Text(stringResource(R.string.max_width), style = MaterialTheme.typography.bodySmall) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            textStyle = MaterialTheme.typography.bodyMedium,
                                            colors = settingsFieldColors(),
                                        )
                                        OutlinedTextField(
                                            value = processingSettings.resizeMaxHeight.toString(),
                                            onValueChange = { value ->
                                                val height = value.toIntOrNull()
                                                if (height != null && height > 0) {
                                                    viewModel.setResizeMaxHeight(height)
                                                }
                                            },
                                            label = { Text(stringResource(R.string.max_height), style = MaterialTheme.typography.bodySmall) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            textStyle = MaterialTheme.typography.bodyMedium,
                                            colors = settingsFieldColors(),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 水印卡片
                    SettingsItemCard {
                        SwitchRow(
                            title = stringResource(R.string.watermark),
                            subtitle = stringResource(R.string.watermark_desc),
                            checked = processingSettings.watermarkEnabled,
                        ) {
                            viewModel.setWatermarkEnabled(it)
                        }

                        AnimatedVisibility(
                            visible = processingSettings.watermarkEnabled,
                            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(tween(300)),
                            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(tween(200)),
                        ) {
                            GroupDivider()
                            IndentedContent {
                                OutlinedTextField(
                                    value = processingSettings.watermarkText,
                                    onValueChange = { viewModel.setWatermarkText(it) },
                                    label = { Text(stringResource(R.string.watermark_text), style = MaterialTheme.typography.bodySmall) },
                                    placeholder = { Text(stringResource(R.string.watermark_text_hint), style = MaterialTheme.typography.bodySmall) },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    colors = settingsFieldColors(),
                                )

                                val positionOptions = WatermarkPositions.POSITIONS.map { pos ->
                                    stringResource(pos.labelResId) to pos.key
                                }
                                val currentPositionLabel = WatermarkPositions.POSITIONS.find {
                                    it.key == processingSettings.watermarkPosition
                                }?.let { stringResource(it.labelResId) } ?: stringResource(R.string.position_default_fallback)

                                InlineDropdownItem(
                                    title = stringResource(R.string.watermark_position),
                                    subtitle = stringResource(R.string.watermark_position_desc),
                                    currentValue = currentPositionLabel,
                                    options = positionOptions,
                                    onSelect = { viewModel.setWatermarkPosition(it) },
                                )

                                SubOptionSlider(
                                    title = stringResource(R.string.opacity),
                                    valueText = "${processingSettings.watermarkOpacity}%",
                                    value = processingSettings.watermarkOpacity.toFloat(),
                                    onValueChange = { viewModel.setWatermarkOpacity(it.toInt()) },
                                    valueRange = 0f..100f,
                                    startLabel = stringResource(R.string.transparent),
                                    endLabel = stringResource(R.string.opaque),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 自动重命名卡片
                    SettingsItemCard {
                        SwitchRow(
                            title = stringResource(R.string.auto_rename),
                            subtitle = stringResource(R.string.auto_rename_desc),
                            checked = processingSettings.autoRenameEnabled,
                        ) {
                            viewModel.setAutoRenameEnabled(it)
                        }

                        AnimatedVisibility(
                            visible = processingSettings.autoRenameEnabled,
                            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(tween(300)),
                            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(tween(200)),
                        ) {
                            GroupDivider()
                            IndentedContent {
                                Text(
                                    text = stringResource(R.string.naming_rule),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.W500,
                                    modifier = Modifier.padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 8.dp),
                                )
                                Text(
                                    text = stringResource(R.string.naming_rule_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MdcitoCardDefaults.glassSubtleContentColor(),
                                    modifier = Modifier.padding(horizontal = MdcitoCardDefaults.ContentPadding),
                                )

                                OutlinedTextField(
                                    value = processingSettings.renameRule,
                                    onValueChange = { viewModel.setRenameRule(it) },
                                    placeholder = { Text("{timestamp}", style = MaterialTheme.typography.bodySmall) },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    colors = settingsFieldColors(),
                                )

                                FlowRow(
                                    modifier = Modifier.padding(horizontal = MdcitoCardDefaults.ContentPadding),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    RenameVariables.ALL.forEach { variable ->
                                        FilterChip(
                                            selected = false,
                                            onClick = {
                                                val current = processingSettings.renameRule
                                                viewModel.setRenameRule(current + variable.key)
                                            },
                                            label = {
                                                Text(
                                                    text = stringResource(variable.labelResId),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                )
                                            },
                                        )
                                    }
                                }

                                RenamePreviewCard(renameRule = processingSettings.renameRule)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            if (!imageHostEnabled) {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showRenameDialog) {
        RenameProfileDialog(
            currentName = editingName,
            onConfirm = { newName ->
                viewModel.renameProfile(newName)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete), fontWeight = FontWeight.W600) },
            text = {
                Text(stringResource(R.string.confirm_delete_profile, activeProfile?.name ?: ""))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val profileId = activeProfileId
                        if (profileId.isNotBlank()) {
                            viewModel.deleteProfile(profileId)
                        }
                        showDeleteDialog = false
                    },
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun settingsFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
)

// ── 方案选择下拉列表 ──
@Composable
private fun ProfileDropdownSelector(
    profiles: List<com.mdcito.app.data.model.ImageHostProfile>,
    activeProfile: com.mdcito.app.data.model.ImageHostProfile?,
    onSelect: (String) -> Unit,
    isSwitching: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentDisplayName = activeProfile?.name ?: stringResource(R.string.no_profile)

    Column {
        SettingsItemCard(onClick = { expanded = !expanded }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MdcitoCardDefaults.ContentPadding,
                        vertical = MdcitoCardDefaults.CompactContentPadding,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.current_profile), style = MaterialTheme.typography.bodyLarge)
                    Text(text = stringResource(R.string.current_profile), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isSwitching) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Text(text = currentDisplayName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (expanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(tween(200)),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(tween(150)),
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(vertical = 6.dp),
            ) {
                profiles.forEachIndexed { index, profile ->
                    val isSelected = profile.id == activeProfile?.id
                    val selectedBgColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                        animationSpec = tween(200),
                        label = "profile_bg",
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(selectedBgColor)
                            .then(if (isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp)) else Modifier)
                            .clickable { onSelect(profile.id); expanded = false }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(20.dp).clip(CircleShape)
                                    .then(if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary) else Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f).padding(vertical = 10.dp)) {
                                Text(
                                    text = buildString { append(profile.name); if (profile.isDefault) append(stringResource(R.string.default_profile_suffix)) },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W400,
                                )
                                Text(text = profile.provider, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (index < profiles.lastIndex) {
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp).height(0.5.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
                    }
                }
            }
        }
    }
}

// ── 供应商下拉选择 ──
@Composable
private fun ProviderDropdownSelector(
    currentProvider: String,
    onProviderChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val providers = listOf(
        Triple("github", stringResource(R.string.provider_github), stringResource(R.string.provider_github_desc)),
        Triple("qiniu", stringResource(R.string.provider_qiniu), stringResource(R.string.provider_qiniu_desc)),
        Triple("smms", stringResource(R.string.provider_smms), stringResource(R.string.provider_smms_desc)),
        Triple("aliyun", stringResource(R.string.provider_aliyun), stringResource(R.string.provider_aliyun_desc)),
        Triple("tencent", stringResource(R.string.provider_tencent), stringResource(R.string.provider_tencent_desc)),
        Triple("imgur", stringResource(R.string.provider_imgur), stringResource(R.string.provider_imgur_desc)),
        Triple("custom", stringResource(R.string.provider_custom), stringResource(R.string.provider_custom_desc)),
    )
    val currentDisplayName = providers.find { it.first == currentProvider }?.second ?: currentProvider

    Column {
        SettingsItemCard(onClick = { expanded = !expanded }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = MdcitoCardDefaults.CompactContentPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.image_host_provider), style = MaterialTheme.typography.bodyLarge)
                    Text(text = stringResource(R.string.image_host_provider), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(text = currentDisplayName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (expanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(tween(200)),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(tween(150)),
        ) {
            Column(
                modifier = Modifier.padding(top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).padding(vertical = 6.dp),
            ) {
                providers.forEachIndexed { index, (key, name, desc) ->
                    val isSelected = currentProvider == key
                    val selectedBgColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                        animationSpec = tween(200),
                        label = "provider_bg",
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(selectedBgColor)
                            .then(if (isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp)) else Modifier)
                            .clickable { onProviderChange(key); expanded = false }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(20.dp).clip(CircleShape)
                                    .then(if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary) else Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f).padding(vertical = 10.dp)) {
                                Text(text = name, style = MaterialTheme.typography.bodyMedium, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W400)
                                Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (index < providers.lastIndex) {
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp).height(0.5.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
                    }
                }
            }
        }
    }
}

// ── 命名规则预览卡片 ──
@Composable
private fun RenamePreviewCard(renameRule: String) {
    val preview = RenameVariables.preview(renameRule)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = stringResource(R.string.rename_preview),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.W500,
            color = MdcitoCardDefaults.glassSubtleContentColor(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = preview,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.W600,
        )
    }
}

// ── 动态供应商配置字段 ──
@Composable
private fun DynamicProviderConfigFields(
    provider: String,
    editingConfig: Map<String, String>,
    onConfigFieldChange: (String, String) -> Unit,
) {
    val fields = ProviderFieldDefs.getFields(provider)
    if (fields.isEmpty()) {
        Column(modifier = Modifier.padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 10.dp)) {
            Text(text = stringResource(R.string.no_config_items), style = MaterialTheme.typography.bodyMedium, color = MdcitoCardDefaults.glassSubtleContentColor())
        }
        return
    }
    Column(modifier = Modifier.padding(horizontal = MdcitoCardDefaults.ContentPadding, vertical = 8.dp)) {
        fields.forEachIndexed { index, fieldDef ->
            DynamicConfigField(
                fieldDef = fieldDef,
                value = editingConfig[fieldDef.key] ?: "",
                onValueChange = { onConfigFieldChange(fieldDef.key, it) },
            )
            if (index < fields.lastIndex) Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

// ── 动态配置字段 ──
@Composable
private fun DynamicConfigField(
    fieldDef: ProviderFieldDefs.FieldDef,
    value: String,
    onValueChange: (String) -> Unit,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var hasBeenEdited by remember { mutableStateOf(false) }
    val validationError = when {
        fieldDef.isRequired && hasBeenEdited && value.isBlank() -> stringResource(R.string.field_required)
        fieldDef.key == "repo" && value.isNotBlank() && !value.contains("/") -> stringResource(R.string.field_invalid_format)
        fieldDef.key == "url" && value.isNotBlank() && !value.startsWith("http") -> stringResource(R.string.field_invalid_format)
        else -> null
    }
    val isHttpWarning = fieldDef.key == "url" && value.startsWith("http://")

    Column {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it); if (!hasBeenEdited) hasBeenEdited = true },
            label = {
                val fieldLabel = stringResource(getFieldLabelResId(fieldDef.labelKey))
                if (fieldDef.isRequired) {
                    Text(buildAnnotatedString { withStyle(SpanStyle(color = MaterialTheme.colorScheme.error)) { append("* ") }; append(fieldLabel) }, style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(fieldLabel, style = MaterialTheme.typography.bodySmall)
                }
            },
            placeholder = { Text(fieldDef.placeholder, style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = if (validationError != null) OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.error,
                unfocusedBorderColor = MaterialTheme.colorScheme.error,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ) else settingsFieldColors(),
            isError = validationError != null,
            visualTransformation = if (fieldDef.isSecret && !passwordVisible) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            trailingIcon = if (fieldDef.isSecret) {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (passwordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            } else null,
        )
        if (validationError != null) {
            Text(text = validationError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 16.dp, top = 2.dp))
        } else if (isHttpWarning) {
            Text(text = stringResource(R.string.upload_custom_http_warning), style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF9800), modifier = Modifier.padding(start = 16.dp, top = 2.dp))
        } else if (fieldDef.helpTextKey.isNotEmpty()) {
            val helpResId = ProviderFieldDefs.getHelpTextResId(fieldDef.helpTextKey)
            if (helpResId != 0) {
                Text(text = stringResource(helpResId), style = MaterialTheme.typography.bodySmall, color = MdcitoCardDefaults.glassSubtleContentColor(), modifier = Modifier.padding(start = 16.dp, top = 2.dp))
            }
        }
    }
}

// ── 重命名对话框 ──
@Composable
private fun RenameProfileDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var newName by remember { mutableStateOf(currentName) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename), fontWeight = FontWeight.W600) },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { input ->
                        if (input.length <= 50) { newName = input; error = "" }
                        else { error = context.getString(R.string.name_too_long) }
                    },
                    label = { Text(stringResource(R.string.rename_placeholder), style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = if (error.isNotEmpty()) OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.error,
                        unfocusedBorderColor = MaterialTheme.colorScheme.error,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    ) else settingsFieldColors(),
                    isError = error.isNotEmpty(),
                )
                if (error.isNotEmpty()) {
                    Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                }
                Text(text = "${newName.length}/50", style = MaterialTheme.typography.bodySmall, color = MdcitoCardDefaults.glassSubtleContentColor(), modifier = Modifier.padding(end = 16.dp, top = 4.dp).align(Alignment.End))
            }
        },
        confirmButton = {
            TextButton(onClick = { if (newName.isBlank()) error = context.getString(R.string.name_empty) else onConfirm(newName.trim()) }, enabled = newName.isNotBlank() && error.isEmpty()) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
