package dev.tomerklein.holocron.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tomerklein.holocron.data.DeliveryLog
import dev.tomerklein.holocron.data.HolocronRepository
import dev.tomerklein.holocron.dispatch.DispatchEnqueuer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeliveryLogUiState(
    val logs: List<DeliveryLog> = emptyList(),
    val destinationNames: Map<Long, String> = emptyMap(),
    val ruleNames: Map<Long, String> = emptyMap(),
)

@HiltViewModel
class DeliveryLogViewModel @Inject constructor(
    private val repository: HolocronRepository,
    private val enqueuer: DispatchEnqueuer,
) : ViewModel() {

    val uiState: StateFlow<DeliveryLogUiState> = combine(
        repository.observeRecentLogs(500),
        repository.observeDestinations(),
        repository.observeRules(),
    ) { logs, destinations, rules ->
        DeliveryLogUiState(
            logs = logs,
            destinationNames = destinations.associate { it.id to it.name },
            ruleNames = rules.associate { it.id to it.name },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeliveryLogUiState())

    val message = MutableStateFlow<String?>(null)

    fun retry(log: DeliveryLog) {
        viewModelScope.launch {
            message.value = if (enqueuer.retry(log)) {
                "Re-queued delivery #${log.id}"
            } else {
                "Cannot retry: message body is no longer available"
            }
        }
    }

    fun consumeMessage() {
        message.value = null
    }
}
