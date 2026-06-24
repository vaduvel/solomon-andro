# Solomon - Hybrid Coach Production Strategy

> Status: foundation in progress (branch `feat/hybrid-coach-foundation`).
> Scope: turn Solomon's coach from a static "brochure + generic LLM" into a real, production-ready
> financial coach by **fusing** what Solomon already does well with how real financial coaches work.

## 1. Honest assessment of the coach today

The current "coach" is three disconnected things, none of which behave like a real coach:

| Source of "knowledge" | What it really is | Hard limit |
|---|---|---|
| `SolomonAdvisor.wisdom()` | ~10 hardcoded text blocks (saving, debt, couple, investing, mindset, big purchase, career, risk, RO culture) | Frozen, uncited, not credentialed, not tied to the user's real numbers. A brochure. |
| `SolomonCoach` (5 vulnerabilities) + `SolomonCoachMemory` | enum with lesson/action/tip + memory storing **one** string | Reduces a person to one label. This is the "beep beep stop spending" problem. |
| Mistral (cloud LLM) | general training knowledge | Generic, not RO-specific, can hallucinate (finance hallucination rates are high), stale. |

**Experience today: zero.** No feedback loop - the coach never learns whether advice was acted on.
Memory is a single string. Knowledge comes from whoever wrote the code + a generic LLM, with no
dated RO source (BNR, ASF, ANPC, Tezaur/Fidelis).

**What is already good:** the deterministic money engine (BudgetEngine, detectors, safe-to-spend,
analytics). That is the hard part and it stays.

## 2. What real financial coaches actually do (research)

The "wow" is NOT more facts - it is the **method**.

- **Coach != advisor.** A coach gives education, tools, structure, and accountability so the client
  decides - it does not prescribe products. This is also the safe legal position (guidance, not advice).
- **There is a process, not one-liners.** CFP 7-step (CGADPIM): understand circumstances -> gather data
  -> set & prioritize goals -> analyze -> recommend -> implement -> **monitor & update**. Step 7
  (cadence of accountability) is exactly what Solomon lacks.
- **The separating skill is behavior change, not knowledge:**
  - Motivational Interviewing (OARS): open questions, affirmations, reflective listening, summaries;
    scaling questions; evoke change talk; resolve ambivalence. The client convinces themselves.
  - Behavioral coaching: the biggest detriment to outcomes is the person's own behavior.
  - Money scripts / financial therapy (Klontz): core money beliefs drive behavior; surface & reframe.
  - Implementation intentions ("if-then" plans): 2-3x more follow-through than a vague intention.
  - Tiny Habits / identity-based habits (Fogg, Clear): cue + tiny action + reinforcement.
  - Cadence of accountability: short, regular check-ins (~20 min): celebrate wins, analyze misses, adjust.

## 3. The hybrid: KEEP / KILL / FUSE

The two halves complete each other: Solomon has the **real numbers** but no coaching brain; real
coaching has the **method** but no access to the user's numbers.

| From Solomon (today) | Decision | Why |
|---|---|---|
| BudgetEngine, detectors, safe-to-spend, analytics | KEEP - the foundation | The hard, real part. Do not throw away. |
| `TrueCostComparator` | KEEP, move into coach | Already a good coaching tool (makes a number tangible). |
| `coupleQuestions` | KEEP, but actively guided | Good skeleton for the couple feature. |
| Static text in `SolomonAdvisor.kt` | MOVE to a dated, cited RO knowledge base | Content is ok but frozen in code and unsourced. |
| 1-string memory + 5 vulnerabilities | KILL/UPGRADE | Too poor. Becomes a real profile (money script, goals, commitments, outcomes). |
| Mistral "bare" | RE-HARNESS to grounding + guardrail | Stops inventing; only links your numbers + RO source. |

## 4. Hybrid architecture (3 layers)

1. **TRUTH layer (today's Solomon)** -> deterministic numbers. Never from the LLM.
2. **COACH layer (this strategy)** -> CGADPIM process + MI/"why" + implementation intentions +
   cadence + memory with feedback loop.
3. **VOICE layer** -> the "was it worth it for you?" tone (anti "beep beep"), adapted to money script.

Flow: engine says *"you went 200 RON over on food delivery"* (L1) -> coach reframes with a scaling
question + an if-then plan (L2) -> phrased for the user's money script (L3).

## 5. Moat features

1. Process-based coaching (CGADPIM applied) - a 4-week program with a short weekly check-in.
2. The "why" engine (RO money scripts) - tone & approach change per dominant script.
3. "Can I afford this?" at transaction time - instant answer grounded in real safe-to-spend + goal impact.
4. Implementation-intention generator ("if-then") - 2-3x action vs "stop spending".
5. Coach with memory + experience (feedback loop) - learns what works for this person.
6. Guided monthly couple money meeting - on their real numbers.

## 6. Making the coach REAL (production-ready)

- **Grounding/RAG over a curated, dated RO knowledge base** (BNR/ROBOR, ASF, ANPC, Tezaur/Fidelis,
  bank grids) instead of hardcoded text; the LLM **cites** the source and never invents.
- **Two clearly separated sources:** (1) your facts = the deterministic engine (never from the LLM);
  (2) education = the dated RO base. The LLM only links and phrases.
- **A "138-checks"-style guardrail** on every response: no invented numbers, no concrete product/
  investment advice, "guidance not advice" disclaimer (coach positioning -> no licensing/fiduciary).
- **Structured memory** (money script, vulnerabilities, goals, commitments, outcomes).
- **RO transaction enrichment** via an EU-native provider (Salt Edge / Bud / Ntropy), not Plaid.

## 7. This branch delivers (Layer 2+3 foundation)

New, additive, dependency-light files (no Money arithmetic touched):

- `MoneyScript.kt` - Klontz 4 money scripts, RO-localized, tone-per-script (the "why" engine).
- `CoachProfile.kt` - structured coaching memory + feedback loop (replaces the 1-string memory).
- `ImplementationIntentionEngine.kt` - if-then plan generator, adapted per script.
- `CoachingVoice.kt` - MI-style voice helpers (scaling, reflect, was-it-worth-it), per script.

### Next steps (require a local build)

1. Wire `CoachProfileStore` into onboarding (capture money script) and into `BudgetCoach`/`TodayViewModel`
   so nudges go through `CoachingVoice.frameForScript(...)` instead of raw alerts.
2. Record `recordActedOn` / `recordIgnored` on mission accept/dismiss and alert interactions (feedback loop).
3. Migrate `SolomonAdvisor` text into a dated, cited RO knowledge store + add the grounding guardrail.
4. Replace any remaining raw "limit exceeded" alerts with the if-then + scaling-question flow.
