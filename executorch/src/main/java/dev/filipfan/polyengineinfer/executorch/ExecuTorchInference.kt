package dev.filipfan.polyengineinfer.executorch

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
import org.json.JSONException
import org.json.JSONObject
import org.pytorch.executorch.extension.llm.LlmCallback
import org.pytorch.executorch.extension.llm.LlmModule

class ExecuTorchInference : LlmInferenceEngine {
    companion object {
        private const val TAG = "ExecuTorchInference"
    }

    private var llmModule: LlmModule? = null
    private var inferenceOptions: LlmInferenceOptions? = null
    private var recentPromptTokenSize = -1

    override suspend fun load(path: LlmModelFiles, options: LlmInferenceOptions) {
        withContext(Dispatchers.IO) {
            cleanUp()
            llmModule =
                LlmModule(
                    LlmModule.MODEL_TYPE_TEXT,
                    path.modelPath,
                    path.tokenizerPath,
                    options.temperature,
                ).apply {
                    load()
                }
            inferenceOptions = options
        }
    }

    override suspend fun unload() {
        withContext(Dispatchers.IO) {
            cleanUp()
        }
    }

    private fun cleanUp() {
        llmModule?.resetNative()
        llmModule = null
        inferenceOptions = null
        recentPromptTokenSize = -1
    }

    override fun generate(prompt: String): Flow<String> {
        val currentModule = checkNotNull(llmModule) { "ExecuTorch model not loaded." }
        val currentOptions = checkNotNull(inferenceOptions) { "ExecuTorch model options not set." }

        return callbackFlow {
            val job = launch(Dispatchers.IO) {
                val callback = object : LlmCallback {
                    override fun onResult(result: String) {
                        trySend(result)
                    }

                    override fun onStats(stats: String?) {
                        super.onStats(stats)
                        if (stats.isNullOrBlank()) {
                            return
                        }
                        recentPromptTokenSize = try {
                            val jsonObject = JSONObject(stats)
                            jsonObject.getInt("prompt_tokens")
                        } catch (e: JSONException) {
                            Log.e(TAG, "Failed to parse stats", e)
                            -1
                        }
                    }
                }
                try {
                    currentModule.generate(
                        prompt,
                        currentOptions.maxTokens, // maximum number of total tokens.
                        callback,
                        false, // `true` for text completion, `false` for chat
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected exception during inferencing", e)
                    close(e)
                } finally {
                    close()
                }
            }
            awaitClose { job.cancel() }
        }
    }

    override fun getLatestGenerationPromptTokenSize() = recentPromptTokenSize
}
