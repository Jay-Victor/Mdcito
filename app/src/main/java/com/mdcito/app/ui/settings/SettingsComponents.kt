package com.mdcito.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mdcito.app.R
import com.mdcito.app.ui.components.LocalCardGlassIntensity
import com.mdcito.app.ui.components.LocalCardStyle
import com.mdcito.app.ui.components.LocalCardTransparency
import com.mdcito.app.ui.components.MdcitoCard
import com.mdcito.app.ui.components.MdcitoCardDefaults

@Composable
internal fun SettingsTopBar(
    title: String,
    onNavigateBack: () -> Unit,
    backIcon: ImageVector? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = backIcon ?: Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.W600,
            modifier = Modifier.weight(1f),
        )
        if (trailingContent != null) {
            trailingContent()
        }
    }
}

@Composable
internal fun SettingsGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.W600,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp, top = 4.dp),
    )
}

@Composable
internal fun SettingsItemCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    MdcitoCard(
        onClick = onClick,
        cardStyle = LocalCardStyle.current,
        glassIntensity = LocalCardGlassIntensity.current,
        transparency = LocalCardTransparency.current,
        modifier = modifier.fillMaxWidth(),
    ) {
        content()
    }
}

@Composable
internal fun SwitchSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsItemCard {
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
            Switch(
                checked = checked,
                onCheckedChange = if (enabled) onCheckedChange else null,
                enabled = enabled,
                colors = SwitchDefaults.colors(
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
}

@Composable
internal fun SettingsNavigationItem(
    icon: ImageVector,
    label: String,
    subtitle: String = "",
    onClick: () -> Unit,
) {
    SettingsItemCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MdcitoCardDefaults.ContentPadding,
                    vertical = MdcitoCardDefaults.CompactContentPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MdcitoCardDefaults.glassIconTint(),
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MdcitoCardDefaults.glassSubtleContentColor(),
                    )
                }
            }
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = MdcitoCardDefaults.glassSubtleContentColor(),
            )
        }
    }
}

@Composable
internal fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    SettingsItemCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MdcitoCardDefaults.ContentPadding,
                    vertical = MdcitoCardDefaults.CompactContentPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MdcitoCardDefaults.glassIconTint(),
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MdcitoCardDefaults.glassSubtleContentColor(),
                )
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

@Composable
internal fun SettingsSliderItem(
    title: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
) {
    SettingsItemCard {
        Column(
            modifier = Modifier.padding(
                horizontal = MdcitoCardDefaults.ContentPadding,
                vertical = 8.dp,
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun SettingsDropdownItem(
    title: String,
    subtitle: String,
    currentValue: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

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
                    Text(text = title, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MdcitoCardDefaults.glassSubtleContentColor(),
                    )
                }
                Text(
                    text = currentValue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(4.dp))
            options.forEach { (label, value) ->
                SettingsItemCard(onClick = {
                    onSelect(value)
                    expanded = false
                }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = MdcitoCardDefaults.ContentPadding,
                                vertical = 10.dp,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            color = if (currentValue == label) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (currentValue == label) FontWeight.W600 else FontWeight.W400,
                        )
                        if (currentValue == label) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                if (value != options.last().second) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
internal fun SettingsInfoItem(
    label: String,
    value: String,
    icon: ImageVector? = null,
    valueColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    SettingsItemCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MdcitoCardDefaults.ContentPadding,
                    vertical = MdcitoCardDefaults.CompactContentPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MdcitoCardDefaults.glassIconTint(),
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (valueColor != Color.Unspecified) valueColor
                else MdcitoCardDefaults.glassSubtleContentColor(),
            )
            if (trailing != null) {
                Spacer(modifier = Modifier.width(4.dp))
                trailing()
            }
        }
    }
}

@Composable
internal fun SettingsTextActionItem(
    icon: ImageVector? = null,
    title: String,
    subtitle: String,
    buttonText: String,
    buttonColor: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onButtonClick: () -> Unit,
) {
    SettingsItemCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MdcitoCardDefaults.ContentPadding,
                    vertical = MdcitoCardDefaults.CompactContentPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (buttonColor != MaterialTheme.colorScheme.primary) buttonColor
                    else MdcitoCardDefaults.glassIconTint(),
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MdcitoCardDefaults.glassSubtleContentColor(),
                )
            }
            TextButton(onClick = onButtonClick) {
                Text(buttonText, color = buttonColor)
            }
        }
    }
}

@Composable
internal fun ColorPickerModal(
    visible: Boolean,
    title: String,
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(1f) }
    var brightness by remember { mutableStateOf(1f) }
    var alpha by remember { mutableStateOf(currentColor.alpha) }
    var inputMode by remember { mutableStateOf("CANVAS") }

    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title, fontWeight = FontWeight.W600) },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = inputMode == "CANVAS",
                            onClick = { inputMode = "CANVAS" },
                            label = { Text(stringResource(R.string.color_canvas)) },
                        )
                        FilterChip(
                            selected = inputMode == "SLIDER",
                            onClick = { inputMode = "SLIDER" },
                            label = { Text(stringResource(R.string.slider)) },
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (inputMode == "CANVAS") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .drawBehind {
                                    val width = size.width
                                    val height = size.height
                                    val dotSize = 2.dp.toPx()
                                    val step = dotSize.toInt().coerceAtLeast(1)
                                    for (x in 0 until width.toInt() step step) {
                                        for (y in 0 until height.toInt() step step) {
                                            val h = (x.toFloat() / width) * 360f
                                            val s = (1f - (y.toFloat() / height) * 0.5f).coerceIn(0f, 1f)
                                            val v = (1f - (y.toFloat() / height) * 0.5f).coerceIn(0f, 1f)
                                            val color = hsvToColor(h, s, v)
                                            drawRect(color, topLeft = Offset(x.toFloat(), y.toFloat()), size = Size(dotSize, dotSize))
                                        }
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        hue = (offset.x / size.width) * 360f
                                        saturation = (1f - (offset.y / size.height) * 0.5f).coerceIn(0f, 1f)
                                        brightness = (1f - (offset.y / size.height) * 0.5f).coerceIn(0f, 1f)
                                    }
                                },
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(stringResource(R.string.brightness), style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = brightness,
                            onValueChange = { brightness = it },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                            ),
                        )

                        Text(stringResource(R.string.opacity), style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = alpha,
                            onValueChange = { alpha = it },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    } else {
                        Text(stringResource(R.string.hue), style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = hue,
                            onValueChange = { hue = it },
                            valueRange = 0f..360f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                            ),
                        )

                        Text(stringResource(R.string.saturation), style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = saturation,
                            onValueChange = { saturation = it },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                            ),
                        )

                        Text(stringResource(R.string.brightness), style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = brightness,
                            onValueChange = { brightness = it },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(currentColor)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                        )
                        Text(stringResource(R.string.current_color), style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterVertically))
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(hsvToColor(hue, saturation, brightness, alpha))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                        )
                        Text(stringResource(R.string.new_color), style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterVertically))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onColorSelected(hsvToColor(hue, saturation, brightness, alpha))
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

private fun hsvToColor(h: Float, s: Float, v: Float, a: Float = 1f): Color {
    val c = v * s
    val x = c * (1 - kotlin.math.abs((h / 60f) % 2 - 1))
    val m = v - c
    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        red = r1 + m,
        green = g1 + m,
        blue = b1 + m,
        alpha = a,
    )
}
