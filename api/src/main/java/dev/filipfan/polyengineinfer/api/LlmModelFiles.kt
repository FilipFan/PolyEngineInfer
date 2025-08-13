package dev.filipfan.polyengineinfer.api

data class LlmModelFiles(
    /** Path to the model for the task. */
    val modelPath: String,
    /** Path to the tokenizer file. */
    val tokenizerPath: String = "",
)
