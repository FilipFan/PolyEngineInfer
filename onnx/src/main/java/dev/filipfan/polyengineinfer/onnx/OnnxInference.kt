package dev.filipfan.polyengineinfer.onnx

import ai.onnxruntime.genai.GeneratorParams
import android.util.Log
import dev.filipfan.polyengineinfer.api.LlmInferenceEngine
import dev.filipfan.polyengineinfer.api.LlmInferenceOptions
import dev.filipfan.polyengineinfer.api.LlmModelFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OnnxInference : LlmInferenceEngine {
    companion object {
        private const val TAG = "OnnxInference"
    }

    private var onnxAi: SimpleGenAiExtension? = null
    private var generatorParams: GeneratorParams? = null

    override suspend fun load(path: LlmModelFiles, options: LlmInferenceOptions) {
        withContext(Dispatchers.IO) {
            onnxAi?.close() // Close previous instance if any.
            onnxAi = SimpleGenAiExtension(path.modelPath).also { genAi ->
                generatorParams = genAi.createGeneratorParams().apply {
                    // See https://onnxruntime.ai/docs/genai/reference/config.html#search.
                    mapOf(
                        "max_length" to options.maxTokens,
                        "top_k" to options.topK,
                        "top_p" to options.topP,
                        "temperature" to options.temperature,
                    ).forEach { (key, value) ->
                        setSearchOption(key, value.toDouble())
                    }
                    // Enable to use random sampling (top-k/top-p).
                    setSearchOption("do_sample", true)
                }
            }
        }
    }

    override suspend fun unload() {
        withContext(Dispatchers.IO) {
            onnxAi?.close()
            onnxAi = null
            generatorParams = null
        }
    }

    override fun generate(prompt: String): Flow<String> {
        val currentEngine = checkNotNull(onnxAi) { "ONNX model not loaded." }

        return callbackFlow {
            val job = launch(Dispatchers.IO) {
                try {
                    currentEngine.generate(generatorParams, prompt) { token ->
                        trySend(token) // Send each generated token to the flow.
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected exception during inferencing", e)
                    close(e)
                } finally {
                    // Ensure the flow is closed when generation is complete.
                    close()
                }
            }
            // When the collector cancels, cancel the generation job.
            awaitClose { job.cancel() }
        }
    }

    override fun getLatestGenerationPromptTokenSize(): Int = onnxAi?.latestGenerationPromptTokenSize ?: -1
}
