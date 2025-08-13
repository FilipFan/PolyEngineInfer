package dev.filipfan.polyengineinfer.llamacpp

import dev.filipfan.polyengineinfer.api.LlmInferenceEngine
import dev.filipfan.polyengineinfer.api.LlmInferenceOptions
import dev.filipfan.polyengineinfer.api.LlmModelFiles
import dev.filipfan.polyengineinfer.llamacpp.internal.LLamaAndroid
import kotlinx.coroutines.flow.Flow

class LlamaCppInference : LlmInferenceEngine {
    private val llamaAndroid: LLamaAndroid = LLamaAndroid.instance()

    override suspend fun load(path: LlmModelFiles, options: LlmInferenceOptions) {
        llamaAndroid.load(path.modelPath, options)
    }

    override suspend fun unload() {
        llamaAndroid.unload()
    }

    override fun generate(prompt: String): Flow<String> = llamaAndroid.send(prompt)

    override fun getLatestGenerationPromptTokenSize(): Int = llamaAndroid.getLatestGenerationPromptTokenSize()
}
