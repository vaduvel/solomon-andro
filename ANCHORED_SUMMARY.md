## Goal
- Port the iOS Solomon app to Android as "Solomon Andro" at `/Users/vaduvageorge/Desktop/Solomon Andro`, 100% functional with native Android UI (Kotlin + Jetpack Compose, Material3 with Solomon Volt accent).

## Constraints & Preferences
- Kotlin + Jetpack Compose **native** (not KMP, not literal SwiftUI port)
- Port **functionalities** from iOS, adapt design system to Android (Material3 + tokens, not iOS Liquid Glass / glassmorphism)
- Module-by-module, bottom-up: Core → Storage → Analytics → Email/Web/LLM → Moments → UI
- On-device LLM deferred; current :llm uses Ollama HTTP + Template fallback (skip MLX, Skip Supertonic TTS, Skip EnableBanking bank connect)
- Build uses Java 17/21 from `/Applications/Android Studio.app/Contents/jbr/Contents/Home` and Android SDK at `~/Library/Android/sdk` (API 36)
- AGP 8.7.3, Kotlin 2.1.0, KSP 2.1.0-1.0.29, Gradle 8.11.1
- `android.suppressUnsupportedCompileSdk=36` set
- Notifications/BGTaskScheduler → WorkManager; Core Data → Room; Keychain → Keystore
- App ID: `ro.solomon.app`; minSdk 26; targetSdk 36
- Version: `0.1.0-android` (versionCode 1)
- POST_NOTIFICATIONS permission requested at MainActivity launch (Tiramisu+)
- Onboarding gate in MainActivity checks `UserConsent.onboardingComplete`; routes to OnboardingScreen or SolomonApp

## Progress
### Done
- All 7 backend modules (`:core`, `:storage`, `:analytics`, `:email`, `:web`, `:llm`, `:moments`) compile clean
- **:moments** fixed: created `MomentCandidates` (in `core/moments`) + `OrchestratorError` (sealed in `:moments`); replaced snake_case enum names with camelCase identifiers; fixed cross-module smart cast via local val; replaced custom `monthName` with `RomanianDateFormatter.monthName(month)`; rewrote `MomentOrchestrator` with `Any` + `KSerializer<*>` + `@Suppress("UNCHECKED_CAST")`
- **:email** added `BankNotificationParser` (object with regex amount extractor + Romanian keyword direction detection + merchant extraction)
- **Design System**: `SolomonColors.kt` (Volt #C5E84B + dark theme + semantic + category colors), `SolomonTheme.kt` (Material3 darkColorScheme + Typography), `SolomonSpacing.kt` (SolSpacing/SolRadius tokens), `Haptics.kt` (Compose wrapper)
- **App infrastructure**: `SolomonApplication.kt` (3 notification channels via `ensureChannel`), `ServiceLocator` (init/holds all repos + engines + json + `momentEngine` + `momentOrchestrator` + `forecastEngine`)
- **Permissions + Manifest**: INTERNET, POST_NOTIFICATIONS, BIND_NOTIFICATION_LISTENER_SERVICE, RECEIVE_BOOT_COMPLETED, WAKE_LOCK, FOREGROUND_SERVICE, RECORD_AUDIO; intent filters `solomon://` scheme + `SEND text/plain`; registered `SolomonNotificationListener` service with BIND_NOTIFICATION_LISTENER_SERVICE permission
- **DAOs/Repos Flow APIs**: Transaction/UserProfile/Obligation/Goal/Subscription repos all expose `observeAll()` / `observeProfile()` / `observeConsent()`
- **Onboarding 9 steps**: OnboardingViewModel state machine + OnboardingScreen native Compose (Welcome, Identity, Income, Bank, Obligations, Goals, Permissions, Processing, Wow)
- **5 tabs built & wired in `SolomonApp.kt`**:
  - `TodayScreen` + `TodayViewModel` — reactive Flow.combine over txns+profile, SafeToSpendCard, StatPill, MomentCard, ActionTile, TransactionRow
  - `AnalysisScreen` + `AnalysisViewModel` — 30-day income/spent/net, category bars, top merchants (uses `it.direction`)
  - `WalletScreen` + `WalletViewModel` — Obligations/Goals/Subscriptions drilldowns, LinearProgressIndicator for goals
  - `ChatSheet` + `ChatViewModel` — Material3 ModalBottomSheet, message bubbles, ChatInput, calls `LLMProvider.generate(systemPrompt, userContext, maxWords)`
  - `SettingsScreen` + `SettingsViewModel` — Profile sections, consent toggles wired to `ServiceLocator.userRepo.saveConsent(...)`, links to ACTION_NOTIFICATION_LISTENER_SETTINGS and ACTION_APPLICATION_DETAILS_SETTINGS
- **CanIAffordSheet** + **CanIAffordViewModel** — full native Compose with FlowRow of FilterChips, calls cashFlow.analyze
- **Services**:
  - `SolomonNotificationListener` — NotificationListenerService with `BankNotificationParser`, scope+SupervisorJob, ingestion notification with `R.drawable.ic_notification`
  - `SolomonChannels` data class + `ensureChannel(ctx, ch)` helper
  - `SolomonWorkScheduler` — WorkManager periodic: `DailyMomentWorker` (1 day), `HourlyIngestWorker` (6h), `ForecastRefreshWorker` (12h), `ExistingPeriodicWorkPolicy.KEEP`
  - `LastMomentStore` + `LastForecastStore` — DataStore-backed persistence
  - `ForecastEngine` (Kotlin port in `:analytics`) — `analyze(cashFlow, transactions, nowEpoch)` returns `ForecastResult(projectedBalanceIn7Days, projectedBalanceIn30Days, riskLevel, tip)` with `RiskLevel.{low, medium, high}`
  - `DemoDataGenerator.seedIfEmpty()` — seeds 30 days of randomized txns (Kaufland, Lidl, Bolt, eMAG, Netflix, Enel, Cinema City, KFC) + 2 obligations + 1 goal + 2 subscriptions
- **Resources**: `xml/data_extraction_rules.xml` (excludes `solomon.db` + `solomon_prefs.xml`), `mipmap-anydpi-v26/ic_launcher{,_round}.xml`, `drawable/ic_launcher_foreground.xml` (Volt logo), `drawable/ic_notification.xml` (Material bell), `proguard-rules.pro` (kotlinx.serialization keeps)
- **MainActivity**: onboarding gate (DataStore-backed check of `UserConsent.onboardingComplete`), POST_NOTIFICATIONS request, ComponentActivity + setContent
- **APK builds clean**: 19.1 MB at `app/build/outputs/apk/debug/app-debug.apk`

### In Progress
- (none — major UI + services + persistence done)

### Blocked / Deferred (per user)
- On-device LLM model download (Google AI Edge / MediaPipe) — current LLM uses `TemplateLLMProvider` for both primary and fallback in `SmartLLMProvider`
- Supertonic TTS — skipped
- EnableBanking bank connection — skipped
- MLX Swift port — N/A
- iOS Shortcuts integration — replaced with Android intents (`solomon://` scheme + `SEND text/plain`)
- LLMToolExecutor not built (iOS had tool calls for addObligation/addSubscription/etc.) — can be added later
- VoiceInputService (SpeechRecognizer) — not built
- Some sub-screens (Edit views, Audit views, EmailParserSheet, ShortcutSetupView, ModelDownloadView, SpiralAlertView, SubscriptionAuditView, SuspiciousTransactionsView, CategoryLimitsView, ConnectBankView) — not built; main flows covered by Today/Analysis/Wallet/Chat/Settings

## Key Decisions
- Native Android UI with Material3, port functionalities (not literal SwiftUI port — no MeshBackground, no ultraThinMaterial, no MeshBlob)
- `SolomonColors.Primary` = `#C5E84B` (Volt)
- `ServiceLocator` is `object` with `lateinit var` (simple manual DI, no Hilt/Koin)
- `BankNotificationParser` uses regex-based amount extraction with Romanian keyword heuristics for direction (incoming/outgoing) and merchant names (Kaufland, Lidl, Carrefour, Auchan, Mega Image, eMAG, Bolt, Uber)
- `SolomonChannels` is a data class (not object) so it has a constructor with `id, name, importance`; 3 instances in companion object
- `SolomonNotificationListener` uses `CoroutineScope(SupervisorJob() + Dispatchers.IO)` for fire-and-forget persistence
- `MomentEngine.Snapshot` is constructed manually in `DailyMomentWorker` (no factory method on MomentEngine)
- `SmartLLMProvider(primary = TemplateLLMProvider(), fallback = TemplateLLMProvider())` — both templates
- `LastMomentStore` / `LastForecastStore` use the same `Context.preferencesStore` DataStore (named `solomon_prefs`)
- `ServiceLocatorInitializer.onboardingComplete(ctx)` uses `runBlocking` to read `UserConsent.onboardingComplete` once at startup
- `state.monthLabel`/`state.categories`/etc. required `collectAsStateWithLifecycle()` (not a custom helper)
- `Transaction.direction` (not `flow`) is the field name
- `Transaction.merchant: String?` and `Goal.destination: String?` are nullable
- `ObligationKind.utility` (singular) and `ObligationConfidence.declared` are the right enum names
- `UserConsent` has fields `emailAccessGranted`, `notificationsGranted`, `datasetOptIn`, `onboardingComplete` (no smsRead / bankSmsRead)
- `UserProfile.DemographicProfile` has no `city`; `FinancialProfile` has no `monthlyNetIncome`
- `Build.VERSION.SDK_INT < Build.VERSION_CODES.O` skip for `NotificationChannel` creation
- Adaptive icon Volt logo on dark canvas, foreground uses @android:color/white tint
- `data_extraction_rules.xml` uses `<exclude domain="database" path="solomon.db"/>` and `<exclude domain="sharedpref" path="solomon_prefs.xml"/>`
- `proguard-rules.pro` keeps `@Serializable` classes for kotlinx.serialization

## Next Steps
- Add LLMToolExecutor (Kotlin) for in-Chat tool calls: addObligation, addGoal, logTransaction, updateConsent, etc.
- Add VoiceInputService using SpeechRecognizer (when user mentions it)
- Add sub-screens on demand: Edit Obligation/Goal/Subscription/Profile/ManualTransaction
- Add SpiralAlertView, SubscriptionAuditView, SuspiciousTransactionsView, CategoryLimitsView
- Add EmailParserSheet (IMAP OAuth or ContentResolver-based email import)
- Add ModelDownloadView (deferred until on-device LLM approach chosen)
- Add unit tests: Flow transformations, ForecastEngine, MomentEngine.Snapshot
- Add UI tests: Compose tests for Today/Wallet
- Final smoke test: install on Android emulator (API 33+), launch, walk through onboarding 9 steps, verify Today/Wallet/Chat/Settings render, verify bank notification simulator adds a transaction
- Add release signing config (currently uses debug signing)
- Add ProGuard/R8 enable for release build

## Critical Context
- Build command: `export JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home && export ANDROID_HOME=$HOME/Library/Android/sdk && cd "/Users/vaduvageorge/Desktop/Solomon Andro" && ./gradlew :app:assembleDebug --no-daemon`
- `local.properties`: `sdk.dir=/Users/vaduvageorge/Library/Android/sdk`
- APK output: `/Users/vaduvageorge/Desktop/Solomon Andro/app/build/outputs/apk/debug/app-debug.apk` (19.1 MB)
- iOS source reference: `/Users/vaduvageorge/Desktop/solomon/Sources/{SolomonCore,SolomonStorage,SolomonAnalytics,SolomonEmail,SolomonWeb,SolomonLLM,SolomonMoments}/` (175 Swift files, ~18,000 lines of UI)
- iOS app: `SolomonApp/` has 85 Swift files including `Onboarding/Screens/{1-9}*.swift`, `Tabs/{TodayView,AnalysisView,WalletView,ChatView,SettingsView,CanIAffordSheet,GoalEditView,...}.swift`, `Services/{BackgroundTaskService,NotificationIngestionService,ForecastEngine,MissionEngine,LLMToolExecutor,MomentCooldownManager,CategoryLimitsStore,DemoDataGenerator,ModelDownloadView,SolomonTTS,VoiceInputService}.swift`
- iOS OnboardingState: 9 steps with persistent UserDefaults draft; final persistence atomic via `OnboardingPersistence.persistOnboardingFinal`
- `RomanianMoneyFormatter`: `format(amount: Int, style: Style)` → `"thousands RON"` for `Style.short`, `"thousands"` for `bareNumber`
- `RomanianDateFormatter`: `format(epochMillis, style: Style)` with styles `dayOfWeek, dayMonth, full, dayOfWeekDayMonth, iso`
- `Transaction` has `FlowDirection.incoming/outgoing` as `direction` (not `flow`)
- `Transaction.date` is `Long` (epoch seconds), `Transaction.id` is `String`
- `Money` constructor: `Money(amount: Int)`; `Money.fromRON(Double)` rounds to int
- iOS `MomentBuilder` protocol: builders only provide `systemPrompt`; orchestrator handles JSON serialization — Kotlin port uses `MomentBuilder<C>` with `contextSerializer: KSerializer<C>`
- iOS `SolomonLLM` interface: `func generate(systemPrompt, userContext, maxWords, imageData?) async throws -> String` — Kotlin port uses same shape via `LLMProvider`
- iOS `EmailSenderRegistry` has ~80 senders (ported most to `:email` `SenderMapper`)
- iOS `OrchestratorError` is a Swift enum; Kotlin port is `sealed class` with `data object NoCandidatesAvailable` and `data class BuildFailed(MomentType, Throwable)`
- `MomentEngine.Snapshot` is `data class` in `:moments` package — constructed manually in `DailyMomentWorker`
- `SafeToSpendCalculator` already in `:analytics`; not yet wired in TodayViewModel (manual calculation done inline)
- `SolomonContextCoder` uses `internalPublished val json` for inline reified functions — Kotlin port passes `Json` instance to MomentEngine + MomentOrchestrator
- `FlowRow` requires `@OptIn(ExperimentalLayoutApi::class)` annotation
- iOS `MomentOrchestrator` is the entry point for the 8 builders; Kotlin port is `MomentOrchestrator(json)` with `.generate(candidates, llm)`

## Relevant Files
- `/Users/vaduvageorge/Desktop/Solomon Andro/settings.gradle.kts`: includes all 8 modules
- `/Users/vaduvageorge/Desktop/Solomon Andro/build.gradle.kts`: root, uses libs.versions.toml
- `/Users/vaduvageorge/Desktop/Solomon Andro/gradle/libs.versions.toml`: all deps (work 2.10.0, datastore 1.1.1, compose-bom 2024.12.01, room 2.6.1)
- `/Users/vaduvageorge/Desktop/Solomon Andro/gradle.properties`: `android.suppressUnsupportedCompileSdk=36`
- `/Users/vaduvageorge/Desktop/Solomon Andro/local.properties`: `sdk.dir=/Users/vaduvageorge/Library/Android/sdk`
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/build.gradle.kts`: depends on all 7 modules + work/datastore/lifecycle-compose
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/proguard-rules.pro`: kotlinx.serialization keeps
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/AndroidManifest.xml`: MainActivity + intent filters + SolomonNotificationListener service
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/MainActivity.kt`: onboarding gate + POST_NOTIFICATIONS
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/SolomonApplication.kt`: 3 channels via ensureChannel + scheduleBackground
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/ServiceLocatorInitializer.kt`: runBlocking onboarding check
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/di/ServiceLocator.kt`: object holding db, repos, engines, llm, momentEngine, momentOrchestrator, forecastEngine
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/ui/SolomonApp.kt`: 5-tab NavigationBar + Chat sheet trigger
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/ui/theme/{SolomonColors,SolomonTheme,SolomonSpacing}.kt`: design system
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/ui/util/Haptics.kt`: HapticFeedback wrapper
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/ui/onboarding/OnboardingViewModel.kt`: 9-step state
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/ui/onboarding/OnboardingScreen.kt`: 9 native Compose steps
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/ui/screens/TodayScreen.kt`: rewritten with real data
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/ui/screens/TodayViewModel.kt`: reactive with Flow.combine
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/ui/screens/AnalysisScreen.kt`: 30-day analysis with category bars + top merchants
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/ui/screens/WalletScreen.kt`: Obligations/Goals/Subscriptions drilldowns
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/ui/screens/SettingsScreen.kt`: Profile/Consent/System/Legal
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/ui/chat/ChatSheet.kt`: ModalBottomSheet with bubbles + ChatInput
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/ui/chat/ChatViewModel.kt`: LLM-backed messages
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/ui/caniafford/CanIAffordSheet.kt`: full Compose with FlowRow FilterChips
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/ui/caniafford/CanIAffordViewModel.kt`: heuristic verdict from cash flow
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/services/SolomonNotificationListener.kt`: NotificationListenerService
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/services/SolomonChannels.kt`: data class + ensureChannel helper
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/services/SolomonWorkScheduler.kt`: 3 periodic workers
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/services/Workers.kt`: DailyMomentWorker, HourlyIngestWorker, ForecastRefreshWorker
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/services/LastMomentStore.kt`: DataStore last moment persistence
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/services/LastForecastStore.kt`: DataStore last forecast persistence
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/services/DemoDataGenerator.kt`: seeds 30 days txns + obligs + goals + subs
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/res/xml/data_extraction_rules.xml`: excludes DB+prefs from backup
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`: adaptive icon
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/res/drawable/ic_launcher_foreground.xml`: Volt logo
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/res/drawable/ic_notification.xml`: Material bell
- `/Users/vaduvageorge/Desktop/Solomon Andro/email/src/main/java/ro/solomon/email/BankNotificationParser.kt`: SMS/push bank notification parser
- `/Users/vaduvageorge/Desktop/Solomon Andro/analytics/src/main/java/ro/solomon/analytics/ForecastEngine.kt`: 7/30 day balance projection
- `/Users/vaduvageorge/Desktop/Solomon Andro/storage/build.gradle.kts`: `api(libs.androidx.room.runtime)` + `api(project(":core"))`
- `/Users/vaduvageorge/Desktop/Solomon Andro/storage/src/main/java/ro/solomon/storage/dao/*Dao.kt`: all have `observe*(): Flow`
- `/Users/vaduvageorge/Desktop/Solomon Andro/storage/src/main/java/ro/solomon/storage/repository/*Repository.kt`: expose Flow APIs
- `/Users/vaduvageorge/Desktop/Solomon Andro/storage/src/main/java/ro/solomon/storage/repository/OnboardingPersistence.kt`: persistOnboardingFinal(profile, consent, obligations, goals)
- `/Users/vaduvageorge/Desktop/Solomon Andro/storage/src/main/java/ro/solomon/storage/UserConsent.kt`: data class with emailAccessGranted/notificationsGranted/datasetOptIn/onboardingComplete
- `/Users/vaduvageorge/Desktop/Solomon Andro/moments/src/main/java/ro/solomon/moments/MomentEngine.kt`: 8 builders wired, uses `RomanianDateFormatter.monthName(month)`
- `/Users/vaduvageorge/Desktop/Solomon Andro/moments/src/main/java/ro/solomon/moments/MomentOrchestrator.kt`: rewritten with `Any`+`KSerializer<*>` dispatch
- `/Users/vaduvageorge/Desktop/Solomon Andro/moments/src/main/java/ro/solomon/moments/OrchestratorError.kt`: sealed class with data object + data class
- `/Users/vaduvageorge/Desktop/Solomon Andro/core/src/main/java/ro/solomon/core/moments/MomentCandidates.kt`: data class with 8 nullable contexts + hasAnyCandidate
- `/Users/vaduvageorge/Desktop/Solomon Andro/core/src/main/java/ro/solomon/core/domain/Transaction.kt`: `direction: FlowDirection`, `merchant: String?`, `date: Long` epoch seconds
- `/Users/vaduvageorge/Desktop/Solomon Andro/core/src/main/java/ro/solomon/core/domain/TransactionCategory.kt`: enum with `displayNameRO` getter
- `/Users/vaduvageorge/Desktop/Solomon Andro/core/src/main/java/ro/solomon/core/domain/Obligation.kt`: `kind: ObligationKind` (utility, rent_mortgage, etc.), `confidence: ObligationConfidence` (declared, detected, estimated)
- `/Users/vaduvageorge/Desktop/Solomon Andro/core/src/main/java/ro/solomon/core/domain/UserProfile.kt`: DemographicProfile (name, addressing, ageRange), FinancialProfile (salaryRange, salaryFrequency, hasSecondaryIncome, secondaryIncomeAvg, primaryBank)
- `/Users/vaduvageorge/Desktop/Solomon Andro/llm/src/main/java/ro/solomon/llm/SmartLLMProvider.kt`: constructor `(primary, fallback)` both LLMProvider
- `/Users/vaduvageorge/Desktop/solomon/Sources/SolomonMoments/MomentOrchestrator.swift`: iOS reference for MomentCandidates/OrchestratorError
- `/Users/vaduvageorge/Desktop/solomon/SolomonApp/Tabs/{Today,Analysis,Wallet,Chat,Settings}View.swift`: iOS reference (1512, 710, 952, 800, 1447 lines)
- `/Users/vaduvageorge/Desktop/solomon/SolomonApp/Onboarding/Screens/OnboardingScreen{1-9}*.swift`: iOS reference (1:1 port target)
- `/Users/vaduvageorge/Desktop/solomon/SolomonApp/Services/*.swift`: 27 services reference (most relevant: BackgroundTaskService, NotificationIngestionService, ForecastEngine, MissionEngine, MomentCooldownManager, LLMToolExecutor, CategoryLimitsStore, DemoDataGenerator, ModelDownloadView, SolomonTTS, VoiceInputService)
- `/Users/vaduvageorge/Desktop/solomon/SolomonApp/Onboarding/State/OnboardingState.swift`: iOS onboarding state with persistence keys (`solomon.onboarding.completed`)
- `/Users/vaduvageorge/Desktop/solomon/SolomonApp/Design/SolomonColors.swift`: iOS colors (Volt #C5E84B, BG #0B0C0E, Surface #16181C) — design source of truth

## Session 2026-06-02 (continued porting)
- User requested to skip emulator testing (other app under test in Android Studio) and continue porting
- Shipped: MissionEngine, MomentCooldownManager, CategoryLimitsStore, ProfileEditScreen, ObligationEditScreen, SubscriptionEditScreen, GoalEditScreen
- Wired: Mission into TodayScreen (active + pending cards), CategoryLimits into SettingsScreen, edit screens into WalletScreen (tap-to-edit + + Adaugă buttons)
- Build green at 18MB; 6 deprecation warnings on `Icons.Filled.ArrowBack` → `Icons.AutoMirrored.Filled.ArrowBack` (not blocking)
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/services/MissionEngine.kt`: `SolomonMission` data class (id, title, description, category, targetSavingsRON, durationDays, startEpochSeconds, linkedGoalName, isAccepted, isCompleted, completedEpochSeconds) + `endEpochSeconds` + `daysRemaining()` + `progressFraction()` + StateFlow active/pending
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/services/LastMissionStore.kt`: DataStore JSON persistence (`mission_active_json` / `mission_pending_json`)
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/services/MomentCooldownManager.kt`: 8 cooldown types {SpiralAlert(0), CanIAfford(0), UpcomingObligation(12h), Payday(24h), PatternAlert(72h), SubscriptionAudit(7d), WeeklySummary(7d), WowMoment(oneShot)}, DataStore `momentCooldown.<name>` long + `momentCooldown.wowShown` bool
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/services/CategoryLimitsStore.kt`: per-cat monthly RON limits via `intPreferencesKey("categoryLimit.${cat.name}")`; `isNearLimit(0.80)` / `isOverLimit(1.00)` helpers
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/ui/profile/ProfileEditScreen.kt`: full Compose form (name, addressing, ageRange, salaryRange, salaryType, paydayDay, secondaryIncome, bank, personality)
- `/Users/vaduvageorge/Desktop/Solomon Andro/app/src/main/java/ro/solomon/app/ui/wallet/{Obligation,Subscription,Goal}EditScreen.kt`: edit/add/delete with viewModelFactory{initializer{VM(id)}}
- `ServiceLocator.appContext: Context` exposed (private set) for service-layer DataStore access
- `WalletScreen` uses local `var editing* by mutableStateOf<String?>(null)` + `var showNew*` flags; returns early before rendering main content
- `MissionEngine` uses `MutableStateFlow<SolomonMission?>` for active + pending, exposed as `StateFlow`
- `TransactionCategory` has no `rawValue` field; use `cat.name` for DataStore keys
- Build errors fixed: `CategoryLimitsStore.readAll()` made suspend; `ArrowBack` icon import added to SettingsScreen; `rememberCoroutineScope` hoisted out of non-composable lambdas
- APK: `/Users/vaduvageorge/Desktop/Solomon Andro/app/build/outputs/apk/debug/app-debug.apk` (18MB)
