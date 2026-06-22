# Solomon Android — Arhitectură

## Filosofie

> **Date locale. Fără server. Fără tracking. Confidențialitate by default.**

Solomon Android este un port nativ al iOS app-ului, scris în **Kotlin + Jetpack Compose** cu **Material3 + tokens proprii (Solomon Design System)**. Spre deosebire de iOS, nu folosim KMP — totul e nativ Android.

Toate datele financiare rămân pe telefon. Zero backend. Zero analytics extern. Singurele apeluri de rețea sunt:
- **Mistral AI API** (opțional, user opt-in, EU-hosted, GDPR-compliant)
- **Open Banking** (în lucru, prin Enable Banking — EU aggregator)

## Module

```
app/           ← UI layer (Compose, ViewModels, Service Locator)
core/          ← Domain models (Transaction, Goal, Obligation, UserProfile)
               ← Domain logic (Money, deterministicUUID, Clock)
               ← Formatters (RomanianMoneyFormatter, RomanianDateFormatter)
               ← Notification parser (BankNotificationParser, MerchantCategoryMatcher, IFNDatabase)
storage/       ← Room database + DAOs + Repositories
analytics/     ← CashFlowAnalyzer, ForecastEngine, PatternDetector,
                  SpiralDetector, SubscriptionAuditor, SuspiciousTransactionDetector,
                  SafeToSpendCalculator, GoalProgress
email/         ← EmailTransactionParser, SenderMapper, SubjectClassifier,
                  SmsPaymentParser (debug/internal only),
                  SenderMapper (~80 senders), SubjectClassifier
web/           ← Web fetchers (Stripe, Wise, eMAG)
llm/           ← LLMProvider interface + MistralLLMProvider, SmartLLMProvider,
                  TemplateLLMProvider, OllamaLLMProvider
moments/       ← MomentEngine, MomentOrchestrator, MomentBuilders
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
                    Room DB (transactions)
                          ↓
                StateFlow → TodayViewModel
                          ↓
                      TodayScreen
```

### Background work (WorkManager)

```
SolomonWorkScheduler
        ├── DailyMomentWorker    (every 24h)  ← MomentEngine.generateBestMoment()
        │                                    → LastMomentStore + MomentHistoryStore
        │                                    → MomentCooldownManager.recordShown()
        ├── HourlyIngestWorker   (every 1h)   ← DemoDataGenerator.seedIfEmpty()
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
| **Open Banking** (în lucru) | **+5%** | EU PSD2 OAuth2 | `EnableBankingClient` (TODO) |

SMS import is debug/internal only. The Play release does not declare `READ_SMS` or `RECEIVE_SMS`, because Google Play SMS policy has a high rejection risk for non-default SMS apps.

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
  - Mistral AI (user opt-in, EU)
  - Open Banking (în lucru, EU)
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

- **v0.1.0 (acum)**: MVP cu Notif + Share + Email + Manual
- **v0.2.0** (următor): Open Banking prin Enable Banking
- **v0.3.0**: PIS — plăți inițiate din Solomon
- **v0.4.0**: On-device LLM (Qwen 2.5 3B sau Ministral 3B)
