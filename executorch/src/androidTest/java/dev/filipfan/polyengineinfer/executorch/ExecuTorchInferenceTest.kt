package dev.filipfan.polyengineinfer.executorch

import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.filipfan.polyengineinfer.api.LlmInferenceOptions
import dev.filipfan.polyengineinfer.api.LlmModelFiles
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ExecuTorchInferenceTest {
    companion object {
        private const val TAG = "ExecuTorchInferenceTest"
        private lateinit var modelFile: File
        private lateinit var tokenizerFile: File
        private lateinit var execuTorchInference: ExecuTorchInference

        private fun copyAssetFolder(
            context: Context,
            sourceAssetDir: String,
            destinationDir: File,
        ) {
            val assetManager = context.assets
            val assets = assetManager.list(sourceAssetDir)
                ?: throw IOException("Failed to list assets in directory: $sourceAssetDir")

            if (!destinationDir.exists()) {
                destinationDir.mkdirs()
            }

            for (assetName in assets) {
                val sourcePath =
                    if (sourceAssetDir.isEmpty()) assetName else "$sourceAssetDir/$assetName"
                val destFile = File(destinationDir, assetName)

                val subAssets = assetManager.list(sourcePath)

                if (!subAssets.isNullOrEmpty()) {
                    copyAssetFolder(context, sourcePath, destFile)
                } else {
                    try {
                        assetManager.open(sourcePath).use { inputStream ->
                            FileOutputStream(destFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    } catch (e: IOException) {
                        Log.w(
                            TAG,
                            "Could not copy asset: $sourcePath. It might be an empty directory.",
                            e,
                        )
                    }
                }
            }
        }

        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            val instrumentationContext = InstrumentationRegistry.getInstrumentation().targetContext
            val externalDir = instrumentationContext.getExternalFilesDir(null)
            assertNotNull("External files directory should not be null", externalDir)
            copyAssetFolder(instrumentationContext, "tinyllamas", externalDir!!)
            modelFile = File(externalDir, "stories15M_h.pte")
            tokenizerFile = File(externalDir, "tokenizer.bin")
            execuTorchInference = ExecuTorchInference()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            execuTorchInference.unload()
        }
    }

    @Test
    fun testLoadAndGenerateWithDefaultOptions() = runBlocking {
        execuTorchInference.load(
            LlmModelFiles(
                modelPath = modelFile.path,
                tokenizerPath = tokenizerFile.path,
            ),
            LlmInferenceOptions(),
        )
        val prompt = "Hello"
        val generatedTokens = execuTorchInference.generate(prompt).toList()

        val resultText = generatedTokens.joinToString("")

        Log.d(TAG, "Default Generation: $resultText")
        assertTrue("The generated text should not be empty.", resultText.isNotEmpty())
    }

    @Test
    fun testGenerationWithCustomMaxLength() = runBlocking {
        val maxTokens = 5
        val customOptions = LlmInferenceOptions(maxTokens = maxTokens)
        execuTorchInference.load(
            LlmModelFiles(
                modelPath = modelFile.path,
                tokenizerPath = tokenizerFile.path,
            ),
            customOptions,
        )

        val prompt = "Hello"
        val generatedTokens = execuTorchInference.generate(prompt).toList()

        Log.d(
            TAG,
            "Custom Max Length (${generatedTokens.size}): ${generatedTokens.joinToString("")}",
        )
        assertTrue(
            "Generated token count should be less than or equal to maxTokens",
            generatedTokens.size <= maxTokens,
        )
    }

    @Test
    fun testGreedyDecodingWithZeroTemperature() = runBlocking {
        val greedyOptions = LlmInferenceOptions(maxTokens = 50, temperature = 0.0f)
        execuTorchInference.load(
            LlmModelFiles(
                modelPath = modelFile.path,
                tokenizerPath = tokenizerFile.path,
            ),
            greedyOptions,
        )
        val prompt = "Hello"

        val firstResult = execuTorchInference.generate(prompt).toList().joinToString("")
        val secondResult = execuTorchInference.generate(prompt).toList().joinToString("")

        Log.d(TAG, "Greedy First Run: $firstResult")
        Log.d(TAG, "Greedy Second Run: $secondResult")

        assertTrue("Greedy results should not be empty", firstResult.isNotEmpty())
        assertEquals(
            "Greedy decoding should produce the same result every time",
            firstResult,
            secondResult,
        )
    }

    @Test
    fun testNonDeterministicGenerationWithTemperature() = runBlocking {
        val stochasticOptions = LlmInferenceOptions(maxTokens = 50, temperature = 0.9f, topK = 100)
        execuTorchInference.load(
            LlmModelFiles(
                modelPath = modelFile.path,
                tokenizerPath = tokenizerFile.path,
            ),
            stochasticOptions,
        )
        val prompt = "Hello"

        val firstResult = execuTorchInference.generate(prompt).toList().joinToString("")
        val secondResult = execuTorchInference.generate(prompt).toList().joinToString("")

        Log.d(TAG, "Stochastic First Run: $firstResult")
        Log.d(TAG, "Stochastic Second Run: $secondResult")

        assertTrue("Stochastic results should not be empty", firstResult.isNotEmpty())
        assertNotEquals(
            "Stochastic decoding should produce different results",
            firstResult,
            secondResult,
        )
    }

    @Test
    @Ignore("Top-K is not supported yet")
    fun testTopKParameterProducesDeterministicResultWhenKIsOne() = runBlocking {
        // Temperature is non-zero.
        val topKOptions = LlmInferenceOptions(maxTokens = 50, topK = 1, temperature = 0.9f)
        execuTorchInference.load(
            LlmModelFiles(
                modelPath = modelFile.path,
                tokenizerPath = tokenizerFile.path,
            ),
            topKOptions,
        )
        val prompt = "Hello"

        val firstResult = execuTorchInference.generate(prompt).toList().joinToString("")
        val secondResult = execuTorchInference.generate(prompt).toList().joinToString("")

        Log.d(TAG, "topK=1 First Run: $firstResult")
        Log.d(TAG, "topK=1 Second Run: $secondResult")

        assertTrue("topK=1 result should not be empty", firstResult.isNotEmpty())
        assertEquals("topK=1 should produce the same result every time", firstResult, secondResult)
    }
}
