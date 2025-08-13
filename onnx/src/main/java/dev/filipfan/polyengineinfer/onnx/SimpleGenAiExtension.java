/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package dev.filipfan.polyengineinfer.onnx;

import ai.onnxruntime.genai.GenAIException;
import ai.onnxruntime.genai.Generator;
import ai.onnxruntime.genai.GeneratorParams;
import ai.onnxruntime.genai.Model;
import ai.onnxruntime.genai.Sequences;
import ai.onnxruntime.genai.Tokenizer;
import ai.onnxruntime.genai.TokenizerStream;
import java.util.function.Consumer;

/**
 * Source from: <a
 * href="https://github.com/microsoft/onnxruntime-genai/blob/v0.8.3/src/java/src/main/java/ai/onnxruntime/genai/SimpleGenAI.java"/>.
 * Expose an API to get recent prompt token size.
 */
class SimpleGenAiExtension implements AutoCloseable {
  private Model model;
  private Tokenizer tokenizer;
  private int recentPromptTokenSize = -1;

  public SimpleGenAiExtension(String modelPath) throws GenAIException {
    model = new Model(modelPath);
    tokenizer = new Tokenizer(model);
  }

  public GeneratorParams createGeneratorParams() throws GenAIException {
    return new GeneratorParams(model);
  }

  public String generate(
      GeneratorParams generatorParams, String prompt, Consumer<String> listener) {
    try {
      try (Generator generator = new Generator(model, generatorParams);
          TokenizerStream stream = (listener != null) ? tokenizer.createStream() : null) {
        Sequences sequences = tokenizer.encode(prompt);
        recentPromptTokenSize = sequences.getSequence(0).length;
        generator.appendTokenSequences(sequences);
        for (int tokenId : generator) {
          if (stream != null) {
            listener.accept(stream.decode(tokenId));
          }
        }

        int[] outputIds = generator.getSequence(0);

        return tokenizer.decode(outputIds);

      } catch (GenAIException e) {
        throw new RuntimeException("Failed to generate a response from the model.", e);
      }
    } catch (Exception e) {
      throw new RuntimeException("An unexpected error occurred during token generation.", e);
    }
  }

  public int getLatestGenerationPromptTokenSize() {
    return recentPromptTokenSize;
  }

  @Override
  public void close() {
    if (tokenizer != null) {
      tokenizer.close();
      tokenizer = null;
    }
    if (model != null) {
      model.close();
      model = null;
    }
    recentPromptTokenSize = -1;
  }
}
