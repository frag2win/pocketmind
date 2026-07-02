# PocketMind — Product Requirements Document (PRD)
**Version:** 3.0  
**Author:** Shubham (frag2win)  
**Date:** July 2026  
**Hackathon:** Google I/O Connect Bengaluru — July 11, 2026  
**Major Project:** University of Mumbai, B.E. AI & Data Science — Semester VI (2026–27)  
**Changelog:**
- v1.0 — Initial PRD, core features, tech stack, hackathon plan
- v2.0 — Multi-chipset architecture, Play Store deployment strategy
- v2.1 — KV cache quantization (Section 8.5), inference optimization techniques (Section 8.6)
- v3.0 — Dynamic model selection & user control (Section 6.8), model size consistency fix, hackathon plan updated to E2B INT4, editorial notes cleaned up

---

## 1. Executive Summary

PocketMind is a fully on-device, open-source AI assistant for Android powered by **Gemma 4 (E2B / E4B)** running locally with universal chipset support — Google Tensor, Qualcomm Snapdragon, and MediaTek Dimensity. It eliminates cloud dependency entirely — no data ever leaves the user's phone. It serves both daily users and professionals by combining conversational AI, document generation (PDF & PPT), and integrations with GitHub and Figma — all offline-first, all private, all free.

The model variant (E2B INT4 / E2B / E4B) is automatically selected based on the device's available RAM and can be overridden by the user in Settings — ensuring PocketMind runs optimally on every Android device from mid-range to flagship.

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
| On-device AI is one-size-fits-all | No app adapts model size to the user's hardware |

**Core insight:** There is no single, open-source, privacy-first AI assistant that works fully offline on Android across all chipsets, adapts intelligently to device capability, and handles real professional workflows.

---

## 3. Goals

### Hackathon Goals (July 11, 2026 — 1 Day)
- Run Gemma 4 **E2B INT4** on-device with chipset auto-detection (stable, fast, demo-safe)
- Working conversational AI chat UI with response streaming
- PDF generation from AI output
- GitHub repository browser + PR summarizer
- Live demo on 2 different chipsets (Dimensity + Qualcomm)
- Model selection visible in Settings UI
- Live demo-ready prototype

### Major Project Goals (Semester VI — 2026–27)
- Full dynamic model switching (E2B INT4 / E2B / E4B) with RAM-aware auto-selection
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
- Device: Mid-range Android (MediaTek / Qualcomm), 8GB RAM
- Default model: E2B INT4 (fast, low footprint)
- Cares about: free, offline, fast

### Persona B — The Developer / Professional
- Software engineer, uses GitHub daily
- Wants AI help with PR reviews, code explanation, README generation
- Device: Flagship Android (any chipset), 12GB+ RAM
- Default model: E4B (best reasoning quality)
- Cares about: privacy, GitHub integration, no subscription

### Persona C — The Daily User
- Non-technical user
- Wants a smart personal assistant for drafting messages, summaries, to-do planning
- Device: Any Android 12+
- Default model: E2B (balanced)
- Cares about: simplicity, privacy, no data leaks

---

## 6. Core Features

### 6.1 On-Device LLM Core
- **Models:** Gemma 4 E2B INT4 / E2B / E4B — auto-selected per device, user-overridable
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
- Response streaming (tokens rendered as generated — feels instant)
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

### 6.8 Dynamic Model Selection & User Control

#### Auto-Selection Logic
On first launch, PocketMind measures available RAM and automatically selects the best model:

```
Available RAM at runtime
         │
         ├── ≥ 6 GB free ──────────► Gemma 4 E4B
         │                           Best reasoning, code, complex tasks
         │
         ├── ≥ 3.5 GB free ─────────► Gemma 4 E2B
         │                           Balanced quality & speed (default)
         │
         └── < 3.5 GB free ─────────► Gemma 4 E2B INT4
                                     Fastest, lowest RAM, budget devices
```

#### Model Comparison

| Variant | Parameters | Model Size | RAM Needed | Speed (Poco X6 Pro) | Best For |
|---|---|---|---|---|---|
| E2B INT4 | ~2B | ~0.8 GB | ~2.5 GB | ~80–100 t/s | Hackathon, budget devices, speed |
| E2B | ~2B | ~1.5 GB | ~3.5–4 GB | ~50–70 t/s | Daily users, students (default) |
| E4B | ~4B | ~3.2 GB | ~6 GB | ~25–40 t/s | Developers, complex reasoning |

#### User Override — Settings Screen
Users can override auto-selection at any time under **Settings → AI Model**:

```
AI Model Quality
─────────────────────────────────
○  Auto (recommended)
   Selects best model for your device

○  E4B — Highest Quality
   Best for coding, complex reasoning
   Requires 6 GB+ free RAM

●  E2B — Balanced  ← default (Poco X6 Pro)
   Great for chat, summaries, PDF generation
   Requires 3.5 GB+ free RAM

○  E2B INT4 — Fastest
   Best for low RAM devices, quick responses
   Requires 2.5 GB+ free RAM

[Apply & Reload Model]
─────────────────────────────────
Current device: Poco X6 Pro
Available RAM: ~4.2 GB
Recommended: E2B
```

#### Model Download Management
- Models are downloaded separately via Play Asset Delivery
- Users can download multiple variants and switch between them
- Downloaded models stored in app private storage
- Storage usage shown per variant in Settings

```
Downloaded Models
─────────────────────────────────
✅  E2B INT4    0.8 GB   [Delete]
✅  E2B         1.5 GB   [Delete]
⬇️  E4B         3.2 GB   [Download]
─────────────────────────────────
Total used: 2.3 GB of 144 GB
```

#### Hackathon Day Default
For July 11, 2026: **E2B INT4** is the default and demo model — fast load, stable under pressure, impressive output for judges, leaves RAM headroom for PDF generation and GitHub calls running simultaneously.

---

## 7. Tech Stack

### Android App
| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM + Clean Architecture |
| Local DB | Room (conversation history, settings, model preferences) |
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

### Model Variants
| Variant | File Size | Precision | Download via |
|---|---|---|---|
| Gemma 4 E2B INT4 | ~0.8 GB | INT4 quantized | Play Asset Delivery |
| Gemma 4 E2B | ~1.5 GB | INT8 quantized | Play Asset Delivery |
| Gemma 4 E4B | ~3.2 GB | INT8 quantized | Play Asset Delivery |

### Document Generation
| Format | Library |
|---|---|
| PDF | iText 7 for Android / Android PdfDocument API |
| PPTX | Apache POI (Android port) |

### External APIs (Optional — Online Only)
| Service | API |
|---|---|
| GitHub | REST API v3 |
| Figma | REST API v1 |

### Play Store Delivery
| Component | Strategy |
|---|---|
| App APK | < 15 MB (no model bundled) |
| Model variants | Play Asset Delivery (user downloads chosen variant) |
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
     │                                     ~30–40 t/s (E4B) / ~60–80 t/s (E2B)
     │
     ├── Qualcomm Snapdragon ────────────► LiteRT + QNN Delegate
     │   (MSM / SM board prefix)           ~25–35 t/s (E4B) / ~50–70 t/s (E2B)
     │
     ├── MediaTek Dimensity ─────────────► LiteRT + NeuroPilot APU Delegate
     │   (MT / Dimensity hardware)         ~15–25 t/s (E4B) / ~40–60 t/s (E2B)
     │                                     (Poco X6 Pro — dev device)
     │
     └── Unknown / Budget ARM64 ─────────► LiteRT CPU Fallback
                                           ~5–10 t/s (E2B INT4 recommended)
```

### 8.2 Unified Inference Interface

All features interact with a single `PocketMindInference` interface — the chipset and model variant layers are completely abstracted from the rest of the app:

```kotlin
interface PocketMindInference {
    suspend fun generate(prompt: String): String
    suspend fun generateStream(prompt: String): Flow<String>
    fun isReady(): Boolean
    fun currentModel(): GemmaVariant
}

enum class GemmaVariant {
    E4B,        // flagship experience
    E2B,        // balanced — default for Poco X6 Pro
    E2B_INT4    // fastest, smallest footprint — hackathon default
}
```

Implementations: `AICoreInference`, `QNNInference`, `NeuronInference`, `LiteRTCPUInference` — all resolved at runtime via `InferenceFactory`, each accepting a `GemmaVariant` parameter.

### 8.3 Performance Per Device & Model

| Chipset | Device Example | Backend | E4B t/s | E2B t/s | E2B INT4 t/s |
|---|---|---|---|---|---|
| Google Tensor G4 | Pixel 9, Pixel 9 Pro | AICore | ~30–40 | ~60–80 | ~100+ |
| Snapdragon 8 Gen 3 | OnePlus 12, Galaxy S24 | QNN | ~25–35 | ~50–70 | ~90+ |
| Dimensity 8300-Ultra | Poco X6 Pro (dev) | NeuroPilot | ~15–25 | ~40–60 | ~80–100 |
| Generic ARM64 | Budget phones | LiteRT CPU | ❌ too slow | ~10–15 | ~20–30 |

### 8.4 Privacy Guarantee — Clarified

PocketMind's privacy model is **inference-private, not internet-free**:

- **AI inference layer:** 100% on-device — prompts never leave the phone, ever, regardless of model variant
- **Integration layer:** GitHub / Figma calls are user-initiated and fetch-only — data comes *in*, nothing goes *out* to any AI server
- When internet features are active, the UI shows a clear **"Online" indicator**
- Airplane mode = full AI functionality, zero degradation

> *"Your data is processed only on your device. External APIs are user-initiated and fetch-only — no AI inference ever happens in the cloud."*

### 8.5 KV Cache Quantization & Memory Management

**The 8GB Hardware Bottleneck**

Running a local LLM with an extended context window (32K–40K tokens) creates a critical memory bottleneck. On an 8GB minimum-tier device, the KV cache, Android OS footprint (~3.5 GB), and model weights together compete for headroom — an uncompressed FP16 KV cache at long context can push the app past the Low Memory Killer (LMK) threshold.

**Optimization Strategy: Low-Bit Cache Compression**

To sustain high-capacity context for multi-step agentic workflows and repository-wide GitHub summaries, PocketMind compresses the KV cache during inference:

- **Baseline:** INT8 quantization yields ~2× compression over FP16 with minimal accuracy loss
- **Stretch goal (Major Project):** TurboQuant — a rotation-based vector quantizer pushing KV cache to ~3 bits with minimal accuracy degradation (~4–6× compression), critical at 40K+ context
- **Architecture advantage:** Gemma 4's hybrid local/global attention pattern with unified Keys/Values on global layers reduces baseline KV cache growth, compounding with runtime quantization

| Feature | Native FP16 Cache | INT8 Cache (baseline) | Impact |
|---|---|---|---|
| Bytes per Token | Baseline | ~50% of baseline | 2× reduction |
| Long-Context RAM | Highest LMK risk | Meaningfully reduced | Safer on 8GB devices |
| Thermal Profile | High | Moderate | Sustains t/s over long sessions |

*Exact figures depend on Gemma 4 E4B's final layer/KV head count — benchmark with AI Edge SDK profiler before quoting specific numbers.*

**Context Management Fallback**

If a prompt's required context exceeds the compressed KV cache budget, the engine falls back to sliding-window summarization: oldest context blocks are summarized into dense semantic chunks, freeing RAM while preserving the logical thread of the ReAct workflow.

**Extended Context Strategy: Linear RoPE Scaling (Major Project)**
To safely extend the effective context window from 32K toward 40K+ tokens without causing the model to hallucinate or lose track of early conversation history, the Major Project architecture will implement RoPE (Rotary Position Embedding) Scaling (e.g., Linear or YaRN interpolation). 
- **Mechanism:** At inference time, the high-frequency positional embeddings of newly generated tokens are mathematically squished/interpolated. 
- **Impact:** This tricks the model's self-attention mechanism into recognizing token positions beyond its native hard-trained limit, allowing long-form code parsing of entire GitHub diff files without needing a complete architecture retrain.

### 8.6 Inference Performance Optimization Techniques

Beyond KV cache compression, these techniques compound with whichever backend is active:

**1. Weight Quantization**

| Precision | Relative Model Size | Speed vs. FP16 | Quality Loss |
|---|---|---|---|
| FP16 (unquantized) | Baseline | Baseline | None |
| INT8 | ~50% | ~2× faster | Minimal |
| INT4 | ~25% | ~3–4× faster | Slight |

LiteRT supports INT4 natively. The E2B INT4 variant shipped via Play Asset Delivery already uses INT4 precision — no runtime conversion needed.

**2. Speculative Decoding** *(Major Project phase)*

E2B drafts tokens ahead → E4B verifies in one pass → accept valid, reject wrong. Net: E4B-quality output at closer to E2B speed.

```
E2B drafts 4 tokens → E4B verifies in one pass → accept valid tokens, reject wrong ones
```

**3. KV Cache Reuse Across Turns**

Persisting the KV cache between conversation turns avoids recomputing attention over prior context on every message.

```kotlin
// Persist KV cache across turns — validate exact API surface
// against current LiteRT-LM / MediaPipe LLM Inference docs before implementation
val cachedSession = inferenceModel.startSession(
    SessionConfig.builder()
        .setKvCacheSize(2048)
        .build()
)
```

**4. Chipset-Specific Thread Tuning**

Dimensity 8300-Ultra: 4 performance cores (Cortex-A715) + 4 efficiency cores (Cortex-A510). Pin inference to performance cores:

```kotlin
// Applies to LiteRT lower-level path; high-level LiteRT-LM API handles this automatically
val options = Interpreter.Options()
    .setNumThreads(4)
    .setUseXNNPack(true)
    .addDelegate(NnApiDelegate())
```

**5. Prefill Chunking**

Split long prompts into 512-token chunks processed sequentially — reduces time-to-first-token for large inputs (PR diffs, repo summaries).

**6. Response Streaming**

Already reflected in `generateStream()` in the `PocketMindInference` interface. Renders tokens as generated — makes the app feel 3× faster without changing total inference time.

```kotlin
inferenceModel.generateStream(prompt).collect { token ->
    uiState.appendToken(token)
}
```

**7. Background Model Warm-Up**

Load model into memory during `Application.onCreate()` in a background coroutine — eliminates cold-start delay on first message.

**Illustrative Combined Impact — LiteRT CPU Fallback Path (E2B INT4)**

| Optimization | Cumulative (illustrative) |
|---|---|
| Baseline — FP16, CPU only | ~8 t/s |
| + INT4 Quantization (E2B INT4) | ~24 t/s |
| + KV Cache Reuse | ~34 t/s |
| + Thread Affinity + XNNPACK | ~40 t/s |
| + Speculative Decoding (Major Project) | ~60 t/s |

**Priority order for implementation:**
1. E2B INT4 model — biggest gain, zero extra code (just use the right model file)
2. Response streaming — UX feels instant, already interface-supported
3. Background warm-up — eliminates cold start, 10 lines of code
4. KV cache reuse — important for chat quality + speed
5. Thread affinity — moderate gain, low effort
6. Speculative decoding — save for Major Project phase

**8. Offline Retrieval-Augmented Generation (Local RAG) (Major Project)**
Instead of brute-forcing massive codebases or multi-page documentation into the active KV cache, the system will implement an entirely offline, on-device RAG pipeline for handling large repositories or attached documents.
- **Vector DB:** A lightweight, pure-Kotlin or C++ embedded vector database (e.g., ObjectBox or a localized HNSW index) running entirely in the app's private storage.
- **Pipeline:** High-context files (like large GitHub repos or 200-page lecture PDFs) are chunked and vectorized locally using a small, specialized embedding model. When a user queries the codebase, the engine retrieves only the top 3–5 most relevant context snippets and dynamically injects them into the current prompt window, protecting physical RAM while simulating an "infinite" context boundary.

---

## 9. System Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                       PocketMind Android App                      │
│                                                                  │
│  ┌──────────────┐   ┌──────────────┐   ┌────────────────────┐   │
│  │   Chat UI    │   │  Doc Studio  │   │   Integrations     │   │
│  │ (Compose)    │   │  PDF / PPT   │   │   GitHub / Figma   │   │
│  └──────┬───────┘   └──────┬───────┘   └─────────┬──────────┘   │
│         │                  │                      │              │
│  ┌──────▼──────────────────▼──────────────────────▼───────────┐  │
│  │                 AI Agent / Workflow Engine                  │  │
│  │              (Function Calling + ReAct Loop)                │  │
│  └───────────────────────────┬─────────────────────────────────┘  │
│                              │                                   │
│  ┌───────────────────────────▼─────────────────────────────────┐  │
│  │         ModelSelector — RAM-aware auto-selection             │  │
│  │         + User override from Settings                        │  │
│  │         (E2B INT4 / E2B / E4B)                              │  │
│  └───────────────────────────┬─────────────────────────────────┘  │
│                              │                                   │
│  ┌───────────────────────────▼─────────────────────────────────┐  │
│  │             InferenceFactory (Chipset Detection)             │  │
│  └───┬──────────────┬──────────────────┬──────────────┬────────┘  │
│      │              │                  │              │           │
│  ┌───▼───┐     ┌────▼────┐       ┌────▼────┐   ┌────▼────┐      │
│  │AICore │     │   QNN   │       │ Neuron  │   │LiteRT   │      │
│  │Pixel  │     │Snapdrgn │       │MediaTek │   │CPU Fall │      │
│  └───────┘     └─────────┘       └─────────┘   └─────────┘      │
│                                                                  │
│   Model Weights (app private storage)                            │
│   ├── gemma4-e2b-int4.bin   ~0.8 GB  ← hackathon default        │
│   ├── gemma4-e2b.bin        ~1.5 GB  ← daily user default       │
│   └── gemma4-e4b.bin        ~3.2 GB  ← developer / flagship     │
│                                                                  │
│  ┌──────────────────────┐   ┌──────────────────────────────┐    │
│  │  Room DB             │   │  EncryptedSharedPrefs        │    │
│  │  (Chat History,      │   │  (GitHub / Figma tokens,     │    │
│  │   Settings,          │   │   Selected model variant)    │    │
│  │   Model Prefs)       │   └──────────────────────────────┘    │
│  └──────────────────────┘                                       │
└──────────────────────────────────────────────────────────────────┘
           │  (Optional — user-initiated, online only)
           ▼
    GitHub REST API  ◄──── data fetched IN, never sent OUT to AI
    Figma REST API
```

---

## 10. Play Store Deployment

### 10.1 Strategy
- **APK size:** < 15 MB (no model bundled)
- **Model delivery:** Play Asset Delivery — user downloads chosen variant(s) post-install
- **Multiple variants:** E2B INT4 (~0.8 GB), E2B (~1.5 GB), E4B (~3.2 GB) — all available via PAD
- **Fallback:** Hugging Face CDN direct download if Play Asset Delivery is unavailable
- **Build format:** Android App Bundle (.aab) — Play Store handles ABI splits automatically

### 10.2 First Launch Flow
```
User installs PocketMind (< 15 MB APK)
         │
         ▼
First launch → "Welcome to PocketMind"
         │
         ▼
Detect chipset + measure available RAM
         │
         ▼
Recommend model variant:
"Your device works best with E2B (Balanced)"
○ E2B INT4 — Fast (0.8 GB download)
● E2B — Balanced (1.5 GB download) ← recommended
○ E4B — Best quality (3.2 GB download)
[Download & Continue]
         │
         ▼
"Downloading Gemma 4 E2B (1.5 GB) — WiFi recommended"
[████████░░] 64% — Progress bar
         │
         ▼
Model stored in app private storage
         │
         ▼
Full PocketMind experience unlocked ✓
"You can download additional model variants in Settings"
```

### 10.3 Play Store Requirements Checklist
- Target API 35+ ✅ (Android 12 min, Android 16 dev device)
- 64-bit ARM64 APK ✅ (all supported chipsets are ARM64)
- Privacy policy — "All AI inference on-device, no data collected" ✅
- App category: **Productivity**
- Content rating: Everyone
- No advertising ✅ (open source, free)

### 10.4 Play Store Listing Plan
- **App name:** PocketMind — Local AI Assistant
- **Tagline:** Private AI on your phone. No cloud. No subscriptions.
- **Keywords:** offline AI, on-device LLM, private assistant, Gemma, local AI, Gemma 4
- **Screenshots:** Chat demo (airplane mode), PDF generation, GitHub integration, model selection screen

---

## 11. User Flows

### Flow 1 — Daily User Morning Routine
1. Open PocketMind → Chat screen (E2B loaded, warm)
2. "Summarize what I need to do today" → Gemma responds in < 3 seconds
3. "Draft a message to my team about the delay" → Gemma drafts
4. User taps "Save as PDF" → PDF generated on-device in < 5 seconds
5. User shares PDF via Android share sheet

### Flow 2 — Developer Workflow
1. Open PocketMind → GitHub tab
2. Login with GitHub token (stored locally, encrypted)
3. Select repository → view open PRs
4. Tap PR → "Summarize this PR" → Gemma (E4B) reads diff on-device, returns summary
5. "Generate PDF report of all open PRs" → Agentic workflow runs → PDF ready

### Flow 3 — Student Study Session
1. Upload lecture PDF (stored locally)
2. "Explain Chapter 3 in simple terms" → Gemma (E2B) answers from document context
3. "Create study notes as a PDF" → structured notes generated
4. "Make a 5-slide PPT on this topic" → PPTX generated *(Major Project)*

### Flow 4 — Model Switch by User
1. User notices responses feel slow on their 8GB phone
2. Opens Settings → AI Model
3. Switches from E2B to E2B INT4
4. Taps "Apply & Reload Model" → model swaps in background
5. Next message is visibly faster — user satisfied

---

## 12. Hackathon Day Plan (July 11, 2026)

### What to Build in 1 Day
| Time | Milestone |
|---|---|
| 00:00 – 01:00 | Project setup, chipset detection, InferenceFactory wiring |
| 01:00 – 03:00 | **Gemma 4 E2B INT4** running on Dimensity via LiteRT + NeuroPilot |
| 03:00 – 05:00 | Chat UI (Jetpack Compose), multi-turn conversation, Room DB, streaming |
| 05:00 – 06:00 | Model selection UI in Settings (E2B INT4 / E2B / E4B toggle) |
| 06:00 – 07:30 | PDF generation from AI output (iText) |
| 07:30 – 09:00 | GitHub OAuth + repo browser + PR summarizer |
| 09:00 – 10:00 | Polish UI, test on friend's Qualcomm device, prepare demo |
| 10:00 – End | Demo + pitch |

### Why E2B INT4 for Hackathon Day
- Loads in ~30 seconds vs ~90 seconds for E4B on Poco X6 Pro
- Uses ~2.5 GB RAM vs ~6 GB — leaves headroom for PDF gen + GitHub calls
- ~80–100 t/s — feels impressively fast live in front of judges
- Zero risk of Low Memory Killer killing the app mid-demo
- Output quality is still excellent for chat, PDF, and PR summarization

### Demo Script
1. **Airplane mode ON** — show full AI working completely offline
2. Ask Gemma a complex reasoning question on **Poco X6 Pro** (Dimensity + E2B INT4)
3. Open Settings → show model selection — switch to E2B live, show it reloads
4. Same question on **friend's Qualcomm phone** — same app, different chipset
5. "Create a PDF report of my thoughts on Gemma 4" → PDF generated on-device
6. Connect GitHub → browse repo → AI summarizes a PR (locally)
7. Show conversation history persisted in Room DB

**Winning move:** model selection UI + 2-chipset demo + airplane mode = production-ready product, not a prototype.

---

## 13. Major Project Roadmap (Semester VI)

### Phase 1 — Foundation (Month 1–2)
- Stable inference across all 3 chipset backends for all 3 model variants
- Complete chat system with history and streaming
- PDF generation with polished templates
- GitHub integration (full feature set)
- Model selection UI + RAM-aware auto-detection
- Internal beta testing on 5+ devices across chipsets

### Phase 2 — Agentic Core (Month 3–4)
- Function calling / tool use engine
- Multi-step workflow planner (ReAct loop)
- Figma API integration
- PPT generation
- Play Store alpha release

### Phase 3 — Advanced (Month 5–6)
- Speculative decoding (E2B draft + E4B verify)
- TurboQuant KV cache compression (~3-bit)
- **Local RAG implementation** using embedded on-device vector indexing for codebase exploration
- **Linear RoPE scaling configurations** to safely stretch the native context boundary to 40K+
- Fine-tune Gemma 4 E2B on professional datasets
- Multi-modal: image input (analyze screenshots, designs)
- Voice-first mode
- Widget for Android home screen
- Play Store public release
- Open-source repo + full documentation

---

## 14. Hardware Requirements

| Tier | Example Devices | RAM | Chipset | Recommended Model | Experience |
|---|---|---|---|---|---|
| Minimum | Mid-range Android | 8 GB | Any ARM64 | E2B INT4 | Works well |
| Good | **Poco X6 Pro (dev)** | 8+4 GB | Dimensity 8300-Ultra | E2B | Smooth |
| Better | OnePlus 12, Galaxy S24 | 12 GB | Snapdragon 8 Gen 3 | E4B | Fast |
| Best | Pixel 9, Pixel 9 Pro | 12–16 GB | Google Tensor G4 | E4B | Fastest |

**Minimum Android version:** Android 12 (API 31)  
**Storage for all 3 models:** ~5.5 GB total (users download only what they need)  
**APK size:** < 15 MB

---

## 15. Privacy & Security

- **Zero telemetry** — no analytics, no crash reporting to cloud
- **All AI inference on-device** — Gemma prompts never touch a server, regardless of model variant
- **Privacy is inference-private** — GitHub/Figma data fetched in, never sent out to any AI service
- **API tokens encrypted** — EncryptedSharedPreferences (AES-256)
- **GitHub/Figma calls are explicit** — user-initiated only, "Online" badge shown in UI
- **Local data only** — Room DB stored in app's private storage
- **Model stored locally** — model weights in app private storage, not accessible to other apps
- **Open source** — full code auditable by anyone (Apache 2.0)
- **No accounts** — no sign-up, no profile, no tracking

---

## 16. Success Metrics

### Hackathon
- Working demo in airplane mode ✓
- E2B INT4 running stable on Poco X6 Pro ✓
- Model selection UI working ✓
- PDF generation working ✓
- GitHub PR summarizer working ✓
- Demo on 2 different chipsets ✓
- Judges can interact with the app live ✓

### Major Project
- All 3 model variants downloadable and switchable in-app
- E4B latency < 3 sec first token (Snapdragon/Tensor), < 6 sec (Dimensity)
- E2B INT4 latency < 1 sec first token on all supported chipsets
- PDF generation < 5 seconds
- GitHub PR summary accuracy rated ≥ 4/5 by test users
- App APK size < 15 MB (excl. model)
- Tested on minimum 5 different Android devices across all 3 chipset families
- 100+ GitHub stars post open-source release
- Play Store listing live by end of Semester VI

---

## 17. Competitive Landscape

| Product | On-Device | Open Source | GitHub Integration | Offline PDF | All Chipsets | Model Choice | Play Store | Free |
|---|---|---|---|---|---|---|---|---|
| **PocketMind** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| ChatGPT Android | ❌ | ❌ | ❌ | ❌ | N/A | ❌ | ✅ | Partial |
| Gemini Android | ❌ | ❌ | ❌ | ❌ | N/A | ❌ | ✅ | Partial |
| Microsoft Copilot | ❌ | ❌ | Partial | ❌ | N/A | ❌ | ✅ | Partial |
| Generic LLM apps | ✅ | Varies | ❌ | ❌ | ❌ | Rarely | Varies | Varies |

**PocketMind is the only solution that combines all eight.**

---

## 18. Open Source Plan

- **License:** Apache 2.0
- **Repository:** github.com/frag2win/pocketmind
- **Release:** Post-hackathon (July 2026)
- **Docs:** Full README, architecture docs, chipset compatibility guide, model selection guide, contribution guide
- **Community:** Submit to Hugging Face, Android Weekly, Product Hunt, r/androiddev, r/LocalLLaMA

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
- [TurboQuant — Google Research KV Cache Compression](https://research.google)

---

*PRD v3.0 — Dynamic model selection, multi-chipset architecture, Play Store deployment.*  
*Prepared for Google I/O Connect Bengaluru Hackathon and University of Mumbai Major Project submission.*  
*Built by Shubham | github.com/frag2win*
