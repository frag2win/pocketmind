# PocketMind Inference Layer Architecture

The inference layer is designed to be multi-chipset aware, providing optimal performance across various Android hardware.

## Core Components

### 1. `PocketMindInference` Interface
Located in `domain/inference/`, this defines the contract for all inference engines:
- `generate(prompt: String): String`
- `generateStream(prompt: String): Flow<String>`
- `isReady(): Boolean`

### 2. `InferenceFactory`
Located in `data/inference/`, this factory detects hardware strings (`Build.HARDWARE`, `Build.BOARD`) and maps them to:
- **Google Tensor** -> `AICoreInference`
- **Qualcomm Snapdragon** -> `QNNInference`
- **MediaTek Dimensity** -> `NeuronInference`
- **Fallback** -> `LiteRTCPUInference`

### 3. Dependency Injection
The `InferenceModule` in `di/` uses Hilt to provide the `PocketMindInference` instance, ensuring the application always uses the most capable engine for the current device.

## Sequence Flow
1. App launches and Hilt requests `PocketMindInference`.
2. `InferenceModule` calls `InferenceFactory.getInferenceImplementation()`.
3. `InferenceFactory` inspects device hardware.
4. Correct implementation is initialized and returned as a Singleton.
