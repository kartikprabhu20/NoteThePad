package com.mintanable.notethepad.feature_note.presentation.assistant

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val assistantRepository: NoteAssistantRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AiAssistantState())
    val state: StateFlow<AiAssistantState> = _state.asStateFlow()

    private var streamJob: Job? = null
    private var transcriptionJob: Job? = null

    fun show() {
        _state.value = _state.value.copy(visible = true)
    }

    fun dismiss() {
        streamJob?.cancel()
        streamJob = null
        transcriptionJob?.cancel()
        transcriptionJob = null
        viewModelScope.launch {
            assistantRepository.resetAiAssistantSession()
        }
        _state.value = AiAssistantState()
    }

    fun onPromptChange(value: TextFieldValue) {
        _state.value = _state.value.copy(prompt = value)
    }

    fun submit() {
        val promptText = _state.value.prompt.text.trim()
        if (promptText.isEmpty() || _state.value.isStreaming) return

        val userMessage = ChatMessage(role = MessageRole.USER, text = promptText)
        val assistantMessageId = UUID.randomUUID().toString()
        
        _state.value = _state.value.copy(
            messages = _state.value.messages + userMessage,
            prompt = TextFieldValue(""),
            isStreaming = true
        )

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            val modelName = userPreferencesRepository.settingsFlow.first().aiModelName
            if (modelName == "None" || modelName.isBlank()) {
                val errorMsg = "No AI model configured. Pick one in Settings."
                _state.value = _state.value.copy(
                    messages = _state.value.messages + ChatMessage(role = MessageRole.ASSISTANT, text = errorMsg, isError = true),
                    isStreaming = false,
                )
                return@launch
            }

            val builder = StringBuilder()
            assistantRepository.runAiAssistant(prompt = promptText, modelName = modelName)
                .onStart {
                    _state.value = _state.value.copy(
                        messages = _state.value.messages + ChatMessage(id = assistantMessageId, role = MessageRole.ASSISTANT, text = "")
                    )
                }
                .onEach { chunk ->
                    builder.append(chunk)
                    updateAssistantMessage(assistantMessageId, builder.toString())
                }
                .onCompletion {
                    _state.value = _state.value.copy(isStreaming = false)
                }
                .collect {}
        }
    }

    private fun updateAssistantMessage(id: String, text: String) {
        _state.value = _state.value.copy(
            messages = _state.value.messages.map {
                if (it.id == id) it.copy(text = text) else it
            }
        )
    }

    fun toggleLiveTranscription() {
        if (_state.value.isTranscribing) {
            stopTranscription()
        } else {
            startTranscription()
        }
    }

    private fun startTranscription() {
        _state.value = _state.value.copy(isTranscribing = true)
        transcriptionJob = viewModelScope.launch {
            val modelName = userPreferencesRepository.settingsFlow.first().aiModelName
            assistantRepository.startLiveTranscription(
                onTranscription = { text ->
                    insertTextAtCursor(text)
                },
                aiModelName = modelName
            )
        }
    }

    private fun stopTranscription() {
        _state.value = _state.value.copy(isTranscribing = false)
        transcriptionJob?.cancel()
        transcriptionJob = null
        viewModelScope.launch {
            val modelName = userPreferencesRepository.settingsFlow.first().aiModelName
            assistantRepository.stopLiveTranscription(modelName)
        }
    }

    private fun insertTextAtCursor(text: String) {
        val current = _state.value.prompt
        val newText = StringBuilder(current.text)
            .replace(current.selection.start, current.selection.end, text)
            .toString()
        val newCursorOffset = current.selection.start + text.length
        
        _state.value = _state.value.copy(
            prompt = TextFieldValue(
                text = newText,
                selection = TextRange(newCursorOffset)
            )
        )
    }

    override fun onCleared() {
        streamJob?.cancel()
        super.onCleared()
    }
}

data class AiAssistantState(
    val visible: Boolean = false,
    val prompt: TextFieldValue = TextFieldValue(""),
    val messages: List<ChatMessage> = emptyList(),
    val isStreaming: Boolean = false,
    val isTranscribing: Boolean = false,
)
 
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val text: String,
    val isError: Boolean = false,
)
 
enum class MessageRole { USER, ASSISTANT }
