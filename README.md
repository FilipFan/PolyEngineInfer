#  Poly Engine Inference 🪄

An experimental Android application that integrates multiple on-device inference engines, allowing you to run inference with different engines within a single app.

## Features

- **Multi-Engine Support:** Load and chat with models supported by different inference engines.
- **Adjustable Parameters:** Adjust inference parameters such as top-k, top-p and temperature.
- **Performance Metrics:** View detailed inference data (time to first token, prefill speed, decode speed).

## Build Instructions

First, clone the repository and its submodules:

```
git clone https://github.com/FilipFan/PolyEngineInfer.git
cd PolyEngineInfer
git submodule update --init --recursive
```

Next, build the project using Gradle:

```
./gradlew clean build
```

## Installation

Install the application on a real device or an emulator using ADB. The app currently supports `arm64-v8a` and `x86_64` architectures.

```
adb install app-release.apk
```

## How to Use the App

The application loads models from the app-specific directory in [external storage](https://developer.android.com/training/data-storage/app-specific#external-access-files). Before selecting a model, you need to first push the model files to this directory (typically `/storage/emulated/0/Android/data/dev.filipfan.polyengineinfer/files`).

The app automatically selects the appropriate inference engine based on the model file's extension and directory structure. You can find pre-converted models for various engines for popular open-source LLMs. For example, using Llama-3.2-1B, you can download the following versions:

- **llama.cpp:** [hugging-quants/Llama-3.2-1B-Instruct-Q8_0-GGUF](https://huggingface.co/hugging-quants/Llama-3.2-1B-Instruct-Q8_0-GGUF/tree/main)
- **ONNX:** [onnx-community/Llama-3.2-1B-Instruct](https://huggingface.co/onnx-community/Llama-3.2-1B-Instruct/tree/main)
- **ExecuTorch:** [executorch-community/Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8-ET](https://huggingface.co/executorch-community/Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8-ET/tree/main)
- **LiteRT:** [litert-community/Llama-3.2-1B-Instruct](https://huggingface.co/litert-community/Llama-3.2-1B-Instruct/tree/main)

Download the model and push it to your device using `adb push`. For example:

```
adb push llama-3.2-1b-instruct-q8_0.gguf /storage/emulated/0/Android/data/dev.filipfan.polyengineinfer/files
```

> [!NOTE]
>
> **For ExecuTorch:** You must push both the model (`.pte`) and tokenizer (`.model`) files.

> [!NOTE]
>
> **For ONNX:** You need to push the entire directory containing the model and configuration files. When selecting the model in the app, choose this directory as the model path.
>
> To use the [ONNX Runtime generate() API](https://onnxruntime.ai/docs/genai/), you may need to further process the downloaded model files to create `genai_config.json`, `tokenizer.json`, etc. Refer to the [ONNX Runtime GenAI Model Builder](https://github.com/microsoft/onnxruntime-genai/tree/v0.8.3/src/python/py/models#config-only) for details.
>
> For instance, after downloading `onnx-community/Llama-3.2-1B-Instruct`, run the following command to generate the necessary files to be pushed to the device:
>
> ```
> python3 -m onnxruntime_genai.models.builder \
>   --input Llama-3.2-1B-Instruct \
>   -o onnx_test_dir \
>   -p int4 \
>   -e cpu \
>   -c onnx_test_dir/cache \
>   --extra_options config_only=true
> ```

Once the files are on your device, you can select the model from the app's settings page and start chatting.

## Dependencies

This project utilizes the following inference engines and versions:

- **llama.cpp:** [b6018](https://github.com/ggml-org/llama.cpp/releases/tag/b6018)

- **ONNX:**

  - onnxruntime-genai: [v0.8.3](https://github.com/microsoft/onnxruntime-genai/releases/tag/v0.8.3)
  - onnxruntime: [v1.22.0](https://github.com/microsoft/onnxruntime/releases/tag/v1.22.0)

- **ExecuTorch:** [v0.7.0](https://github.com/pytorch/executorch/releases/tag/v0.7.0)

- **MediaPipe (LiteRT):** [v0.10.26](https://github.com/google-ai-edge/mediapipe/releases/tag/v0.10.26)

## Current Limitations

- **No Chat Template:** Chat templates, including roles and special tokens, are not implemented.

- **Stateless Conversations:** The context of multi-turn conversations is not preserved; each interaction is a new session.

- **Text-Only:** The app does not handle multimodal inputs.

- **No Hardware Acceleration:** The current version does not include optimizations for specific hardware backends.

## References

This project was developed with reference to the official documentation and open-source examples from the following sources:

- [Google AI Edge](https://ai.google.dev/edge)
- [ONNX Runtime](https://onnxruntime.ai/docs/)
- [ExecuTorch](https://docs.pytorch.org/executorch/stable/intro-overview.html)
- [llama.cpp](https://github.com/ggml-org/llama.cpp)
