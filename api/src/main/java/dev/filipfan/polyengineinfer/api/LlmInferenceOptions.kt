package dev.filipfan.polyengineinfer.api

data class LlmInferenceOptions(
    /** The total number of input + output tokens the model needs to handle. */
    val maxTokens: Int = 512,
    /** Top-K number of tokens to be sampled from for each decoding step. */
    val topK: Int = 40,
    /** Top-P (nucleus) sampling parameter. */
    val topP: Float = 1.0f,
    /** Randomness when decoding the next token. A value of 0.0f means greedy decoding. */
    val temperature: Float = 0.8f,
) {
    init {
        require(maxTokens > 0) { "maxTokens must be positive." }
        require(topK > 0) { "topK must be positive." }
        require(topP in 0.0f..1.0f) { "topP must be in the range [0.0, 1.0]." }
        require(temperature in 0.0f..2.0f) { "temperature must be in the range [0.0, 2.0]." }
    }
}
