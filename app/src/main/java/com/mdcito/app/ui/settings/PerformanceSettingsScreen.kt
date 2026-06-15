package com.mdcito.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mdcito.app.R
import com.mdcito.app.data.locale.LanguageHelper

@Composable
fun PerformanceSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    LanguageHelper.LocalLocaleVersion.current
    val hardwareAccel by viewModel.hardwareAccel.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        SettingsTopBar(
            title = stringResource(R.string.performance_settings),
            onNavigateBack = onNavigateBack,
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsGroupTitle(stringResource(R.string.gpu_rendering))

        SwitchSetting(
            title = stringResource(R.string.hardware_accel),
            subtitle = stringResource(R.string.hardware_accel_desc),
            checked = hardwareAccel,
        ) {
            viewModel.setHardwareAccel(it)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
