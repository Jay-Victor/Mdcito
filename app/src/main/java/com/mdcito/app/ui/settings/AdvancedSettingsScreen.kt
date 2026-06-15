package com.mdcito.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mdcito.app.R
import com.mdcito.app.data.locale.LanguageHelper

@Composable
fun AdvancedSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPerformance: () -> Unit,
    onNavigateToLog: () -> Unit,
) {
    LanguageHelper.LocalLocaleVersion.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        SettingsTopBar(
            title = stringResource(R.string.advanced_settings),
            onNavigateBack = onNavigateBack,
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsGroupTitle(stringResource(R.string.advanced_settings_group))

        SettingsNavigationItem(
            icon = Icons.Outlined.Speed,
            label = stringResource(R.string.performance_settings),
            subtitle = stringResource(R.string.performance_settings_subtitle),
            onClick = onNavigateToPerformance,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsNavigationItem(
            icon = Icons.Outlined.Terminal,
            label = stringResource(R.string.log_settings),
            subtitle = stringResource(R.string.log_settings_subtitle),
            onClick = onNavigateToLog,
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}
