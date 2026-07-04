# About PocketMind

## The Vision
PocketMind was born out of a desire for absolute digital privacy without sacrificing the incredible utility of modern Large Language Models (LLMs). As AI becomes more integrated into our daily lives, the reliance on cloud infrastructure introduces latency, subscription costs, and significant data privacy concerns. 

PocketMind aims to solve this by putting the AI directly in your pocket.

## Core Philosophy
1. **Privacy Above All**: Your conversations never leave your device. There are no telemetry servers, no cloud APIs, and no data harvesting.
2. **Accessible AI**: By optimizing for mobile hardware (NPU/GPU), PocketMind democratizes access to state-of-the-art models like Google's Gemma.
3. **Open Ecosystem**: We believe in open-source collaboration. PocketMind is built to be a reference architecture for Android developers looking to integrate on-device LLMs using Clean Architecture and Jetpack Compose.

## The Technology
Under the hood, PocketMind leverages the cutting-edge **LiteRT** (formerly AI Edge / TensorFlow Lite) C++ APIs through a custom Kotlin JNI bridge. It manages memory intelligently using KV cache preservation for multi-turn conversations and gracefully falls back between NPU, GPU, and XNNPACK CPU threads based on your specific mobile hardware capabilities.

Join us in building the future of decentralized, on-device artificial intelligence!
