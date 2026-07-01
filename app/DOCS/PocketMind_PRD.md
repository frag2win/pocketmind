# PocketMind — Product Requirements Document (PRD)
**Version:** 2.0  
**Author:** Shubham (frag2win)  
**Date:** July 2026  
**Hackathon:** Google I/O Connect Bengaluru — July 11, 2026  
**Major Project:** University of Mumbai, B.E. AI & Data Science — Semester VI (2026–27)  
**Changelog:** v2.0 — Added multi-chipset architecture, Play Store deployment strategy · v2.1 — Added KV cache quantization & memory management (Section 8.5), inference performance optimization techniques (Section 8.6)

---

## 1. Executive Summary

PocketMind is a fully on-device, open-source AI assistant for Android powered by **Gemma 4 (E4B)** running locally with universal chipset support — Google Tensor, Qualcomm Snapdragon, and MediaTek Dimensity. It eliminates cloud dependency entirely — no data ever leaves the user's phone. It serves both daily users and professionals by combining conversational AI, document generation (PDF & PPT), and integrations with GitHub and Figma — all offline-first, all private, all free.

**One line pitch:** *"Your private AI workspace — Claude-level intelligence, zero cloud, on your phone."*

**Supported devices:** Any Android 12+ device with 8GB+ RAM — covering 95%+ of the Android ecosystem.

---

## 2. Problem Statement

| Pain Point | Current Reality |
|---|---|
| AI assistants require internet | ChatGPT, Gemini — all cloud-dependent |
| Data privacy is compromised | Every query is sent to third-party servers |
| Professional tools are fragmented | Separate apps for docs, code, design review |
| Cloud AI is expensive | API costs add up; no free tier for heavy use |
| No offline AI for students / rural users | Internet connectivity is unreliable in India |
| On-device AI locked to Pixel phones | Google AICore only supports Tensor/select devices |

**Core insight:** There is no single, open-source, privacy-first AI assistant that works fully offline on Android across all chipsets and handles real professional workflows.

---

## 3. Goals

### Hackathon Goals (July 11, 2026 — 1 Day)
- Run Gemma 4 E4B fully on-device with chipset auto-detection
- Working conversational AI chat UI
- PDF generation from AI output
- GitHub repository browser + PR summarizer
- Live demo on 2 different chipsets (Dimensity + Qualcomm)
- Live demo-ready prototype

### Major Project Goals (Semester VI — 2026–27)
- Full agentic workflow engine (multi-step task planning)
- PPT generation on-device
- Figma API integration (fetch designs, summarize components)
- Fine-tuned Gemma 4 variant for professional use cases
- Multi-modal input (image + voice)
- Google Play Store release (production-grade)
- Open-source release on GitHub under Apache 2.0

---

## 4. Non-Goals
- This is NOT a cloud AI wrapper
- This is NOT a replacement for full desktop productivity suites
- No real-time web search (offline-first; web is optional layer)
- No user accounts or backend servers
- No iOS support (Android-first)

---

## 5. Target Users

### Persona A — The Student
- B.E. / B.Tech student, limited data plan
- Uses AI for study help, note summarization, PDF generation
- Device: Mid-range Android (MediaTek / Qualcomm)
- Cares about: free, offline, fast

### Persona B — The Developer / Professional
- Software engineer, uses GitHub daily
- Wants AI help with PR reviews, code explanation, README generation
- Device: Flagship Android (any chipset)
- Cares about: privacy, GitHub integration, no subscription

### Persona C — The Daily User
- Non-technical user
- Wants a smart personal assistant for drafting messages, summaries, to-do planning
- Device: Any Android 12+
- Cares about: simplicity, privacy, no data leaks

---

## 6. Core Features

### 6.1 On-Device LLM Core
- **Model:** Gemma 4 E4B (4B effective parameters, ~3–4 GB on-device)
- **Runtime:** Adaptive — auto-selected per chipset (see Section 8)
- **Capabilities:** Multi-turn conversation, reasoning, summarization, Q&A
- **Thinking Mode:** Extended chain-of-thought for complex queries
- **Context Window:** Up to 32K tokens on-device
- **Offline:** 100% — no internet required for core inference

### 6.2 Conversational AI Chat
- Clean chat UI (Jetpack Compose)
- Multi-turn memory within session
- Persistent conversation history (Room DB, local only)
- Markdown rendering for AI responses
- Copy / share individual messages
- Voice input (Android SpeechRecognizer, on-device)

### 6.3 Document Generation — PDF
- User prompt → Gemma generates structured content → rendered as PDF
- Use cases: reports, study notes, meeting summaries, resumes
- Customizable templates (professional, academic, minimal)
- Export to device storage + Android share sheet
- Library: iText 7 / Android PdfDocument API

### 6.4 Document Generation — PPT *(Major Project Phase)*
- Prompt → Gemma generates slide outline → rendered as PPTX
- Slide-by-slide content with title, bullets, speaker notes
- Export as .pptx compatible with Google Slides / PowerPoint
- Library: Apache POI (Android port)

### 6.5 GitHub Integration
- Personal Access Token login (stored in EncryptedSharedPreferences)
- Browse repositories, branches, files
- AI-powered features (all processed locally by Gemma):
  - Summarize Pull Requests
  - Explain code files
  - Generate commit messages
  - Draft README.md from codebase
  - Review code for bugs / improvements
- API: GitHub REST API v3 (internet required; AI inference stays local)

### 6.6 Figma Integration *(Major Project Phase)*
- Figma Personal Access Token (stored locally)
- Fetch design files and component trees
- AI-powered features:
  - Summarize design decisions
  - Generate component documentation
  - Extract design tokens (colors, fonts, spacing)
  - Translate design to Jetpack Compose component skeleton
- API: Figma REST API (internet required; AI inference stays local)

### 6.7 Agentic Workflow Engine *(Major Project Phase)*
- Multi-step task planning: user gives a goal → Gemma breaks it into steps → executes sequentially
- Tool use / function calling via Gemma 4's native support
- Example workflow: *"Summarize my last 5 GitHub PRs and create a PDF report"*
  1. Gemma plans: fetch PRs → summarize each → compile → generate PDF
  2. Executes each step with tool calls
  3. Returns final PDF to user
- ReAct loop (Reason → Act → Observe → Repeat)

---

## 7. Tech Stack

### Android App
| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM + Clean Architecture |
| Local DB | Room (conversation history, settings) |
| Secure Storage | EncryptedSharedPreferences (API tokens, AES-256) |
| Networking | Retrofit + OkHttp (GitHub / Figma APIs) |
| Dependency Injection | Hilt |
| Build Format | Android App Bundle (.aab) for Play Store |

### On-Device AI — Multi-Chipset
| Chipset | Runtime | Library |
|---|---|---|
| Google Tensor (Pixel) | Google AICore / ML Kit GenAI Prompt API | `com.google.mlkit:genai-inference` |
| Qualcomm Snapdragon | LiteRT + Qualcomm QNN Delegate | `com.google.ai.edge.litert:litert-qualcomm` |
| MediaTek Dimensity | LiteRT + MediaTek NeuroPilot Delegate | `com.google.ai.edge.litert:litert-mediatek` |
| Any ARM64 (fallback) | LiteRT CPU | `com.google.ai.edge.litert:litert` |

### Document Generation
| Format | Library |
|---|---|
| PDF | iText 7 for Android / Android PdfDocument API |
| PPTX | Apache POI (Android) |

### External APIs (Optional — Online Only)
| Service | API |
|---|---|
| GitHub | REST API v3 |
| Figma | REST API v1 |

### Play Store Delivery
| Component | Strategy |
|---|---|
| App APK | < 15 MB (no model bundled) |
| Gemma 4 E4B Model | Play Asset Delivery (downloaded post-install) |
| Model fallback | Hugging Face CDN direct download |

---

## 8. Multi-Chipset Architecture

### 8.1 Runtime Detection Flow

```
App Launches
     │
     ▼
Detect Chipset (Build.HARDWARE / Build.BOARD)
     │
     ├── Google Tensor (Pixel 6+) ──────► Google AICore / ML Kit GenAI API
     │                                     ~30–40 tokens/sec
     │
     ├── Qualcomm Snapdragon ────────────► LiteRT + QNN Delegate
     │   (MSM / SM board prefix)           ~25–35 tokens/sec
     │
     ├── MediaTek Dimensity ─────────────► LiteRT + NeuroPilot APU Delegate
     │   (MT / Dimensity hardware)         ~15–25 tokens/sec
     │                                     (your Poco X6 Pro)
     │
     └── Unknown / Budget ARM64 ─────────► LiteRT CPU Fallback
                                           ~5–10 tokens/sec
```

### 8.2 Unified Inference Interface

All features interact with a single `PocketMindInference` interface — the chipset layer is completely abstracted from the rest of the app:

```kotlin
interface PocketMindInference {
    suspend fun generate(prompt: String): String
    suspend fun generateStream(prompt: String): Flow<String>
    fun isReady(): Boolean
}
```

Implementations: `AICoreInference`, `QNNInference`, `NeuronInference`, `LiteRTCPUInference` — all resolved at runtime via `InferenceFactory`.

### 8.3 Performance Per Device Tier

| Chipset | Example Devices | Backend | Est. Tokens/sec |
|---|---|---|---|
| Google Tensor G4 | Pixel 9, Pixel 9 Pro | AICore | ~30–40 |
| Snapdragon 8 Gen 3 | OnePlus 12, Galaxy S24 | QNN | ~25–35 |
| Dimensity 8300-Ultra | Poco X6 Pro (dev device) | NeuroPilot | ~15–25 |
| Generic ARM64 | Budget phones | LiteRT CPU | ~5–10 |

### 8.4 Privacy Guarantee — Clarified

PocketMind's privacy model is **inference-private, not internet-free**:

- **AI inference layer:** 100% on-device — prompts never leave the phone, ever
- **Integration layer:** GitHub / Figma calls are user-initiated and fetch-only — data comes *in*, nothing goes *out* to any AI server
- When internet features are active, the UI shows a clear **"Online" indicator**
- Airplane mode = full AI functionality, zero degradation

> *"Your data is processed only on your device. External APIs are user-initiated and fetch-only — no AI inference ever happens in the cloud."*

### 8.5 KV Cache Quantization & Memory Management

**The 8GB Hardware Bottleneck**

Running a local LLM with an extended context window (32K–40K tokens) creates a critical memory bottleneck. On an 8GB "Minimum" tier device (Section 14), the KV cache, Android OS footprint (~3.5 GB), and Gemma 4 E4B model weights (~3–4 GB) together compete for headroom — an uncompressed FP16 KV cache at long context can push the app past the Low Memory Killer (LMK) threshold.

**Optimization Strategy: Low-Bit Cache Compression**

To sustain a high-capacity context window for multi-step agentic workflows and repository-wide GitHub summaries, PocketMind will compress the KV cache during inference rather than storing it at full FP16 precision.

* **Technique:** The KV cache is quantized dynamically from FP16 to a lower precision (INT8 as a baseline; evaluate Google Research's **TurboQuant** — a rotation-based vector quantizer that pushes KV cache precision down to ~3 bits with minimal accuracy loss — as a stretch goal for the Major Project phase). INT8 alone yields ~2x compression; TurboQuant-class methods can realistically approach 4–6x, which matters more at 40K+ context.
* **Architecture note:** Gemma 4's edge variants already use a hybrid local/global attention pattern with unified Keys/Values on global layers, which reduces baseline KV cache growth compared to a standard transformer — this compounds with, rather than replaces, runtime quantization.
* **Memory impact:** Compressing the cache is what makes it possible to hold a long conversation *and* stay under the 8GB ceiling with a stable buffer for background OS operations.
* **Performance impact:** Aims to preserve semantic accuracy and reasoning quality for code comprehension tasks while reducing memory-bus pressure and thermal throttling on mid-range APUs like the MediaTek Dimensity.

| Feature | Native FP16 Cache | INT8 Cache (baseline) | Impact |
|---|---|---|---|
| **Bytes per Token** | Baseline | ~50% of baseline | 2x reduction |
| **Long-Context RAM** | Highest risk of LMK termination | Meaningfully reduced | Safer on 8GB devices |
| **Thermal Profile** | High | Moderate | Sustains t/s over longer sessions |

*Note: exact bytes-per-token and GB-at-context figures depend on Gemma 4 E4B's final layer count, KV head count, and head dimension, which should be benchmarked directly with the AI Edge SDK profiler rather than assumed — treat the table above as directional, not a spec.*

**Context Management Fallback**

If a prompt's required context mathematically exceeds the compressed KV cache budget, the local inference engine falls back to a sliding-window summarization protocol: oldest context blocks are summarized into dense semantic chunks, freeing RAM while preserving the logical thread of the ReAct workflow.

### 8.6 Inference Performance Optimization Techniques

Beyond KV cache compression (8.5), several complementary techniques reduce latency and improve throughput. These compound with whichever backend is active (AICore, QNN, NeuroPilot, or CPU fallback) rather than replacing it.

**1. Weight Quantization**

| Precision | Relative Model Size | Speed vs. FP16 | Quality Loss |
|---|---|---|---|
| FP16 (unquantized) | Baseline | Baseline | None |
| INT8 | ~50% | ~2x faster | Minimal |
| INT4 | ~25% | ~3–4x faster | Slight |

LiteRT supports INT4 natively, and it's the single biggest lever available. *Note: Section 10.1 already lists the Gemma 4 E4B download at ~3.2 GB, which suggests the shipped build is already quantized (likely INT8-equivalent) rather than raw FP16 — worth confirming which precision that 3.2 GB figure assumes, and updating it here once the actual quantized build size is measured, so the two sections don't imply different model sizes.

**2. Speculative Decoding** *(Major Project phase — complex to implement)*

A smaller draft model (Gemma 4 E2B) predicts several tokens ahead; E4B verifies them in a single pass and accepts or rejects. Net effect: E4B-level output quality at closer to E2B-level speed.

```
E2B drafts 4 tokens → E4B verifies in one pass → accept valid tokens, reject wrong ones
```

**3. KV Cache Reuse Across Turns**

Persisting the KV cache between turns in a multi-turn conversation avoids recomputing attention over prior context on every message — this is the same cache discussed in Section 8.5, reused rather than rebuilt.

```kotlin
// Conceptual — persist KV cache across turns.
// Exact API surface depends on the AI Edge SDK version in use;
// validate against current LiteRT-LM / MediaPipe LLM Inference docs
// before implementation.
val cachedSession = inferenceModel.startSession(
    SessionConfig.builder()
        .setKvCacheSize(2048)
        .build()
)
```

**4. Chipset-Specific Thread Tuning**

The Dimensity 8300-Ultra (Poco X6 Pro) has 4 performance cores (Cortex-A715) and 4 efficiency cores (Cortex-A510). Pinning inference threads to the performance cores avoids scheduler context-switching overhead:

```kotlin
// Conceptual — thread/backend tuning.
// If using LiteRT-LM's high-level API this is largely handled for you;
// this raw Interpreter.Options pattern applies if falling back to
// a lower-level TFLite path.
val options = Interpreter.Options()
    .setNumThreads(4)
    .setUseXNNPack(true)
    .addDelegate(NnApiDelegate())
```

**5. Prefill Chunking**

Splitting a long prompt into chunks (e.g. 512 tokens) processed sequentially instead of all at once reduces time-to-first-token, which matters most for the repo-summarization and PR-review flows (Section 6.5) where the input is large.

**6. Response Streaming**

Already reflected in the `PocketMindInference` interface's `generateStream()` method (Section 8.2). Doesn't reduce total inference time but makes the app feel dramatically faster by showing tokens as they generate rather than waiting for the full response.

```kotlin
inferenceModel.generateStream(prompt).collect { token ->
    uiState.appendToken(token)
}
```

**7. Background Model Warm-Up**

Load the model into memory during app startup (`Application.onCreate()`, background coroutine) instead of on first message, eliminating cold-start latency.

**Illustrative Combined Impact — LiteRT CPU Fallback Path**

The table below illustrates roughly how these techniques compound *on the CPU-only fallback path* (Section 8.3's "Generic ARM64" tier, ~5–10 t/s baseline) — this is where the relative gains are largest and easiest to demonstrate. The Poco X6 Pro's primary path uses the NeuroPilot NPU delegate (~15–25 t/s per Section 8.3, which already assumes hardware acceleration), so treat the numbers below as "what CPU-only inference could reach with these optimizations applied," not as a replacement for the NeuroPilot figures elsewhere in this doc — benchmark both paths separately before quoting numbers to judges.

| Optimization | Cumulative (illustrative) |
|---|---|
| Baseline (FP16, CPU only, no opts) | ~8 t/s |
| + INT4 Quantization | ~24 t/s |
| + KV Cache Reuse | ~34 t/s |
| + Thread Affinity + XNNPACK | ~40 t/s |
| + Speculative Decoding | ~60 t/s |

**Priority order:**
1. INT4 quantization — biggest gain, easiest to implement
2. Response streaming — makes UX feel instant, already interface-supported
3. Background warm-up — eliminates cold start
4. KV cache reuse — important for chat quality and speed
5. Thread affinity — moderate gain, low effort
6. Speculative decoding — save for Major Project phase, higher implementation complexity

---

## 9. System Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                     PocketMind Android App                    │
│                                                              │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────────┐  │
│  │   Chat UI    │   │  Doc Studio  │   │  Integrations    │  │
│  │ (Compose)    │   │  PDF / PPT   │   │  GitHub / Figma  │  │
│  └──────┬───────┘   └──────┬───────┘   └────────┬─────────┘  │
│         │                  │                     │            │
│  ┌──────▼──────────────────▼─────────────────────▼─────────┐  │
│  │               AI Agent / Workflow Engine                 │  │
│  │           (Function Calling + ReAct Loop)                │  │
│  └──────────────────────────┬───────────────────────────────┘  │
│                             │                                │
│  ┌──────────────────────────▼───────────────────────────────┐  │
│  │              InferenceFactory (Runtime Detection)         │  │
│  └───┬──────────────┬─────────────────┬──────────────┬──────┘  │
│      │              │                 │              │         │
│  ┌───▼───┐     ┌────▼────┐      ┌────▼────┐   ┌────▼────┐    │
│  │AICore │     │   QNN   │      │Neuron   │   │LiteRT   │    │
│  │Pixel  │     │Snapdrgn │      │MediaTek │   │CPU/Fall │    │
│  └───────┘     └─────────┘      └─────────┘   └─────────┘    │
│                                                              │
│        Gemma 4 E4B Model Weights (~3–4 GB local)             │
│                                                              │
│  ┌──────────────────┐      ┌────────────────────────────┐    │
│  │  Room DB         │      │  EncryptedSharedPrefs      │    │
│  │  (Chat History,  │      │  (GitHub / Figma tokens)   │    │
│  │   Settings)      │      └────────────────────────────┘    │
│  └──────────────────┘                                        │
└──────────────────────────────────────────────────────────────┘
          │  (Optional — user-initiated, online only)
          ▼
   GitHub REST API  ◄──── data fetched IN, never sent OUT to AI
   Figma REST API
```

---

## 10. Play Store Deployment

### 10.1 Strategy
- **APK size:** < 15 MB (model NOT bundled)
- **Model delivery:** Play Asset Delivery — downloads Gemma 4 E4B (~3.2 GB) on first launch over WiFi
- **Fallback:** If Play Asset Delivery unavailable, direct download from Hugging Face CDN
- **Build format:** Android App Bundle (.aab) — Play Store handles ABI/architecture splits automatically

### 10.2 First Launch Flow
```
User installs PocketMind (< 15 MB)
         │
         ▼
First launch → "Welcome to PocketMind"
         │
         ▼
Detect chipset → select correct model variant
         │
         ▼
"Downloading AI Model (3.2 GB) — WiFi recommended"
[████████░░] 64% — Progress bar
         │
         ▼
Model stored in app private storage
         │
         ▼
Full PocketMind experience unlocked ✓
```

### 10.3 Play Store Requirements Checklist
- Target API 35+ ✅ (Android 12 min, Android 16 dev device)
- 64-bit ARM64 APK ✅ (all supported chipsets are ARM64)
- Privacy policy required — "All AI inference on-device, no data collected" ✅
- App category: **Productivity**
- Content rating: Everyone
- No advertising ✅ (open source, free)

### 10.4 Play Store Listing Plan
- **App name:** PocketMind — Local AI Assistant
- **Tagline:** Private AI on your phone. No cloud. No subscriptions.
- **Keywords:** offline AI, on-device LLM, private assistant, Gemma, local AI
- **Screenshots:** Chat demo, PDF generation, GitHub integration, airplane mode proof

---

## 11. User Flows

### Flow 1 — Daily User Morning Routine
1. Open PocketMind → Chat screen
2. "Summarize what I need to do today" → Gemma responds (uses local context / notes)
3. "Draft a message to my team about the delay" → Gemma drafts
4. User taps "Save as PDF" → PDF generated on-device in < 5 seconds
5. User shares PDF via Android share sheet

### Flow 2 — Developer Workflow
1. Open PocketMind → GitHub tab
2. Login with GitHub token (stored locally, encrypted)
3. Select repository → view open PRs
4. Tap PR → "Summarize this PR" → Gemma reads diff on-device, returns summary
5. "Generate PDF report of all open PRs" → Agentic workflow runs → PDF ready

### Flow 3 — Student Study Session
1. Upload lecture PDF (stored locally)
2. "Explain Chapter 3 in simple terms" → Gemma answers from document context
3. "Create study notes as a PDF" → structured notes generated
4. "Make a 5-slide PPT on this topic" → PPTX generated

---

## 12. Hackathon Day Plan (July 11, 2026)

### What to Build in 1 Day
| Time | Milestone |
|---|---|
| 00:00 – 01:00 | Project setup, chipset detection, InferenceFactory wiring |
| 01:00 – 03:00 | Gemma 4 E4B running on Dimensity (LiteRT + NeuroPilot) |
| 03:00 – 05:00 | Chat UI (Jetpack Compose), multi-turn conversation, Room DB |
| 05:00 – 07:00 | PDF generation from AI output (iText) |
| 07:00 – 09:00 | GitHub OAuth + repo browser + PR summarizer |
| 09:00 – 10:00 | Polish UI, test on friend's Qualcomm device, prepare demo |
| 10:00 – End | Demo + pitch |

### Demo Script
1. **Airplane mode ON** — show full AI working completely offline
2. Ask Gemma a complex reasoning question on **Poco X6 Pro** (Dimensity)
3. Same question on **friend's Qualcomm phone** — show same app, different chipset
4. "Create a PDF report of my thoughts on Gemma 4" → PDF generated on-device
5. Connect GitHub → browse repo → AI summarizes a PR (locally)
6. Show conversation history persisted locally in Room DB

**Winning move:** demoing on 2 chipsets live is something almost no hackathon team does. It proves production-readiness.

---

## 13. Major Project Roadmap (Semester VI)

### Phase 1 — Foundation (Month 1–2)
- Stable Gemma 4 E4B across all 3 chipset backends
- Complete chat system with history
- PDF generation (polished templates)
- GitHub integration (full feature set)
- Internal beta testing on 5+ devices

### Phase 2 — Agentic Core (Month 3–4)
- Function calling / tool use engine
- Multi-step workflow planner (ReAct loop)
- Figma API integration
- PPT generation
- Play Store alpha release

### Phase 3 — Advanced (Month 5–6)
- Fine-tune Gemma 4 E4B on professional datasets
- Multi-modal: image input (analyze screenshots, designs)
- Voice-first mode
- Widget for Android home screen
- Play Store public release
- Open-source repo + documentation

---

## 14. Hardware Requirements

| Tier | Example Devices | RAM | Chipset | Experience |
|---|---|---|---|---|
| Minimum | Mid-range Android | 8 GB | Any ARM64 | Works (CPU fallback) |
| Good | Poco X6 Pro (dev device) | 8+4 GB | Dimensity 8300-Ultra | Smooth (NeuroPilot) |
| Better | OnePlus 12, Galaxy S24 | 12 GB | Snapdragon 8 Gen 3 | Fast (QNN) |
| Best | Pixel 9, Pixel 9 Pro | 12–16 GB | Google Tensor G4 | Fastest (AICore) |

**Minimum Android version:** Android 12 (API 31)  
**Model storage required:** ~3.2 GB free internal storage  
**APK size:** < 15 MB

---

## 15. Privacy & Security

- **Zero telemetry** — no analytics, no crash reporting to cloud
- **All AI inference on-device** — Gemma 4 prompts never touch a server, ever
- **Privacy is inference-private** — GitHub/Figma data fetched in, never sent out to AI
- **API tokens encrypted** — EncryptedSharedPreferences (AES-256)
- **GitHub/Figma calls are explicit** — user-initiated only, clearly indicated in UI with "Online" badge
- **Local data only** — Room DB stored in app's private storage
- **Open source** — full code auditable by anyone (Apache 2.0)
- **No accounts** — no sign-up, no profile, no tracking

---

## 16. Success Metrics

### Hackathon
- Working demo in airplane mode ✓
- PDF generation working ✓
- GitHub PR summarizer working ✓
- Demo on 2 different chipsets ✓
- Judges can interact with the app live ✓

### Major Project
- Gemma 4 inference latency < 3 sec (Snapdragon/Tensor), < 6 sec (Dimensity)
- PDF generation < 5 seconds
- GitHub PR summary accuracy rated ≥ 4/5 by test users
- App APK size < 15 MB (excl. model)
- Tested on minimum 5 different Android devices
- 100+ GitHub stars post open-source release
- Play Store listing live by end of Semester VI

---

## 17. Competitive Landscape

| Product | On-Device | Open Source | GitHub Integration | Offline PDF | All Chipsets | Play Store | Free |
|---|---|---|---|---|---|---|---|
| **PocketMind** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| ChatGPT Android | ❌ | ❌ | ❌ | ❌ | N/A | ✅ | Partial |
| Gemini Android | ❌ | ❌ | ❌ | ❌ | N/A | ✅ | Partial |
| Microsoft Copilot | ❌ | ❌ | Partial | ❌ | N/A | ✅ | Partial |
| Generic LLM apps | ✅ | Varies | ❌ | ❌ | ❌ | Varies | Varies |

**PocketMind is the only solution that combines all seven.**

---

## 18. Open Source Plan

- **License:** Apache 2.0
- **Repository:** github.com/frag2win/pocketmind
- **Release:** Post-hackathon (July 2026)
- **Docs:** Full README, architecture docs, chipset compatibility guide, contribution guide
- **Community:** Submit to Hugging Face, Android Weekly, Product Hunt, r/androiddev

---

## 19. References & Resources

- [Gemma 4 on Android — Google AI Edge](https://ai.google.dev/edge/gemma)
- [ML Kit GenAI Prompt API](https://developers.google.com/ml-kit/genai)
- [LiteRT (formerly TFLite)](https://ai.google.dev/edge/litert)
- [LiteRT Qualcomm QNN Delegate](https://ai.google.dev/edge/litert/android/delegates/qualcomm)
- [MediaTek NeuroPilot / LiteRT Delegate](https://ai.google.dev/edge/litert/android/delegates/mediatek)
- [Play Asset Delivery](https://developer.android.com/guide/playcore/asset-delivery)
- [Gemma 4 Model Card — Google DeepMind](https://deepmind.google/models/gemma/gemma-4/)
- [GitHub REST API v3 Docs](https://docs.github.com/en/rest)
- [Figma REST API Docs](https://www.figma.com/developers/api)
- [iText 7 for Android](https://itextpdf.com/products/itext-7)
- [Jetpack Compose Docs](https://developer.android.com/jetpack/compose)

---

*PRD v2.0 — Updated with multi-chipset architecture and Play Store deployment strategy.*  
*Prepared for Google I/O Connect Bengaluru Hackathon and University of Mumbai Major Project submission.*  
*Built by Shubham | github.com/frag2win*
