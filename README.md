# PocketMind 🧠

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![TensorFlow Lite](https://img.shields.io/badge/TensorFlow_Lite-FF6F00?style=for-the-badge&logo=tensorflow&logoColor=white)

PocketMind is a fully offline, privacy-first conversational AI application for Android, powered by the Google Gemma models via LiteRT (formerly TensorFlow Lite). 

## 🌟 Features
- **100% Offline Inference:** Runs entirely on your Android device—no cloud, no API keys, absolute privacy.
- **Hardware Acceleration:** Native NPU and GPU delegation using `com.google.ai.edge.litert` for blazing-fast token generation.
- **Dynamic Model Downloader:** Custom high-performance OkHttp streaming downloader with live 60fps progress to fetch models (E4B, E2B, INT4) directly to local storage.
- **Production UI/UX:** Built with Jetpack Compose, featuring a premium "Midnight Slate" theme, live markdown rendering, and auto-scrolling chat.
- **Export & Share:** Native on-device PDF generation via iText7 and MediaStore integration to safely export your conversations.
- **Clean Architecture:** Built using MVVM, Hilt Dependency Injection, and strict unidirectional data flow.

## 🚀 Getting Started

1. **Clone the Repository**
   ```bash
   git clone https://github.com/frag2win/pocketmind.git
   ```
2. **Build and Install**
   Open the project in Android Studio and run the app on a physical device (Emulators may lack the necessary NPU/GPU drivers for optimal performance).
3. **Download a Model**
   Navigate to the Settings screen to download the Gemma model of your choice directly to the device.

## 🤝 Community
Please see our [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) to understand our community standards, and [ABOUT.md](ABOUT.md) for more details on the vision behind PocketMind.
