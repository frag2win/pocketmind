# PocketMind Change Log

All notable changes to the PocketMind project will be documented in this file.
  
  ## [Unreleased] - August 06, 2026
  
  ### Added
- Add document parsing and attachment support to chat UI
- **FEATURE: AI Canvas Bridge Skeleton**: Added `buildCanvasPrompt` system prompt in `PromptBuilder`, created `CanvasScreen` and `CanvasViewModel` with embedded WebView JS evaluation bridge.
  
  ### Fixed
- **BUG: Token Output Serialization**: Refactored LiteRTInferenceEngine token extraction logic to extract raw Content.Text fragments from Message turns instead of relying on toString(), preventing metadata leak into the chat stream and adding mid-stream exception handling.
- **UI: Navigation & TopBar Cleanup**: Removed non-functional dropdown arrow from top app bar, replaced hardcoded developer name in navigation drawer and empty chat state with clean placeholders, and standardized drawer padding grid.
- **UI: Settings Top Padding & User Profile**: Reduced top inset gap on Settings screen, added Back navigation button, added Display Name input bound to EncryptedSharedPreferences, and dynamically updated navigation drawer user label.
- **FEATURE: Display Name & System Prompt Awareness**: Integrated `KEY_DISPLAY_NAME` into EncryptedSharedPreferences, cleaned up GitHub input from Settings, removed GitHub Browser item from navigation drawer, and injected user self-awareness preamble into local Gemma prompt formatting.
- **UI: Chat Interface Refinement**: Enhanced user message bubble contrast with slate grey background, subtle stroke and elevation, standardized 16dp spacing between AI response action icons (Copy, PDF), and applied safe navigation bar padding to bottom text input pill.
- **BUG & UX: Prompt Tag Leak & Theme Adaptive Rendering**: Sanitized raw model output to strip `<start_of_turn>` / `<end_of_turn>` prompt tags, fixed token space stripping during streaming, restored instruction-tuned Gemma turn history formatting, and made Markdown, Streaming text, background gradients, user bubbles, and input pill adaptively styled for full high-contrast legibility in both Android Light Mode and Dark Mode.
  
  ### Changed
  
  ## [Unreleased] - July 13, 2026
  ### Added
- Add document parsing and attachment support to chat UI
- Register GitHub navigation routes
- **FEATURE: AI GitHub Analysis**: Enabled AI-powered code explanation and Pull Request summarization directly from the GitHub browser UI.
- **UI: AI Action Buttons**: Added "AutoAwesome" icons to GitHub list items to clearly indicate available AI actions.
- **UX: GitHub Token Validation**: Implemented a "No Token" state in the GitHub browser with a direct link to Settings for easier setup.
  ## [Unreleased] - July 10, 2026
  
  ### Added
- Add document parsing and attachment support to chat UI
- Register GitHub navigation routes
- **FEATURE: Deep Web-RAG**: Implemented on-device page scraping to fetch actual article content from search results, providing the AI with rich live data instead of just links.
- **FEATURE: API-Free Search**: Added a fallback mechanism using Jsoup to scrape DuckDuckGo's static HTML, enabling real-time grounding without an external API key.
- **FEATURE: Full-Text Search (FTS)**: Integrated Room FTS4 for lightning-fast local indexing of all conversation history, accessible via a new sidebar search bar.
- **FEATURE: Auto-Titling**: Implemented automatic chat session titling after the first exchange, using the local AI to summarize the conversation into a 3-5 word title.

  ### Fixed
- **UI: Bouncing & Jitter**: Fundamentally resolved the violent screen bouncing and jitter during streaming by implementing `reverseLayout` and `Hybrid Rendering` (Text for streaming, Markdown for final).
- **UI: Scroll Hijacking**: Fixed the auto-scroll conflict by implementing a state-driven scroll lock that respects user manual gestures.
- **UI: Markdown Flashing**: Eliminated raw syntax flashing (e.g., `**`, `##`) during streaming by creating a stream-safe `toLiveAnnotatedString` parser.
- **BUG: Shared State**: Fixed "New Chat" button failure by hoisting the `ChatViewModel` in `MainActivity` to ensure synchronized state between the sidebar and chat screen.
- **UI: Model Loading**: Fixed the persistent model loading overlay bug to ensure it only appears when the engine is not ready.

  ## [Unreleased] - July 09, 2026
  
  ### Added
- Add document parsing and attachment support to chat UI
- Register GitHub navigation routes
  
  ### Fixed
  
  ### Changed
- Update changelog and status for GitHub UI
- optimize streaming message rendering and update markdown styling
- **PERF: Streaming**: Optimized chat streaming performance and markdown rendering styling.
  
  ## [Unreleased] - July 06, 2026
  
  ### Added
- Add document parsing and attachment support to chat UI
- Register GitHub navigation routes
- Implement Web Search (RAG) grounding via Tavily API
  
  ### Fixed
  
  ### Changed
- Update changelog and status for GitHub UI
- optimize streaming message rendering and update markdown styling
  
  ## [Unreleased] - July 04, 2026
  
  ### Added
- Add document parsing and attachment support to chat UI
- Register GitHub navigation routes
- Implement Web Search (RAG) grounding via Tavily API
- Add Share PDF, Clear Chat, Copy to Clipboard, Context Management, and resolve UI streaming bugs
- **FEATURE: Share PDF**: Added "Share" button to the PDF generation dialog to easily share exported transcripts.
- **FEATURE: Clear Chat**: Added manual "Clear Chat" button to reset conversation context.
- **REFACTOR: Context Management**: Implemented sliding-window history truncation to strictly stay within the 4096 token limit.
- **FEATURE: Copy to Clipboard**: Added "Copy All" button to chat messages and wrapped Markdown in a `SelectionContainer` for granular copying.
- **UI: Professional Markdown**: Integrated Material 3 Markdown rendering with custom slate-themed colors and typography.
  
  ### Fixed
- Implement Midnight Slate premium theme and Markdown rendering
- **BUG: Keyboard Visibility**: Fixed issue where the input field was hidden behind the keyboard by correctly applying `imePadding`.
- **BUG: Text Blinking**: Mitigated text "blinking" during streaming by ensuring the Markdown component maintains state between updates.
- **BUG: UI Jitter**: Resolved "bouncing" effect during AI streaming by switching to `scrollToItem` and implementing stable `LazyColumn` keys.
- **BUG: Scroll Control**: Refined auto-scroll logic to prevent snapping back to bottom if the user manually scrolls up to read history during generation.
- **BUG: Keyboard Gap**: Eliminated the large gap between keyboard and input field by hiding the `NavigationBar` during typing and fixing inset consumption.
- **BUG: Build System**: Resolved major Kotlin/KSP metadata incompatibility (`2.4.0` vs `2.2.0`) by aligning versions and forcing consistent `kotlin-stdlib` resolution.
- **BUG: SDK Conflict**: Updated `compileSdk` and `targetSdk` to 37 to meet newer dependency requirements.

  ## [Unreleased] - July 03, 2026
  
  ### Added
- Add document parsing and attachment support to chat UI
- Register GitHub navigation routes
- Implement Web Search (RAG) grounding via Tavily API
- Add Share PDF, Clear Chat, Copy to Clipboard, Context Management, and resolve UI streaming bugs
- Complete production architecture overhaul, custom downloader, and security patches
  
  ### Fixed
- Implement Midnight Slate premium theme and Markdown rendering
- **BUG: ClassCastException**: Resolved crash when streaming tokens by correctly handling the LiteRT-LM `Message` object.
- **BUG: Permissions**: Added missing `INTERNET` and `ACCESS_NETWORK_STATE` permissions to `AndroidManifest.xml`.
- **BUG: Model Downloads**: Fixed 404 error by updating Gemma 4 download URLs to official `litert-community` paths.
  
  ### Changed
- Update changelog and status for GitHub UI
- optimize streaming message rendering and update markdown styling
- Add README, ABOUT, and CODE_OF_CONDUCT for community standards
- synchronize changelog updates
- Update status and build reports
  
  ## [Unreleased] - July 02, 2026
  
  ### Added
- Add document parsing and attachment support to chat UI
- Register GitHub navigation routes
- Implement Web Search (RAG) grounding via Tavily API
- Add Share PDF, Clear Chat, Copy to Clipboard, Context Management, and resolve UI streaming bugs
- **FEATURE: Model Downloader**: Built a custom OkHttp + Coroutines byte-stream downloader for 60fps real-time progress updates.
- **SEC: Token Storage**: Added secure Hugging Face token management via EncryptedSharedPreferences.
- **FEATURE: PDF Generation**: Added on-device PDF export for AI responses using iText7 and MediaStore (Scoped Storage). *(Tested and verified on device. Safe branch.)*
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
- Implement Midnight Slate premium theme and Markdown rendering
- **BUG: ClassCastException**: Resolved crash when streaming tokens by correctly handling the LiteRT-LM `Message` object.
- **BUG: Permissions**: Added missing `INTERNET` and `ACCESS_NETWORK_STATE` permissions to `AndroidManifest.xml`.
- **BUG: Model Downloads**: Fixed 404 error by updating Gemma 4 download URLs to official `litert-community` paths.
  
  ### Changed
- Update changelog and status for GitHub UI
- optimize streaming message rendering and update markdown styling
- Add README, ABOUT, and CODE_OF_CONDUCT for community standards
- synchronize changelog updates
- Update status and build reports

## [Unreleased] - July 01, 2026

### Fixed
- Implement Midnight Slate premium theme and Markdown rendering
- Fix model download URLs to litert-community paths
- **MainActivity.kt:** Resolved "Unresolved reference 'androidx'" build error by correcting extension function call syntax and adding missing imports for `consumeWindowInsets` and `imePadding`.
- **MainActivity.kt:** Cleaned up fully qualified names for `ChatScreenRoot` and `Modifier` extensions to improve readability and comply with standard Kotlin style.

### Added
- Add document parsing and attachment support to chat UI
- Register GitHub navigation routes
- Implement Web Search (RAG) grounding via Tavily API
- Add Share PDF, Clear Chat, Copy to Clipboard, Context Management, and resolve UI streaming bugs
- Complete production architecture overhaul, custom downloader, and security patches
- Test auto documentation trigger
- **Documentation:** Initialized `CHANGELOG.md` in the `DOCS` folder to track project evolution over time.

---
*Note: This log is maintained by the AI assistant as part of the development process.*
