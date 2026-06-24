package ro.solomon.app.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ro.solomon.analytics.CashFlowAnalyzer
import ro.solomon.analytics.ForecastEngine
import ro.solomon.analytics.PatternDetector
import ro.solomon.analytics.RecurringDetectionEngine
import ro.solomon.analytics.SpiralDetector
import ro.solomon.analytics.SubscriptionAuditor
import ro.solomon.analytics.SafeToSpendCalculator
import ro.solomon.analytics.SuspiciousTransactionDetector
import ro.solomon.app.services.MistralConfig
import ro.solomon.app.services.MissionEngine
import ro.solomon.llm.LLMProvider
import ro.solomon.llm.MistralLLMProvider
import ro.solomon.llm.TemplateLLMProvider
import ro.solomon.moments.MomentEngine
import ro.solomon.moments.MomentOrchestrator
import ro.solomon.storage.SolomonDatabase
import ro.solomon.storage.repository.GoalRepository
import ro.solomon.storage.repository.ObligationRepository
import ro.solomon.storage.repository.OnboardingPersistence
import ro.solomon.storage.repository.SubscriptionRepository
import ro.solomon.core.enablebanking.BankConnectionService
import ro.solomon.storage.repository.TransactionRepository
import ro.solomon.storage.repository.UserProfileRepository

val Context.preferencesStore: DataStore<Preferences> by preferencesDataStore(name = "solomon_prefs")

object ServiceLocator {
    lateinit var appContext: Context
        private set
    lateinit var db: SolomonDatabase
        private set
    lateinit var userRepo: UserProfileRepository
        private set
    lateinit var txnRepo: TransactionRepository
        private set
    lateinit var obligationRepo: ObligationRepository
        private set
    lateinit var goalRepo: GoalRepository
        private set
    lateinit var subRepo: SubscriptionRepository
        private set
    lateinit var onboardingPersistence: OnboardingPersistence
        private set
    lateinit var cashFlow: CashFlowAnalyzer
        private set
    lateinit var patternDetector: PatternDetector
        private set
    lateinit var spiralDetector: SpiralDetector
        private set
    lateinit var subscriptionAuditor: SubscriptionAuditor
        private set
    lateinit var safeToSpend: SafeToSpendCalculator
        private set
    lateinit var suspicious: SuspiciousTransactionDetector
        private set
    lateinit var recurringDetection: RecurringDetectionEngine
        private set
    lateinit var llm: LLMProvider
        private set
    lateinit var momentEngine: MomentEngine
        private set
    lateinit var momentOrchestrator: MomentOrchestrator
        private set
    lateinit var forecastEngine: ForecastEngine
        private set
    lateinit var missionEngine: MissionEngine
        private set
    lateinit var voiceInput: ro.solomon.app.services.VoiceInputService
        private set
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun init(context: Context) {
        if (::db.isInitialized) return
        val app = context.applicationContext
        appContext = app
        db = SolomonDatabase.getInstance(app)
        userRepo = UserProfileRepository(db.userProfileDao())
        txnRepo = TransactionRepository(db.transactionDao())
        obligationRepo = ObligationRepository(db.obligationDao())
        goalRepo = GoalRepository(db.goalDao())
        subRepo = SubscriptionRepository(db.subscriptionDao())
        onboardingPersistence = OnboardingPersistence(userRepo, obligationRepo, goalRepo)
        cashFlow = CashFlowAnalyzer()
        patternDetector = PatternDetector()
        spiralDetector = SpiralDetector()
        subscriptionAuditor = SubscriptionAuditor()
        safeToSpend = SafeToSpendCalculator()
        suspicious = SuspiciousTransactionDetector()
        recurringDetection = RecurringDetectionEngine()
        forecastEngine = ForecastEngine()
        llm = ResolvingLLMProvider(
            templateFallback = TemplateLLMProvider()
        )
        momentOrchestrator = MomentOrchestrator(json = json)
        momentEngine = MomentEngine(llm = llm, json = json)
        missionEngine = MissionEngine()
        voiceInput = ro.solomon.app.services.VoiceInputService(app)
        ro.solomon.app.services.SolomonTTS.init(app)
        BankConnectionService.apply {
            persistenceDir = app.filesDir
            onTransactionIngested = { tx ->
                CoroutineScope(Dispatchers.IO).launch {
                    txnRepo.save(tx)
                    // Event-driven reactivity: react the instant an open-banking
                    // sync ingests a new transaction, not just on the daily worker.
                    ro.solomon.app.services.ReactiveMomentEvaluator.onTransactionIngested(appContext, tx)
                }
            }
            initialize()
        }
    }
}

class ResolvingLLMProvider(
    private val templateFallback: LLMProvider
) : LLMProvider {
    override val isReady: Boolean = true

    private suspend fun resolve(): LLMProvider {
        val enabled = MistralConfig.enabled()
        if (!enabled) return templateFallback
        val key = MistralConfig.apiKey()
        if (key.isBlank()) return templateFallback
        val model = MistralConfig.model()
        return MistralLLMProvider(apiKey = key, model = model)
    }

    override suspend fun generate(
        systemPrompt: String,
        userContext: String,
        maxWords: Int
    ): String = resolve().generate(systemPrompt, userContext, maxWords)

    override suspend fun generate(
        systemPrompt: String,
        userContext: String,
        maxWords: Int,
        imageData: ByteArray?
    ): String = resolve().generate(systemPrompt, userContext, maxWords, imageData)
}
