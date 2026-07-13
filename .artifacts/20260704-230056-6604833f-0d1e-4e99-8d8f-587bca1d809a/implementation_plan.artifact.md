# Implementation Plan: GitHub Integration & Local AI Analysis

This plan outlines the integration of GitHub's REST API into PocketMind, enabling the local Gemma model to perform code analysis, PR summarization, and repository browsing while maintaining strict on-device privacy.

## User Review Required

> [!IMPORTANT]
> **GitHub Personal Access Token (PAT)**: Users must generate a classic PAT or Fine-grained token with `repo` (read-only) scopes to use these features.
> **Network Usage**: While AI inference is local, fetching repository data requires an active internet connection.

## Proposed Changes

### 1. Data Layer: Secure Storage & API

#### [ModelPreferences.kt](file:///C:/Users/sunanda.AMFIIND/AndroidStudioProjects/PocketMind/app/src/main/java/com/frag2win/pocketmind/data/local/ModelPreferences.kt)
- Add `getGitHubToken()` and `setGitHubToken()` to manage the encrypted PAT.

#### [NEW] [GitHubRepository.kt](file:///C:/Users/sunanda.AMFIIND/AndroidStudioProjects/PocketMind/app/src/main/java/com/frag2win/pocketmind/data/repository/GitHubRepository.kt)
- Implement Retrofit/OkHttp service to:
    - `getRepositories()`: Fetch user's repos.
    - `getFileContent(owner, repo, path)`: Fetch specific file text. Use `Accept: application/vnd.github.v3.raw` to get raw text instead of Base64 JSON.
    - `getPullRequestDiff(owner, repo, number)`: Fetch PR diffs. Use `Accept: application/vnd.github.v3.diff` for direct diff text.

---

### 2. Domain Layer: AI Prompt Engineering

#### [PromptBuilder.kt](file:///C:/Users/sunanda.AMFIIND/AndroidStudioProjects/PocketMind/domain/inference/PromptBuilder.kt)
- Add `buildCodeAnalysisPrompt(code)` and `buildPRSummaryPrompt(diff)` to format GitHub data for Gemma's local inference engine.
- **Invisible Prompts**: Ensure the UI shows a clean "Analyze this file" message while the backend ships the full structured context to the engine.

---

### 3. Presentation Layer: UI & Workflow

#### [SettingsScreen.kt](file:///C:/Users/sunanda.AMFIIND/AndroidStudioProjects/PocketMind/app/src/main/java/com/frag2win/pocketmind/ui/settings/SettingsScreen.kt)
- Add a secure input field for the GitHub PAT.

#### [NEW] [GitHubScreen.kt](file:///C:/Users/sunanda.AMFIIND/AndroidStudioProjects/PocketMind/app/src/main/java/com/frag2win/pocketmind/ui/github/GitHubScreen.kt)
- Create a new screen to:
    - List repositories.
    - Browse files and folders.
    - Trigger "Explain this file" or "Summarize PR" actions which route to the `ChatViewModel`.

#### [ChatViewModel.kt](file:///C:/Users/sunanda.AMFIIND/AndroidStudioProjects/PocketMind/app/src/main/java/com/frag2win/pocketmind/ui/chat/ChatViewModel.kt)
- Add specialized message handling for GitHub payloads to trigger the RAG-like analysis.

---

## Verification Plan

### Automated Tests
- `gradlew :app:testDebugUnitTest`: Run existing unit tests to ensure no regressions in chat/FTS logic.
- Add new unit tests for `GitHubRepository` mock responses.

### Manual Verification
1. **Token Storage**: Enter a PAT in Settings, restart the app, and verify it persists.
2. **Repo Browsing**: Open the GitHub screen and verify a list of personal repositories is displayed.
3. **AI PR Summary**: Open a PR, tap "Summarize," and verify the AI generates a coherent summary based on the actual diff content.
4. **Offline Check**: Ensure AI analysis of *previously fetched* code works even if the device is disconnected.
