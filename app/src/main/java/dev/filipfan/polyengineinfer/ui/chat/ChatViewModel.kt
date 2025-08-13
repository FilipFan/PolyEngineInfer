package dev.filipfan.polyengineinfer.ui.chat

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.filipfan.polyengineinfer.api.LlmInferenceEngine
import dev.filipfan.polyengineinfer.api.LlmInferenceOptions
import dev.filipfan.polyengineinfer.api.LlmModelFiles
import dev.filipfan.polyengineinfer.executorch.ExecuTorchInference
import dev.filipfan.polyengineinfer.llamacpp.LlamaCppInference
import dev.filipfan.polyengineinfer.mediapipe.MediaPipeInference
import dev.filipfan.polyengineinfer.onnx.OnnxInference
import dev.filipfan.polyengineinfer.ui.settings.LlmSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException

/** Represents a single message in the chat. */
data class ChatMessage(
    val text: String,
    val isFromMe: Boolean,
    val stats: InferenceStats? = null,
    val showStats: Boolean = false,
)

/** Holds performance statistics for a single inference pass. */
data class InferenceStats(
    /** The total number of tokens in the input prompt that were processed. */
    val prefillTokens: Int,
    /** The processing speed of the prompt, measured in tokens per second (tokens/s). */
    val prefillSpeed: Float,
    /** The total number of generated tokens. */
    val decodeTokens: Int,
    /** The generation speed for the response, measured in tokens per second (tokens/s). */
    val decodeSpeed: Float,
    /**
     * The time from when the prompt is sent until the first response token is received,
     * measured in milliseconds (ms).
     */
    val timeToFirstToken: Long,
    /**
     * The total latency for the entire inference operation, from prompt submission to
     * receiving the final token, measured in milliseconds (ms).
     */
    val latency: Long,
)

/**
 * ViewModel for the chat screen, responsible for managing the state,
 * loading the LLM, and handling user interactions.
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    /** Sealed class for representing UI states clearly. */
    sealed class State {
        data object Uninitialized : State()

        data object Loading : State()

        data object Loaded : State()

        data object Generating : State()

        data class Error(val message: String?) : State()
    }

    private var engine: LlmInferenceEngine? = null

    private var generationJob: Job? = null

    // Holds the list of messages.
    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> = _messages

    private val _uiState = MutableStateFlow<State>(State.Uninitialized)
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    private val _currentModelTag = MutableStateFlow("ChatBot")
    val currentModelTag: StateFlow<String> = _currentModelTag.asStateFlow()

    fun loadModel(config: LlmSettings) {
        generationJob?.cancel()
        viewModelScope.launch {
            releaseEngine()
            _uiState.value = State.Loading

            val modelFiles =
                LlmModelFiles(modelPath = config.modelPath, tokenizerPath = config.tokenizerPath)
            val options = LlmInferenceOptions(
                maxTokens = config.maxTokens,
                topK = config.topK,
                topP = config.topP,
                temperature = config.temperature,
            )
            try {
                engine =
                    createEngineFromPath(modelFiles.modelPath, modelFiles.tokenizerPath).apply {
                        load(modelFiles, options)
                    }
            } catch (e: IllegalArgumentException) {
                _uiState.value = State.Error(e.message)
                return@launch
            }
            _uiState.value = State.Loaded
        }
    }

    fun sendMessage(userInput: String) {
        generationJob?.cancel()

        val currentInstance = checkNotNull(engine) { "Engine not loaded." }

        // Add the user's message to the list.
        _messages.add(ChatMessage(userInput, isFromMe = true))

        // Start generating a response.
        generationJob = viewModelScope.launch {
            val fullResponse = StringBuilder()
            // Record the stats of generating.
            var firstRun = true
            var timeToFirstToken: Long
            var firstTokenTs = 0L
            var decodeTokens = 0
            var prefillSpeed: Float
            var decodeSpeed: Float
            val startTime = System.currentTimeMillis()

            currentInstance.generate(userInput)
                .onStart {
                    _uiState.value = State.Generating
                    _messages.add(ChatMessage("", isFromMe = false))
                }
                .onCompletion {
                    _uiState.value = State.Loaded
                    // Calculate the stats.
                    val endTime = System.currentTimeMillis()
                    val prefillTokens = currentInstance.getLatestGenerationPromptTokenSize()
                    timeToFirstToken = firstTokenTs - startTime
                    prefillSpeed = prefillTokens / (timeToFirstToken / 1000f)
                    decodeSpeed = (decodeTokens - 1) / ((endTime - firstTokenTs) / 1000f)
                    val latencyMs: Long = endTime - startTime
                    val stats = InferenceStats(
                        prefillTokens = prefillTokens,
                        prefillSpeed = prefillSpeed,
                        decodeTokens = decodeTokens,
                        decodeSpeed = decodeSpeed,
                        timeToFirstToken = timeToFirstToken,
                        latency = latencyMs,
                    )
                    _messages[_messages.lastIndex] = _messages.last().copy(stats = stats)
                }
                .catch { e ->
                    releaseEngine()
                    _uiState.value = State.Error(e.message)
                }
                .collect { token ->
                    if (firstRun) {
                        firstTokenTs = System.currentTimeMillis()
                        firstRun = false
                    }
                    decodeTokens++

                    // Append each token to the full response and update the last message.
                    fullResponse.append(token)
                    _messages[_messages.lastIndex] =
                        _messages.last().copy(text = fullResponse.toString())
                }
        }
    }

    fun toggleStatsVisibility(messageIndex: Int) {
        val message = _messages[messageIndex]
        _messages[messageIndex] = message.copy(showStats = !message.showStats)
    }

    override fun onCleared() {
        super.onCleared()
        generationJob?.cancel()
        viewModelScope.launch {
            releaseEngine()
        }
    }

    private suspend fun releaseEngine() {
        engine?.unload()
        engine = null
        _messages.clear()
        _uiState.value = State.Uninitialized
    }

    private fun createEngineFromPath(modelPath: String, tokenizerPath: String): LlmInferenceEngine {
        val modelFile = File(modelPath)

        if (!modelFile.exists()) {
            throw FileNotFoundException("Path does not exist: $modelFile")
        }

        when {
            modelFile.isFile -> {
                return when (modelFile.extension.lowercase()) {
                    "gguf" -> {
                        _currentModelTag.value = "${modelFile.name} (llama.cpp)"
                        LlamaCppInference()
                    }

                    "task" -> {
                        _currentModelTag.value = "${modelFile.name} (LiteRT)"
                        MediaPipeInference(getApplication())
                    }

                    "pte" -> {
                        if (!File(tokenizerPath).isFile) {
                            throw IllegalArgumentException("Please select a tokenizer file for ExecuTorch.")
                        }
                        _currentModelTag.value = "${modelFile.name} (ExecuTorch)"
                        ExecuTorchInference()
                    }

                    "onnx" -> throw IllegalArgumentException("Please select a directory as the model path if using ONNX.")
                    else -> throw IllegalArgumentException("Unknown model file type: ${modelFile.name}")
                }
            }

            modelFile.isDirectory -> {
                val configFile = File(modelFile, "genai_config.json")
                return if (configFile.exists() && configFile.isFile) {
                    _currentModelTag.value = "${modelFile.name} (ONNX)"
                    OnnxInference()
                } else {
                    throw IllegalArgumentException("When using ONNX, ensure the path contains a valid genai_config.json file, or select a specific model file for using other engines")
                }
            }

            else -> {
                throw IllegalArgumentException("Path is neither a file nor a directory: $modelPath")
            }
        }
    }
}
