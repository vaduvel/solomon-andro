# Solomon Android — Arhitectură

## Filosofie

> **Date locale. Fără server propriu. Fără tracking. Confidențialitate by default.**

Solomon Android este un port nativ al iOS app-ului, scris în **Kotlin + Jetpack Compose** cu **Material3 + tokens proprii (Solomon Design System)**. Spre deosebire de iOS, nu folosim KMP — totul e nativ Android.

Toate datele financiare rămân pe telefon în Room DB. Nu rulăm un backend propriu și nu folosim analytics extern. Apelurile de rețea sunt:
- **Mistral AI API** (creierul cloud — **singura cale AI activă**, user opt-in, EU-hosted, GDPR-compliant; identificatorii personali sunt anonimizați înainte de trimitere prin `PiiScrubber`)
- **Open Banking** (prin Enable Banking — EU aggregator) — **cod prezent, dar dezactivat în v1 public** (credențialele/keys ASPSP nu sunt încă provisionate)

> ⚠️ **Decizie cloud-only AI:** inferența LLM rulează exclusiv în cloud (Mistral). Providerul local `OllamaLLMProvider` este **deprecated / doar dev** și nu e cablat în release; `TemplateLLMProvider` rămâne doar ca fallback determinist offline (fără LLM real). „Local-first” se referă la stocarea datelor (Room), nu la inferența AI: datele financiare rămân pe telefon, iar AI-ul cloud e opt-in și primește doar date anonimizate.

## Module

```
app/           ← UI layer (Compose, ViewModels, Service Locator) + app services (MissionEngine)
core/          ← Domain models (Transaction, Goal, Obligation, UserProfile)
               ← Domain logic (Money, deterministicUUID, Clock)
               ← Formatters (RomanianMoneyFormatter, RomanianDateFormatter)
               ← Notification parser (BankNotificationParser, MerchantCategoryMatcher, IFNDatabase)
               ← Open Banking (BankConnectionService, EnableBankingClient) — dezactivat în v1
storage/       ← Room database + DAOs + Repositories
analytics/     ← CashFlowAnalyzer, ForecastEngine, PatternDetector,
                  SpiralDetector, SubscriptionAuditor, SubscriptionUsageDetector,
                  SuspiciousTransactionDetector, SafeToSpendCalculator,
                  BudgetEngine, RecurringDetectionEngine, GoalProgress, GoalBudgetContribution
email/         ← EmailTransactionParser, SenderMapper, SubjectClassifier,
                  SmsPaymentParser (debug/internal only)
web/           ← Web fetchers + ScamPatternMatcher
llm/           ← SolomonLLM (interface) + SmartLLMProvider (router) + MistralLLMProvider (cloud, tool-aware),
                  TemplateLLMProvider (fallback determinist offline), OllamaLLMProvider (deprecated / dev-only),
                  PiiScrubber, LLMAgentTool, LLMOutputValidator
moments/       ← MomentEngine, MomentOrchestrator + builders
                  (BudgetAlert, CanIAfford, PatternAlert, PaydayMagic, SpiralAlert,
                   SubscriptionAudit, UpcomingObligation, WeeklySummary, WowMoment)
```

## Data Flow

### Ingest (transactions in)

```
[BT Pay / George / Revolut / eMAG / Share Intent / Email]
                          ↓
     SolomonNotificationListener / Share Intent
                          ↓
               BankNotificationParser
                          ↓
              TransactionRepository.save()
                          ↓
        ┌─────────────────┴───────────────────────────┐
        ↓                                              ↓
  Room DB (transactions)            ReactiveMomentEvaluator.onTransactionIngested()
        ↓                            (event-driven: alerte buget, spiral, can-I-afford)
  StateFlow → TodayViewModel
        ↓
  TodayScreen
```

> Open Banking (`BankConnectionService`) este o sursă suplimentară de ingestie, **inactivă în v1** până la provisionarea keys.

### Background work (WorkManager)

```
SolomonWorkScheduler
        ├── DailyMomentWorker    (every 24h)  ← MomentEngine.generateBestMoment()
        │                                    + BudgetCoach.evaluateDaily()  (alerte buget)
        │                                    → LastMomentStore + MomentHistoryStore
        │                                    → MomentCooldownManager.recordShown()
        ├── HourlyIngestWorker   (every 1h)   ← BankConnectionService.syncAll()  (inactiv în v1)
        └── ForecastRefreshWorker (every 6h)  ← ForecastEngine.analyze()
                                             → LastForecastStore
```

## Surse de date publice (acoperire ~85% fără Open Banking)

| Sursă | Acoperire | Permisiune | Component |
|---|---|---|---|
| Notificări bancare | ~70% | `BIND_NOTIFICATION_LISTENER_SERVICE` | `SolomonNotificationListener` |
| Share Intent | ~5% | none (system-wide) | `MainActivity.handleShareIntent` |
| Email parsing | ~10% | Gmail OAuth (deferred) | `EmailTransactionParser` |
| Manual entry | fallback | none | `ManualTransactionScreen`, `ChatSheet` |
| Open Banking | +5% (**deferred v1**) | EU PSD2 OAuth2 | `EnableBankingClient` / `BankConnectionService` |

SMS import is debug/internal only. The Play release does not declare `READ_SMS` or `RECEIVE_SMS`, because Google Play SMS policy has a high rejection risk for non-default SMS apps.

Open Banking nu este activ în v1: keys/credentials ASPSP (Enable Banking) nu sunt încă provisionate. v1 acoperă ingestia prin Notification Listener + Share + Email + Manual.

## Design System (Solomon DS)

Inspirat din iOS Liquid Glass + Claude Design v3, adaptat la Material3.

```
SolomonColors     ← Volt (#C5E84B), Mint, Amber, Rose, Blue, Violet
SolomonTheme      ← Material3 dark theme wrapper
SolomonSpacing    ← xs(4), sm(8), base(16), md(20), lg(24), xl(32), screenHorizontal
SolRadius         ← sm(8), md(12), lg(18), pill(999)
SolomonComponents ← SolChip, SolHeroCard, SolInsightCard, SolListCard,
                    SolSectionHeaderRow, SolHairlineDivider, SolProgressRing,
                    SolLinearProgress, SolBackButton, IngestionToast, EmptyStateView
```

## Voice input

`VoiceInputService` (Android `SpeechRecognizer`) → text → `ChatViewModel.setDraft()` → user trimite manual sau auto.

Suport: română (`ro-RO`) + preferință offline (`EXTRA_PREFER_OFFLINE`).

## Privacy

- **Local-first**: toate datele în Room DB (encrypted at rest cu Android keystore)
- **Zero tracking**: fără Google Analytics, Firebase, Crashlytics
- **Network only for**:
  - Mistral AI (creier cloud, user opt-in, EU, PII anonimizat)
  - Open Banking (EU) — inactiv în v1
- **User control**: toate permisiunile opționale, revocabile din Setări

## Build

```bash
export JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home
export ANDROID_HOME=$HOME/Library/Android/sdk
cd "/Users/vaduvageorge/Desktop/Solomon Andro"
./gradlew :app:assembleDebug --no-daemon
```

APK: `app/build/outputs/apk/debug/app-debug.apk` (~18 MB)

## Roadmap

- **v0.1.0 (livrat, fără Open Banking activ)**: Notif + Share + Email + Manual + creier cloud Mistral cu anonimizare PII. Open Banking există în cod dar e dezactivat (keys neprovizionate).
- **v0.2.0 (livrat)**: coaching event-driven (`ReactiveMomentEvaluator`) + Bugete pe categorie (`BudgetEngine`/`BudgetCoach` + alerte) + misiuni RO localizate (`MissionEngine`) + playbook scam „ce faci acum”
- **v0.3.0 (next)**: activare Open Banking — provisionare keys/ASPSP Enable Banking + Privacy Policy / Data Safety actualizate pentru fluxul AISP (vezi `PLAY_COMPLIANCE_MVP_2026.md`)
- **v0.4.0**: PIS — plăți inițiate din Solomon
