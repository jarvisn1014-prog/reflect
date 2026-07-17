# Sprint 3: Reflect — PM Learning Guide
## A Product Manager's Guide to Building an AI-Powered Reflective Journaling App

---

## 1. Product Overview

### What We're Building
Reflect is an AI-first reflective journaling app where journal entries and mood signals are inputs the AI reasons over, and the output the user returns for is synthesized insight they couldn't produce themselves. Free, private, no account required — users bring their own API key.

### Target Audience
Students and professionals who journal and want AI to surface emotional patterns they'd miss on their own. They're privacy-conscious, price-sensitive, and technically comfortable enough to get a free Gemini API key.

### Competitive Analysis

| Competitor | Strengths | Where They're Thin | Reflect's Wedge |
|-----------|-----------|---------------------|-----------------|
| Daylio | 10M+ downloads, quick mood tap, habit tracking | Never synthesizes patterns across entries | Corpus-level AI synthesis — weekly digest reads your week and writes back |
| Day One | Best multimedia journaling, sync, polish | AI is cosmetic — basic prompts, no synthesis | AI-first, not AI-bolted-on. The digest is the home screen, not the entry list |
| Stoic | CBT-grounded, thoughtful prompts | Static CBT framework, no generative insight | AI generates novel reflection prompts contextual to actual entries |
| Reflectly | AI-powered, friendly UI | Shallow AI, locked behind subscription | BYOK = unlimited AI at $0, radical transparency |
| Apple Health mood | Free, integrated, zero friction | 5-state emoji log with no narrative | Opposite trade: more friction at capture, dramatically more value at return |

### The Strategic Position
Every competitor treats AI as a feature bolted onto a journaling product. Reflect treats journaling as the data layer for an AI product. The reflection digest is the home screen, not the entry list.

---

## 2. AI Concepts (5 New, Zero Overlap with Apps 1 & 2)

### Concept 1: Structured Sentiment Classification (NEW)

**What it is:** Using an LLM as a classifier, not a generator. You send text in, you get structured JSON emotion data back.

**How we use it:** Gemini returns `{emotion, valence, arousal, confidence, themes, reflectionPrompt}` — structured data from unstructured text. This is the classification pattern, the second most common enterprise AI use case after RAG.

**Why it's new:** Flashcard Maker used structured JSON for generation. Here we use it for analysis — turning messy text into queryable structured data.

### Concept 2: Hybrid On-Device + Cloud AI (NEW)

**What it is:** Not everything needs an LLM. A lexicon-based sentiment scorer runs on-device, instantly, offline, with zero API cost. The cloud LLM is reserved for deep analysis.

**How we use it:** 800-word lexicon catches ~70% of sentiment for 0% cost. Gemini handles the 30% that needs context — sarcasm, mixed emotions, theme extraction.

**Why it's new:** Doc Chat taught on-device extraction (PDFBox). This teaches on-device inference — a lightweight model running locally, with cloud AI as an enhancement layer.

### Concept 3: Temporal Pattern Aggregation (NEW)

**What it is:** Feeding aggregated structured data (not raw text) to an LLM to find patterns over time. Compressing history into statistics and asking the LLM to reason about trends.

**How we use it:** Weekly insight call sends Gemini a JSON summary: `{entryCount, avgValence, avgEnergy, emotionDistribution, topThemes, moodTrend}`. Gemini returns a human-readable insight grounded in the user's actual week.

**Why it's new:** This is data-to-text generation — the inverse of sentiment classification. Text → structured sentiment → temporal aggregation → narrative insight. The full round-trip.

### Concept 4: Few-Shot Prompting for Tone Calibration (NEW)

**What it is:** Providing example entries + ideal reflection prompts in the prompt so the AI calibrates to the right tone. Zero-shot gives generic therapy-speak. Few-shot gives specific, contextual prompts.

**How we use it:** 2 examples in the system instruction. Gemini matches the warm, curious, non-judgmental tone. The stepping stone to fine-tuning — shaping behavior through examples, not weights.

### Concept 5: AI Auditability via Stored DataInput (NEW)

**What it is:** Every AI analysis stores the exact JSON that was sent to Gemini in a `aiDataInput` field. Any AI output is fully traceable — you can see what data the AI used.

**Why it's new:** In a mental health context, auditability isn't just nice — it's a trust pattern. The user can verify what the AI "saw" when it classified their emotion. This is a privacy/transparency pattern borrowed from Doc Chat's source citations, applied to a new domain.

---

## 3. Decision Logs

### Decision 1: Hybrid Over Journal-First or Mood-First

**Context:** Which direction — journal-first, mood-first, or hybrid?

**Decision:** Hybrid — journal-first surface, mood-as-metadata, AI-as-output.

**Rationale:** Pure journal-first loses to Day One on capture/sync/polish. Pure mood-first loses to Apple Health (free, integrated). Hybrid with AI-synthesis-as-hero is a different category: reflective AI companion. The only architecture where value compounds with use.

### Decision 2: MoodPredictor Cut (Cold-Start + Redundancy)

**Context:** Should we include mood prediction (LLM-as-reasoner over feature vector)?

**Decision:** Cut entirely. Retain as interview talking point.

**Rationale:** Cold-start problem (need 7-14 days of data). Redundant with weekly digest. Shipping a prediction we couldn't validate would violate our confidence design principle. "We designed the feature vector but deferred — shipping a prediction we couldn't stand behind would violate our confidence principle." This turns a cut into a design-integrity story.

### Decision 3: 800-Word Lexicon (Not 200)

**Context:** How many words in the local sentiment lexicon?

**Decision:** 800 words (expanded from initial 200 proposal).

**Rationale:** 200 words matches ~1-2 sentiment-bearing words per entry — barely above random. 800 covers ~80% of sentiment words in casual journaling. Still manageable as a Kotlin mapOf (~30KB).

### Decision 4: Combined "Save & Reflect" Over Separate Buttons

**Context:** Should "Deep Analyze" and "Reflect" be separate buttons?

**Decision:** One "Save & Reflect" action. One Gemini call returns emotion + themes + confidence + reflection prompt.

**Rationale:** Two buttons confuse users (both sound like "make AI think about my entry"). Two calls waste API quota. One action = one call, one user decision, cleaner UX.

### Decision 5: Canvas Chart Over Vico Library

**Context:** Charting library or custom drawing?

**Decision:** Compose Canvas, ~100 lines of code, no dependency.

**Rationale:** Vico adds a dependency + 1 day of learning. Custom Canvas demonstrates drawing ability (stronger portfolio signal than "imported a chart library").

### Decision 6: AI Off Toggle as Trust Feature

**Context:** Should AI be mandatory?

**Decision:** Settings → "AI Reflection" toggle (default ON).

**Rationale:** Some users want a pure journal. If AI is mandatory, the app is a surveillance tool that stores text. The toggle is a trust feature. "AI reflection is off. Your entries are saved locally and never analyzed."

### Decision 7: Streaks Deliberately Rejected

**Context:** Should we show writing streaks?

**Decision:** No streaks. Deliberately rejected.

**Rationale:** Streaks create guilt in a mental health context. A user who misses a day shouldn't feel they've "broken" something. This is a deliberate rejection of Duolingo patterns in a wellness context.

---

## 4. Confidence State Matrix (The Trust Design)

| AI Result | UX Treatment |
|-----------|-------------|
| High confidence (≥0.7), themes found | Show mood + themes as filled pills. Indigo accent. No "AI" label. |
| Medium confidence (0.4–0.7) | Show themes as pills but mood as soft suggestion: "feels like [emotion]?" with tappable "?". |
| Low confidence (<0.4) or no themes | Show nothing. "Some writing is just writing." No error. No shame. |
| AI error / timeout | Entry always saves. "Reflection unavailable right now. Your entry is saved." |
| User disagrees with AI tag | Long-press to correct. Corrected pills use outlined style vs AI's filled style. |

**Design principle:** The AI is a margin note, not a narrator. The user's words are the page.

---

## 5. Portfolio Arc

| App | AI Role | Pattern | Time Scope |
|-----|---------|---------|------------|
| Flashcard Maker | AI as **generator** | Text → structured content (one-shot) | Single input |
| Doc/PDF Chat | AI as **retrieval reasoner** | Document → grounded answers (single-session) | Single session |
| Reflect | AI as **longitudinal synthesizer** | Personal corpus → insight over time (compounding) | Weeks |

Each app is a different verb: generation, retrieval, synthesis. No FAANG interviewer would fail to notice that arc.

---

## 6. Interview Talking Points

### "Tell me about your portfolio."
"I built 3 AI apps in 30 days, each a different AI verb — generation, retrieval, synthesis. The third taught me the hardest AI PM lesson: when AI reasons over personal data, the failure mode isn't inaccuracy — it's false confidence. Here's how I designed for it."

### "How did you handle AI confidence?"
"I designed a confidence state matrix. High confidence shows pills. Medium shows 'feels like [emotion]?' with a tappable question mark. Low confidence shows nothing — 'some writing is just writing.' The user can long-press to correct any AI tag, and corrected pills use outlined style vs AI's filled style. This visual contract is the trust contract."

### "What did you cut and why?"
"Mood prediction. It was the most technically interesting feature and the least aligned with the core loop. Cold-start problem, redundant with the weekly digest. I designed the feature vector schema but deferred — shipping a prediction I couldn't validate would violate my own confidence design principle. That's a design-integrity story, not a scope-creep story."

### "How did you minimize API costs?"
"Hybrid architecture: an 800-word lexicon runs on-device for instant, free sentiment. Gemini is reserved for deep analysis. One combined call per save returns emotion + themes + confidence + reflection prompt. Total: ~5 calls/day for an active user, well within the 1500/day free tier."

### "How does this compare to Daylio?"
"Daylio tracks mood but never synthesizes patterns across entries. Reflect's weekly digest is corpus-level AI synthesis — the AI reads your week and writes back. Daylio optimizes capture. We optimize reflection."

---

## 7. Success Metrics (Hobby Edition)

| Metric | Target | How to measure |
|--------|--------|----------------|
| Entries per week | 3+ | Room query count |
| Weekly digest generation | 1/week | AIInsight count |
| AI correction rate | < 20% | userCorrectedEmotion != null count |
| Mood check-in frequency | 3+/week | MoodLog count |
| AI toggle off rate | < 10% | Settings check |

---

## 8. Risk Register

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| AI mislabels emotion in mental health context | High | High | Confidence state matrix + correction path |
| Weekly digest feels generic | Medium | High | Grounded prompt with actual data, few-shot examples |
| App feels like "just a journal" with few entries | High | Medium | Seeded demo data, 3-entry threshold for digest |
| 800-word lexicon misses sentiment | Medium | Medium | User mood tag is primary, lexicon is pre-filter |
| Privacy concern: journal text sent to Gemini | Medium | High | AI off toggle, radical transparency in Settings |
| Streaks guilt if included | N/A | N/A | Deliberately rejected |