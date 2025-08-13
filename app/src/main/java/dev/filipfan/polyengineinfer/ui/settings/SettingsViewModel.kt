package dev.filipfan.polyengineinfer.ui.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Represents the configuration for loading and running a Large Language Model. */
data class LlmSettings(
    val modelPath: String,
    val tokenizerPath: String = "",
    val maxTokens: Int = 512,
    val topK: Int = 40,
    val topP: Float = 1.0f,
    val temperature: Float = 0.8f,
)

/** A ViewModel responsible for managing and exposing the settings for the UI. */
class SettingsViewModel : ViewModel() {
    private val _settings = MutableStateFlow(LlmSettings(""))
    val settings: StateFlow<LlmSettings> = _settings.asStateFlow()

    fun updateSettings(newSettings: LlmSettings) {
        _settings.value = newSettings
    }
}
