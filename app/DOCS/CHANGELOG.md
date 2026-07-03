# PocketMind Change Log

All notable changes to the PocketMind project will be documented in this file.
  
  ## [Unreleased] - July 03, 2026
  
  ### Added
- Complete production architecture overhaul, custom downloader, and security patches
  
  ### Fixed
- Fix model download URLs to litert-community paths
  
  ### Changed
- Update status and build reports
  
  ## [Unreleased] - July 02, 2026
  
  ### Added
- **FEATURE: Model Downloader**: Built a custom OkHttp + Coroutines byte-stream downloader for 60fps real-time progress updates.
- **SEC: Token Storage**: Added secure Hugging Face token management via EncryptedSharedPreferences.
- **FEATURE: PDF Generation**: Added on-device PDF export for AI responses using iText7 and MediaStore (Scoped Storage). *(Note: Implemented but not tested. Not a safe branch.)*
- **FEATURE: Model Selection UI**: New Settings screen with manual model override (E4B, E2B, E2B_INT4) and RAM-aware auto-selection.
- **UI: Navigation**: Integrated Bottom Navigation Bar for switching between Chat and Settings.
- **CHORE: Dependency Update**: Upgraded to LiteRT-LM v0.13.1 for enhanced NPU support and better streaming.
- **SEC: Scoped Storage Patch**: Migrated PDF export to MediaStore API for Android 12+ compliance.
- **PERF: UX Optimization**: Added "Thinking..." state to reduce perceived latency during LLM prefill.
- **BUG: Crash Protection**: Implemented model file validation and fallback to prevent native crashes from missing assets.
- **BUG: Build Fix**: Resolved `libLiteRt.so` native library conflict in `build.gradle.kts`.
- **BUG: Permissions**: Added missing `INTERNET` and `ACCESS_NETWORK_STATE` permissions to `AndroidManifest.xml`.
- **BUG: Model Downloads**: Fixed 404 error by updating Gemma 4 download URLs to official `litert-community` paths.
  
  ### Fixed
- Fix model download URLs to litert-community paths
  
  ### Changed
- Update status and build reports

## [Unreleased] - July 01, 2026

### Fixed
- Fix model download URLs to litert-community paths
- **MainActivity.kt:** Resolved "Unresolved reference 'androidx'" build error by correcting extension function call syntax and adding missing imports for `consumeWindowInsets` and `imePadding`.
- **MainActivity.kt:** Cleaned up fully qualified names for `ChatScreenRoot` and `Modifier` extensions to improve readability and comply with standard Kotlin style.

### Added
- Complete production architecture overhaul, custom downloader, and security patches
- Test auto documentation trigger
- **Documentation:** Initialized `CHANGELOG.md` in the `DOCS` folder to track project evolution over time.

---
*Note: This log is maintained by the AI assistant as part of the development process.*
