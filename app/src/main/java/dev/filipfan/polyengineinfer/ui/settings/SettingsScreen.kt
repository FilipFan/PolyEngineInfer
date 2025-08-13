package dev.filipfan.polyengineinfer.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.io.File

private enum class TargetFileType {
    Model,
    Tokenizer,
}

@Composable
fun SettingsScreen(
    currentSettings: LlmSettings,
    onSave: (LlmSettings) -> Unit,
    onCancel: () -> Unit,
) {
    var modelPath by remember { mutableStateOf(currentSettings.modelPath) }
    var tokenizerPath by remember { mutableStateOf(currentSettings.tokenizerPath) }
    var maxTokens by remember { mutableStateOf(currentSettings.maxTokens.toString()) }
    var topK by remember { mutableIntStateOf(currentSettings.topK) }
    var topP by remember { mutableFloatStateOf(currentSettings.topP) }
    var temperature by remember { mutableFloatStateOf(currentSettings.temperature) }

    var showFileSelectorFor by remember { mutableStateOf<TargetFileType?>(null) }

    if (showFileSelectorFor != null) {
        FileSelectorDialog(
            onFileSelected = { file ->
                if (showFileSelectorFor == TargetFileType.Model) {
                    modelPath = file.absolutePath
                } else if (showFileSelectorFor == TargetFileType.Tokenizer) {
                    tokenizerPath = file.absolutePath
                }
                showFileSelectorFor = null
            },
            onDismiss = { showFileSelectorFor = null },
        )
    }
    /* ==== The settings screen layout. ==== */
    Dialog(onDismissRequest = onCancel) {
        Card(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.padding(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Settings", style = MaterialTheme.typography.titleLarge)

                // File selectors.
                SettingsItem(label = "Model File", value = File(modelPath).name) {
                    showFileSelectorFor = TargetFileType.Model
                }
                SettingsItem(label = "Tokenizer File", value = File(tokenizerPath).name) {
                    showFileSelectorFor = TargetFileType.Tokenizer
                }

                // Max Tokens input.
                OutlinedTextField(
                    value = maxTokens,
                    onValueChange = { maxTokens = it.filter { c -> c.isDigit() } },
                    label = { Text("Max Tokens") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                // Sliders: Top-K, Top-P, Temperature.
                SliderSettingsItem("Top-K", topK.toFloat(), 1f..50f, 0) { topK = it.toInt() }
                SliderSettingsItem("Top-P", topP, 0f..1f, 1) { topP = it }
                SliderSettingsItem("Temperature", temperature, 0f..2f, 1) { temperature = it }

                // Action Buttons.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val newSettings = LlmSettings(
                            modelPath = modelPath,
                            tokenizerPath = tokenizerPath,
                            maxTokens = maxTokens.toIntOrNull() ?: currentSettings.maxTokens,
                            topK = topK,
                            topP = topP,
                            temperature = temperature,
                        )
                        onSave(newSettings)
                    }) {
                        Text("OK")
                    }
                }
            }
        }
    }
}
