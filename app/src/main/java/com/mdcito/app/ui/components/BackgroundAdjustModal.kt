package com.mdcito.app.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.mdcito.app.R
import com.mdcito.app.ui.settings.SettingsViewModel

@Composable
fun BackgroundAdjustModal(
    visible: Boolean,
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val backgroundBlur by viewModel.backgroundBlur.collectAsState()
    val backgroundBlurIntensity by viewModel.backgroundBlurIntensity.collectAsState()
    val backgroundBrightness by viewModel.backgroundBrightness.collectAsState()
    val cardTransparency by viewModel.cardTransparency.collectAsState()
    val navBarTransparency by viewModel.navBarTransparency.collectAsState()

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setBackgroundImageUri(it.toString()) }
    }

    if (visible) {
        MdcitoCenterModal(
            onDismissRequest = onDismiss,
            visible = true,
            title = stringResource(R.string.background_adjust),
        ) {
            Text(
                text = stringResource(R.string.background_image),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { imageLauncher.launch("image/*") },
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.select_image), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.setBackgroundImageUri(null) },
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.remove_image), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.blur_and_brightness),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.gaussian_blur), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.gaussian_blur_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = backgroundBlur,
                    onCheckedChange = { viewModel.setBackgroundBlur(it) },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedThumbColor = Color.White,
                    ),
                )
            }

            if (backgroundBlur) {
                Spacer(modifier = Modifier.height(8.dp))
                SliderRow(stringResource(R.string.blur_intensity), stringResource(R.string.intensity_level, backgroundBlurIntensity), backgroundBlurIntensity.toFloat(), 0f..20f) {
                    viewModel.setBackgroundBlurIntensity(it.toInt())
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            SliderRow(stringResource(R.string.brightness_adjust), "${backgroundBrightness}%", backgroundBrightness.toFloat(), 50f..150f) {
                viewModel.setBackgroundBrightness(it.toInt())
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.transparency),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            SliderRow(stringResource(R.string.card_transparency_pct), "${cardTransparency}%", cardTransparency.toFloat(), 0f..100f) {
                viewModel.setCardTransparency(it.toInt())
            }

            Spacer(modifier = Modifier.height(8.dp))
            SliderRow(stringResource(R.string.nav_bar_transparency_pct), "${navBarTransparency}%", navBarTransparency.toFloat(), 0f..100f) {
                viewModel.setNavBarTransparency(it.toInt())
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
            }
        }
    }
}

@Composable
private fun SliderRow(
    title: String,
    valueLabel: String,
    currentValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = currentValue,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
