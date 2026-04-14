package com.mintanable.notethepad.feature_note.presentation.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mintanable.notethepad.database.preference.repository.UserPreferencesRepository
import com.mintanable.notethepad.feature_ai.domain.repository.NoteAssistantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val assistantRepository: NoteAssistantRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AiAssistantState())
    val state: StateFlow<AiAssistantState> = _state.asStateFlow()

    private var streamJob: Job? = null

    fun show() {
        _state.value = _state.value.copy(visible = true)
    }

    fun dismiss() {
        streamJob?.cancel()
        streamJob = null
        _state.value = AiAssistantState()
    }

    fun onPromptChange(value: String) {
        _state.value = _state.value.copy(prompt = value)
    }

    fun submit() {
        val prompt = _state.value.prompt.trim()
        if (prompt.isEmpty() || _state.value.isStreaming) return

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            val modelName = userPreferencesRepository.settingsFlow.first().aiModelName
            if (modelName == "None" || modelName.isBlank()) {
                _state.value = _state.value.copy(
                    response = "No AI model configured. Pick one in Settings.",
                    isStreaming = false,
                )
                return@launch
            }

            val builder = StringBuilder()
            assistantRepository.runAiAssistant(prompt = prompt, modelName = modelName)
                .onStart {
                    _state.value = _state.value.copy(
                        isStreaming = true,
                        response = "",
                    )
                }
                .onEach { chunk ->
                    builder.append(chunk)
                    _state.value = _state.value.copy(response = builder.toString())
                }
                .onCompletion {
                    _state.value = _state.value.copy(isStreaming = false)
                }
                .collect {}
        }
    }

    override fun onCleared() {
        streamJob?.cancel()
        super.onCleared()
    }
}

data class AiAssistantState(
    val visible: Boolean = false,
    val prompt: String = "",
    val response: String = "",
    val isStreaming: Boolean = false,
)
