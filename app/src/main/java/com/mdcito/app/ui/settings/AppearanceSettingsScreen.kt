package com.mdcito.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mdcito.app.R
import com.mdcito.app.data.locale.LanguageHelper

@Composable
fun AppearanceSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToFont: () -> Unit,
    onNavigateToBackground: () -> Unit,
    onNavigateToCardStyle: () -> Unit,
) {
    LanguageHelper.LocalLocaleVersion.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        SettingsTopBar(title = stringResource(R.string.appearance_settings), onNavigateBack = onNavigateBack)

        Spacer(modifier = Modifier.height(12.dp))

        SettingsGroupTitle(stringResource(R.string.personalization))

        SettingsNavigationItem(
            icon = Icons.Outlined.Palette,
            label = stringResource(R.string.theme_settings),
            subtitle = stringResource(R.string.theme_settings_subtitle),
            onClick = onNavigateToTheme,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsNavigationItem(
            icon = Icons.Outlined.FormatSize,
            label = stringResource(R.string.font_settings),
            subtitle = stringResource(R.string.font_settings_subtitle),
            onClick = onNavigateToFont,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsNavigationItem(
            icon = Icons.Outlined.Image,
            label = stringResource(R.string.background_settings),
            subtitle = stringResource(R.string.background_settings_subtitle),
            onClick = onNavigateToBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsNavigationItem(
            icon = Icons.Outlined.Brush,
            label = stringResource(R.string.card_nav_style),
            subtitle = stringResource(R.string.card_nav_style_subtitle),
            onClick = onNavigateToCardStyle,
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}
