package com.mdcito.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mdcito.app.R
import com.mdcito.app.data.locale.LanguageHelper
import com.mdcito.app.ui.components.LocalCardGlassIntensity
import com.mdcito.app.ui.components.LocalCardStyle
import com.mdcito.app.ui.components.LocalCardTransparency
import com.mdcito.app.ui.components.MdcitoCard
import com.mdcito.app.ui.components.MdcitoCardDefaults

@Composable
fun SettingsScreen(
    onNavigateToAppearance: () -> Unit,
    onNavigateToEditorSettings: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToCloudSync: () -> Unit,
    onNavigateToImageHost: () -> Unit,
    onNavigateToAdvanced: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    LanguageHelper.LocalLocaleVersion.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.settings),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.W600,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SettingsItem(
            icon = Icons.Outlined.Palette,
            label = stringResource(R.string.appearance_settings),
            onClick = onNavigateToAppearance,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsItem(
            icon = Icons.Outlined.Edit,
            label = stringResource(R.string.editor_settings),
            onClick = onNavigateToEditorSettings,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsItem(
            icon = Icons.Outlined.Folder,
            label = stringResource(R.string.storage_settings),
            onClick = onNavigateToStorage,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsItem(
            icon = Icons.Outlined.Cloud,
            label = stringResource(R.string.cloud_sync_settings),
            onClick = onNavigateToCloudSync,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsItem(
            icon = Icons.Outlined.Image,
            label = stringResource(R.string.image_host_settings),
            onClick = onNavigateToImageHost,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsItem(
            icon = Icons.Outlined.Code,
            label = stringResource(R.string.advanced_settings),
            onClick = onNavigateToAdvanced,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsItem(
            icon = Icons.Outlined.Info,
            label = stringResource(R.string.about),
            onClick = onNavigateToAbout,
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    MdcitoCard(
        onClick = onClick,
        cardStyle = LocalCardStyle.current,
        glassIntensity = LocalCardGlassIntensity.current,
        transparency = LocalCardTransparency.current,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MdcitoCardDefaults.ContentPadding,
                    vertical = MdcitoCardDefaults.CompactContentPadding,
                ),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MdcitoCardDefaults.glassIconTint(),
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = MdcitoCardDefaults.glassSubtleContentColor(),
            )
        }
    }
}
