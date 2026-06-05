package dev.tomerklein.holocron.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tomerklein.holocron.data.DeliveryLog
import dev.tomerklein.holocron.data.HolocronRepository
import dev.tomerklein.holocron.data.SettingsStore
import dev.tomerklein.holocron.service.ListeningService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val foregroundServiceEnabled: Boolean = false,
    val recent: List<DeliveryLog> = emptyList(),
    val ruleCount: Int = 0,
    val destinationCount: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: HolocronRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        settingsStore.settings,
        repository.observeRecentLogs(10),
        repository.observeRules(),
        repository.observeDestinations(),
    ) { settings, recent, rules, destinations ->
        HomeUiState(
            foregroundServiceEnabled = settings.foregroundServiceEnabled,
            recent = recent,
            ruleCount = rules.size,
            destinationCount = destinations.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setForegroundService(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setForegroundServiceEnabled(enabled)
            if (enabled) ListeningService.start(appContext) else ListeningService.stop(appContext)
        }
    }
}
