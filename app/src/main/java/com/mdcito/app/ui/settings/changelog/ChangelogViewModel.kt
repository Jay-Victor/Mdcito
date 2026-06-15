package com.mdcito.app.ui.settings.changelog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdcito.app.data.update.ChangelogEntry
import com.mdcito.app.data.update.UpdateChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ChangelogState {
    data object Loading : ChangelogState()
    data class Success(val entries: List<ChangelogEntry>) : ChangelogState()
    data class Error(val message: String) : ChangelogState()
}

@HiltViewModel
class ChangelogViewModel @Inject constructor(
    private val updateChecker: UpdateChecker,
) : ViewModel() {

    private val _changelogState = MutableStateFlow<ChangelogState>(ChangelogState.Loading)
    val changelogState: StateFlow<ChangelogState> = _changelogState.asStateFlow()

    init {
        fetchChangelog()
    }

    fun fetchChangelog() {
        viewModelScope.launch {
            _changelogState.value = ChangelogState.Loading
            val result = runCatching {
                updateChecker.fetchAllReleases()
            }
            _changelogState.value = result.fold(
                onSuccess = { entries ->
                    if (entries.isEmpty()) {
                        ChangelogState.Error("No releases found")
                    } else {
                        ChangelogState.Success(entries)
                    }
                },
                onFailure = { ChangelogState.Error(it.message ?: "Unknown error") },
            )
        }
    }
}
