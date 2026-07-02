# PocketMind Change Log

All notable changes to the PocketMind project will be documented in this file.
  
  ## [Unreleased] - July 02, 2026
  
  ### Added
- **FEATURE: PDF Generation**: Added on-device PDF export for AI responses using iText7. *(Note: Implemented but not tested. Not a safe branch.)*
- **FEATURE: Model Selection UI**: New Settings screen with manual model override (E4B, E2B, E2B_INT4) and RAM-aware auto-selection.
- **UI: Navigation**: Integrated Bottom Navigation Bar for switching between Chat and Settings.
- **CHORE: Dependency Update**: Upgraded to LiteRT-LM v0.13.1 for enhanced NPU support and better streaming.
  
  ### Fixed
  
  ### Changed

## [Unreleased] - July 01, 2026

### Fixed
- **MainActivity.kt:** Resolved "Unresolved reference 'androidx'" build error by correcting extension function call syntax and adding missing imports for `consumeWindowInsets` and `imePadding`.
- **MainActivity.kt:** Cleaned up fully qualified names for `ChatScreenRoot` and `Modifier` extensions to improve readability and comply with standard Kotlin style.

### Added
- Test auto documentation trigger
- **Documentation:** Initialized `CHANGELOG.md` in the `DOCS` folder to track project evolution over time.

---
*Note: This log is maintained by the AI assistant as part of the development process.*
