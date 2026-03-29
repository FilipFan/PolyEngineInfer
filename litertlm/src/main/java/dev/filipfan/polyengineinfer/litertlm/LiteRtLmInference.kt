package dev.filipfan.polyengineinfer.litertlm

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
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

class LiteRtLmInference(private val context: Context) : LlmInferenceEngine {
    companion object {
        private const val TAG = "LiteRtLmInference"
    }

    private data class LlmModelInstance(
        val engine: Engine,
        var session: Conversation?,
        val options: LlmInferenceOptions,
    )

    private var instance: LlmModelInstance? = null

    init {
        @OptIn(ExperimentalApi::class)
        ExperimentalFlags.enableBenchmark = true
    }

    override suspend fun load(path: LlmModelFiles, options: LlmInferenceOptions) {
        withContext(Dispatchers.IO) {
            cleanUp()
            val preferredBackend =
                when (options.backend) {
                    LlmInferenceOptions.Backend.CPU -> Backend.CPU()
                    LlmInferenceOptions.Backend.GPU -> Backend.GPU()
                }

            val engine = Engine(
                EngineConfig(
                    modelPath = path.modelPath,
                    backend = preferredBackend,
                    visionBackend = null,
                    audioBackend = null,
                    maxNumTokens = options.maxTokens,
                    cacheDir = context.externalCacheDir?.path,
                ),
            )
            engine.initialize()
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
    }

    private fun createNewSession(instance: LlmModelInstance): Conversation {
        instance.session?.close()

        val sessionConfig =
            ConversationConfig(
                systemInstruction = null,
                tools = emptyList(),
                samplerConfig = SamplerConfig(
                    topK = instance.options.topK,
                    topP = instance.options.topP.toDouble(),
                    temperature = instance.options.temperature.toDouble(),
                    // Make the random sampling non-deterministic.
                    seed = if (instance.options.temperature != 0.0f) Random.nextInt() else 0,
                ),
            )

        return instance.engine.createConversation(sessionConfig).also {
            instance.session = it
        }
    }

    override fun generate(prompt: String): Flow<String> {
        val currentInstance = checkNotNull(instance) { "LiteRT-LM model not loaded." }

        return callbackFlow {
            val job = launch(Dispatchers.IO) {
                try {
                    // TODO: keep session.
                    val session = createNewSession(currentInstance)
                    val contents = mutableListOf<Content>()
                    if (prompt.isNotBlank()) {
                        contents.add(Content.Text(prompt))
                    }
                    val messageCallback = object : MessageCallback {
                        override fun onMessage(message: Message) {
                            trySend(message.toString())
                        }

                        override fun onDone() {
                            close()
                        }

                        override fun onError(throwable: Throwable) {
                            close(throwable)
                        }
                    }
                    session.sendMessageAsync(Message.of(contents), messageCallback)
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected exception during inferencing", e)
                    close(e)
                }
            }
            awaitClose {
                job.cancel()
                currentInstance.session?.cancelProcess()
            }
        }
    }

    @OptIn(ExperimentalApi::class)
    override fun getLatestGenerationPromptTokenSize(): Int = instance?.let { inst ->
        inst.session?.getBenchmarkInfo()?.lastPrefillTokenCount ?: -1
    } ?: error("LiteRT-LM model not loaded.")
}
