package dev.tomerklein.holocron.ui.destinations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tomerklein.holocron.data.Destination
import dev.tomerklein.holocron.data.HolocronRepository
import dev.tomerklein.holocron.data.SecurePrefs
import dev.tomerklein.holocron.dispatch.DispatchResult
import dev.tomerklein.holocron.dispatch.TestSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DestinationsViewModel @Inject constructor(
    private val repository: HolocronRepository,
    private val securePrefs: SecurePrefs,
    private val testSender: TestSender,
) : ViewModel() {

    val destinations: StateFlow<List<Destination>> =
        repository.observeDestinations()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Transient toast-style message (test results, blocked deletes, etc.). */
    val message = MutableStateFlow<String?>(null)

    fun test(destination: Destination) {
        viewModelScope.launch {
            message.value = "Testing ${destination.name}…"
            message.value = describe(destination.name, testSender.sendTest(destination))
        }
    }

    fun delete(destination: Destination) {
        viewModelScope.launch {
            val inUse = repository.ruleCountForDestination(destination.id)
            if (inUse > 0) {
                message.value =
                    "Can't delete \"${destination.name}\": ${inUse} rule(s) use it. Reassign or delete those rules first."
                return@launch
            }
            repository.deleteDestination(destination)
            securePrefs.clearSecrets(destination.id)
            message.value = "Deleted \"${destination.name}\""
        }
    }

    fun consumeMessage() {
        message.value = null
    }

    private fun describe(name: String, result: DispatchResult): String = when (result) {
        is DispatchResult.Success -> "$name: test succeeded"
        is DispatchResult.Retryable -> "$name: failed (retryable) — ${result.reason}"
        is DispatchResult.Permanent -> "$name: failed — ${result.reason}"
    }
}
