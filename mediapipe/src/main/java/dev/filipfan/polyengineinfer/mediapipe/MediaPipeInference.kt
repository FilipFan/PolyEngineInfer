package dev.filipfan.polyengineinfer.mediapipe

import android.content.Context
import android.util.Log
import com.google.common.util.concurrent.ListenableFuture
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import dev.filipfan.polyengineinfer.api.LlmInferenceEngine
import dev.filipfan.polyengineinfer.api.LlmInferenceOptions
import dev.filipfan.polyengineinfer.api.LlmModelFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class MediaPipeInference(private val context: Context) : LlmInferenceEngine {
    companion object {
        private const val TAG = "MediaPipeInference"
    }

    private data class LlmModelInstance(
        val engine: LlmInference,
        var session: LlmInferenceSession?,
        val options: LlmInferenceOptions,
    )

    private var instance: LlmModelInstance? = null
    private var recentPrompt: String? = null

    override suspend fun load(path: LlmModelFiles, options: LlmInferenceOptions) {
        withContext(Dispatchers.IO) {
            cleanUp()
            val engine = LlmInference.createFromOptions(
                context,
                LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(path.modelPath)
                    .setMaxTokens(options.maxTokens)
                    .setPreferredBackend(LlmInference.Backend.CPU)
                    .build(),
            )
            instance = LlmModelInstance(engine = engine, session = null, options = options)
        }
    }

    override suspend fun unload() {
        withContext(Dispatchers.IO) {
            cleanUp()
        }
    }

    private fun cleanUp() {
        instance?.let {
            try {
                it.session?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to close the LLM Inference session", e)
            }
            try {
                it.engine.close()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to close the LLM Inference engine", e)
            }
            instance = null
        }
        recentPrompt = null
    }

    private fun createNewSession(instance: LlmModelInstance): LlmInferenceSession {
        instance.session?.close()
        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder().apply {
            setTopK(instance.options.topK)
            setTopP(instance.options.topP)
            setTemperature(instance.options.temperature)
            if (instance.options.temperature != 0.0f) {
                // Make the random sampling non-deterministic.
                setRandomSeed(Random.nextInt())
            }
            setGraphOptions(
                GraphOptions.builder()
                    .setEnableVisionModality(false)
                    .build(),
            )
        }.build()

        return LlmInferenceSession.createFromOptions(instance.engine, sessionOptions).also {
            instance.session = it
        }
    }

    override fun generate(prompt: String): Flow<String> {
        val currentInstance = checkNotNull(instance) { "MediaPipe model not loaded." }

        var generationFuture: ListenableFuture<String>? = null

        return callbackFlow {
            val job = launch(Dispatchers.IO) {
                try {
                    // TODO: keep session.
                    val session = createNewSession(currentInstance)
                    if (prompt.isNotBlank()) {
                        session.addQueryChunk(prompt)
                        recentPrompt = prompt
                    }
                    generationFuture = session.generateResponseAsync { partialResult, done ->
                        trySend(partialResult)
                        if (done) {
                            close()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected exception during inferencing", e)
                    close(e)
                }
            }
            awaitClose {
                job.cancel()
                generationFuture?.cancel(true)
            }
        }
    }

    override fun getLatestGenerationPromptTokenSize(): Int = instance?.let { inst ->
        inst.session?.sizeInTokens(recentPrompt) ?: -1
    } ?: error("MediaPipe model not loaded.")
}
