package dev.tomerklein.holocron.ui.rules

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tomerklein.holocron.data.Destination
import dev.tomerklein.holocron.data.HolocronRepository
import dev.tomerklein.holocron.data.MatchType
import dev.tomerklein.holocron.data.Rule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RulesUiState(
    val rules: List<Rule> = emptyList(),
    val destinationNames: Map<Long, String> = emptyMap(),
)

@HiltViewModel
class RulesViewModel @Inject constructor(
    private val repository: HolocronRepository,
) : ViewModel() {

    val uiState: StateFlow<RulesUiState> = combine(
        repository.observeRules(),
        repository.observeDestinations(),
    ) { rules, destinations ->
        RulesUiState(rules, destinations.associate { it.id to it.name })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RulesUiState())

    fun toggleEnabled(rule: Rule) {
        viewModelScope.launch {
            repository.upsertRule(rule.copy(enabled = !rule.enabled, updatedAt = System.currentTimeMillis()))
        }
    }

    fun delete(rule: Rule) {
        viewModelScope.launch { repository.deleteRule(rule) }
    }
}

data class RuleForm(
    val id: Long = 0,
    val name: String = "",
    val senderPattern: String = "",
    val matchType: MatchType = MatchType.EXACT,
    val destinationId: Long? = null,
    val enabled: Boolean = true,
    val loading: Boolean = true,
) {
    val isValid: Boolean get() = name.isNotBlank() && senderPattern.isNotBlank() && destinationId != null
}

@HiltViewModel
class RuleEditViewModel @Inject constructor(
    private val repository: HolocronRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val ruleId: Long = savedStateHandle.get<Long>("ruleId") ?: -1L
    val isNew: Boolean = ruleId <= 0L

    private val _form = MutableStateFlow(RuleForm(loading = !isNew))
    val form: StateFlow<RuleForm> = _form

    val destinations: StateFlow<List<Destination>> =
        repository.observeDestinations()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (isNew) {
            _form.value = RuleForm(loading = false)
        } else {
            viewModelScope.launch {
                repository.rule(ruleId)?.let { r ->
                    _form.value = RuleForm(
                        id = r.id,
                        name = r.name,
                        senderPattern = r.senderPattern,
                        matchType = r.matchType,
                        destinationId = r.destinationId,
                        enabled = r.enabled,
                        loading = false,
                    )
                } ?: run { _form.value = RuleForm(loading = false) }
            }
        }
    }

    fun update(transform: (RuleForm) -> RuleForm) = _form.update(transform)

    fun save(onSaved: () -> Unit) {
        val f = _form.value
        if (!f.isValid) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.upsertRule(
                Rule(
                    id = f.id,
                    name = f.name.trim(),
                    senderPattern = f.senderPattern.trim(),
                    matchType = f.matchType,
                    destinationId = f.destinationId!!,
                    enabled = f.enabled,
                    createdAt = if (isNew) now else now, // createdAt preserved below for edits
                    updatedAt = now,
                ).let { rule ->
                    if (isNew) rule else rule.copy(createdAt = repository.rule(f.id)?.createdAt ?: now)
                },
            )
            onSaved()
        }
    }
}
