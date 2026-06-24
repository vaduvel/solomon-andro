package ro.solomon.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.services.CategoryLimitsStore
import ro.solomon.app.services.MistralConfig
import ro.solomon.app.ui.components.SolHairlineDivider
import ro.solomon.app.ui.components.SolListCard
import ro.solomon.app.ui.components.SolSectionHeaderRow
import ro.solomon.app.ui.profile.ProfileEditScreen
import ro.solomon.app.ui.theme.SolSpacing
import ro.solomon.app.ui.theme.SolomonColors
import ro.solomon.core.domain.TransactionCategory
import ro.solomon.storage.UserConsent

class SettingsViewModel : ViewModel() {
    data class State(
        val displayName: String = "\u2014",
        val ageRange: String = "\u2014",
        val salaryRange: String = "\u2014",
        val primaryBank: String = "\u2014",
        val emailAccess: Boolean = false,
        val notifications: Boolean = false,
        val datasetOptIn: Boolean = false,
        val onboardingComplete: Boolean = false,
        val mistralActive: Boolean = false,
        val mistralModel: String = "mistral-small-latest"
    )

    val state: StateFlow<State> = combine(
        ServiceLocator.userRepo.observeProfile(),
        ServiceLocator.userRepo.observeConsent(),
        MistralConfig.enabledFlow(),
        MistralConfig.apiKeyFlow(),
        MistralConfig.modelFlow()
    ) { values ->
        val p = values[0] as? ro.solomon.core.domain.UserProfile
        val c = values[1] as? UserConsent
        val mistralEnabled = values[2] as? Boolean ?: false
        val mistralKey = values[3] as? String ?: ""
        val mistralModel = values[4] as? String ?: "mistral-small-latest"
        State(
            displayName = p?.demographics?.name ?: "\u2014",
            ageRange = p?.demographics?.ageRange?.displayNameRO ?: "\u2014",
            salaryRange = p?.financials?.salaryRange?.name ?: "\u2014",
            primaryBank = p?.financials?.primaryBank?.displayNameRO ?: "\u2014",
            emailAccess = c?.emailAccessGranted ?: false,
            notifications = c?.notificationsGranted ?: false,
            datasetOptIn = c?.datasetOptIn ?: false,
            onboardingComplete = c?.onboardingComplete ?: false,
            mistralActive = mistralEnabled && mistralKey.isNotBlank(),
            mistralModel = mistralModel
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State())

    suspend fun setEmail(v: Boolean) = ServiceLocator.userRepo.saveConsent(
        ServiceLocator.userRepo.fetchConsent()?.copy(emailAccessGranted = v)
            ?: UserConsent(emailAccessGranted = v)
    )
    suspend fun setNotifications(v: Boolean) = ServiceLocator.userRepo.saveConsent(
        ServiceLocator.userRepo.fetchConsent()?.copy(notificationsGranted = v)
            ?: UserConsent(notificationsGranted = v)
    )
    suspend fun setDatasetOptIn(v: Boolean) = ServiceLocator.userRepo.saveConsent(
        ServiceLocator.userRepo.fetchConsent()?.copy(datasetOptIn = v)
            ?: UserConsent(datasetOptIn = v)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var showProfile by remember { mutableStateOf(false) }
    var showLimits by remember { mutableStateOf(false) }
    var showMistral by remember { mutableStateOf(false) }
    var showBank by remember { mutableStateOf(false) }
    var showBankSetup by remember { mutableStateOf(false) }
    var showShortcuts by remember { mutableStateOf(false) }
    var showDataSources by remember { mutableStateOf(false) }

    if (showProfile) {
        ProfileEditScreen(onClose = { showProfile = false })
        return
    }
    if (showLimits) {
        CategoryLimitsScreen(onClose = { showLimits = false })
        return
    }
    if (showMistral) {
        MistralSettingsScreen(onClose = { showMistral = false })
        return
    }
    if (showBank) {
        ro.solomon.app.ui.bank.ConnectBankScreen(onBack = { showBank = false })
        return
    }
    if (showBankSetup) {
        ro.solomon.app.ui.bank.EnableBankingSetupScreen(onClose = { showBankSetup = false })
        return
    }
    if (showShortcuts) {
        ro.solomon.app.ui.shortcuts.ShortcutSetupScreen(onBack = { showShortcuts = false })
        return
    }
    if (showDataSources) {
        ro.solomon.app.ui.datasources.DataSourcesScreen(onBack = { showDataSources = false })
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SolSpacing.base),
        verticalArrangement = Arrangement.spacedBy(SolSpacing.base)
    ) {
        Text("Set\u0103ri", style = MaterialTheme.typography.headlineLarge, color = SolomonColors.TextPrimary)

        SolSectionHeaderRow("Profil")
        SolListCard {
            SettingsRow("Editeaz\u0103 profilul", "\u2192") { showProfile = true }
            SolHairlineDivider()
            SettingsRow("Nume", state.displayName) {}
            SolHairlineDivider()
            SettingsRow("V\u00e2rst\u0103", state.ageRange) {}
            SolHairlineDivider()
            SettingsRow("Venit", state.salaryRange) {}
            SolHairlineDivider()
            SettingsRow("Banca principal\u0103", state.primaryBank) {}
        }

        SolSectionHeaderRow("Consim\u021b\u0103minte")
        SolListCard {
            ConsentRow("Acces email-uri tranzac\u021bii", state.emailAccess) { scope.launch { vm.setEmail(it) } }
            SolHairlineDivider()
            ConsentRow("Notific\u0103ri Solomon", state.notifications) { scope.launch { vm.setNotifications(it) } }
            SolHairlineDivider()
            ConsentRow("Date anonime pentru model (opt-in)", state.datasetOptIn) { scope.launch { vm.setDatasetOptIn(it) } }
        }

        SolSectionHeaderRow("Conectivitate")
        SolListCard {
            SettingsRow("Conecteaz\u0103 banca (Open Banking)", "8 b\u0103nci RO") { showBank = true }
            SolHairlineDivider()
            SettingsRow("Configurare Enable Banking", "APP_ID + cheie RSA") { showBankSetup = true }
            SolHairlineDivider()
            SettingsRow("Automatizare pl\u0103\u021bi (Tasker/Macrodroid)", "ghid pas-cu-pas") { showShortcuts = true }
            SolHairlineDivider()
            SettingsRow("Import email-uri tranzac\u021bii", "configureaz\u0103") { showMistral = true }
        }

        SolSectionHeaderRow("Surse de date")
        SolListCard {
            SettingsRow("Status import (Notif/SMS/Share)", "vezi acoperire") { showDataSources = true }
        }

        SolSectionHeaderRow("Sistem Android")
        SolListCard {
            SettingsRow("Acces la notific\u0103ri", "Porne\u0219te listener bancar") {
                ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            SolHairlineDivider()
            SettingsRow("Permisiuni aplica\u021bie", "Gestioneaz\u0103") {
                ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + ctx.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }

        SolSectionHeaderRow("Limite de cheltuieli")
        SolListCard {
            SettingsRow("Limite per categorie", "configureaz\u0103 \u2192") { showLimits = true }
        }

        SolSectionHeaderRow("Model lingvistic")
        SolListCard {
            SettingsRow(
                "Model activ",
                if (state.mistralActive) "Mistral cloud (UE) \u00b7 ${state.mistralModel}" else "Template local (f\u0103r\u0103 cheie Mistral)"
            ) { showMistral = true }
            SolHairlineDivider()
            SettingsRow("Mistral AI (cloud, EU, GDPR)", "configureaz\u0103 \u2192") { showMistral = true }
        }

        SolSectionHeaderRow("Legal")
        SolListCard {
            SettingsRow("Termeni \u0219i condi\u021bii", "cite\u0219te") {}
            SolHairlineDivider()
            SettingsRow("Politica de confiden\u021bialitate", "cite\u0219te") {}
            SolHairlineDivider()
            SettingsRow("Licen\u021be open-source", "vezi") {}
        }

        Spacer(Modifier.height(SolSpacing.sm))
        Text("Solomon v0.1.0-android", color = SolomonColors.TextTertiary, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(SolSpacing.xl))
    }
}

@Composable
private fun SettingsRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(SolSpacing.base),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = SolomonColors.TextPrimary, modifier = Modifier.weight(1f))
        Text(value, color = SolomonColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(SolSpacing.xs))
        Icon(Icons.Filled.ChevronRight, null, tint = SolomonColors.TextTertiary)
    }
}

@Composable
private fun ConsentRow(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SolSpacing.base, vertical = SolSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = SolomonColors.TextPrimary, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked, colors = SwitchDefaults.colors(
            checkedThumbColor = SolomonColors.Primary,
            checkedTrackColor = SolomonColors.Primary.copy(alpha = 0.5f),
            uncheckedThumbColor = SolomonColors.TextTertiary,
            uncheckedTrackColor = SolomonColors.SurfaceVariant
        ))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryLimitsScreen(onClose: () -> Unit) {
    val limitsState = remember { mutableStateOf<Map<TransactionCategory, Int>>(emptyMap()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        limitsState.value = CategoryLimitsStore.limits()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Limite per categorie") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "\u00cenapoi")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SolomonColors.Background)
            )
        },
        containerColor = SolomonColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(SolSpacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(SolSpacing.md)
        ) {
            Text(
                "Seteaz\u0103 c\u00e2t vrei s\u0103 cheltui per categorie. La 80% din limit\u0103, Solomon te aten\u021bioneaz\u0103 \u00een Today. La 100% te opre\u0219te.",
                color = SolomonColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            TransactionCategory.values().forEach { cat ->
                CategoryLimitRow(cat, current = limitsState.value[cat] ?: 0, onSave = { v ->
                    scope.launch {
                        CategoryLimitsStore.setLimit(cat, v)
                        limitsState.value = CategoryLimitsStore.limits()
                    }
                }, onRemove = {
                    scope.launch {
                        CategoryLimitsStore.remove(cat)
                        limitsState.value = CategoryLimitsStore.limits()
                    }
                })
            }
        }
    }
}

@Composable
private fun CategoryLimitRow(
    cat: TransactionCategory,
    current: Int,
    onSave: (Int) -> Unit,
    onRemove: () -> Unit
) {
    var editing by remember(cat) { mutableStateOf(false) }
    var text by remember(cat) { mutableStateOf(if (current > 0) current.toString() else "") }
    Card(
        colors = CardDefaults.cardColors(containerColor = SolomonColors.Surface),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(SolSpacing.md),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(SolSpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(cat.displayNameRO, color = SolomonColors.TextPrimary, modifier = Modifier.weight(1f))
                Text(
                    if (current > 0) "$current RON / lun\u0103" else "nelimitat",
                    color = if (current > 0) SolomonColors.Primary else SolomonColors.TextTertiary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (editing) {
                Spacer(Modifier.height(SolSpacing.xs))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() } },
                    label = { Text("Limit\u0103 RON / lun\u0103") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(SolSpacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.xs)) {
                    Button(onClick = {
                        onSave(text.toIntOrNull() ?: 0)
                        editing = false
                    }) { Text("Salveaz\u0103") }
                    OutlinedButton(onClick = { editing = false }) { Text("Renun\u021b\u0103") }
                }
            } else {
                Spacer(Modifier.height(SolSpacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.xs)) {
                    TextButton(onClick = { editing = true }) {
                        Text(if (current > 0) "Modific\u0103" else "Seteaz\u0103")
                    }
                    if (current > 0) {
                        TextButton(onClick = onRemove) { Text("\u0218terge") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MistralSettingsScreen(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val savedKey by MistralConfig.apiKeyFlow().collectAsStateWithLifecycle(initialValue = "")
    val savedEnabled by MistralConfig.enabledFlow().collectAsStateWithLifecycle(initialValue = false)
    val savedModel by MistralConfig.modelFlow().collectAsStateWithLifecycle(initialValue = "mistral-small-latest")
    var apiKey by remember(savedKey) { mutableStateOf(savedKey) }
    var model by remember(savedModel) { mutableStateOf(savedModel) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mistral AI") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "\u00cenapoi")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SolomonColors.Background)
            )
        },
        containerColor = SolomonColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(SolSpacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(SolSpacing.md)
        ) {
            Text(
                "Mistral proceseaz\u0103 datele \u00een UE (Paris), e GDPR-native \u0219i ISO 27001. C\u00e2nd e activ, chat-ul \u0219i comenzile de tip \u201eadaug\u0103 chirie 1500 RON\u201d folosesc Mistral \u00een loc de template-uri locale. Identificatorii personali (IBAN, card, email, telefon, nume) sunt anonimiza\u021bi \u00eenainte de trimitere.",
                color = SolomonColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Model implicit: mistral-small-latest. Po\u021bi schimba \u00een mistral-large-latest pt r\u0103spunsuri mai bune (cost\u0103 mai mult).",
                color = SolomonColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key Mistral (sk-...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Model") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Activeaz\u0103 Mistral", color = SolomonColors.TextPrimary, modifier = Modifier.weight(1f))
                Switch(
                    checked = savedEnabled,
                    onCheckedChange = { v ->
                        scope.launch { MistralConfig.setEnabled(v) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SolomonColors.Primary,
                        checkedTrackColor = SolomonColors.Primary.copy(alpha = 0.5f)
                    )
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
                Button(
                    onClick = {
                        scope.launch {
                            MistralConfig.setApiKey(apiKey.trim())
                            MistralConfig.setModel(model.trim().ifBlank { "mistral-small-latest" })
                        }
                    }
                ) { Text("Salveaz\u0103") }
                OutlinedButton(
                    onClick = {
                        testing = true
                        testResult = null
                        scope.launch {
                            testResult = try {
                                val test = ro.solomon.llm.MistralLLMProvider(
                                    apiKey = apiKey.trim(),
                                    model = model.trim().ifBlank { "mistral-small-latest" }
                                )
                                val r = test.generate(
                                    systemPrompt = "R\u0103spunde scurt, \u00een rom\u00e2n\u0103.",
                                    userContext = "Spune 'OK'.",
                                    maxWords = 20
                                )
                                "\u2713 $r"
                            } catch (e: Throwable) {
                                "\u2717 ${e.message?.take(80) ?: "eroare"}"
                            }
                            testing = false
                        }
                    },
                    enabled = !testing && apiKey.isNotBlank()
                ) { Text(if (testing) "Testez..." else "Testeaz\u0103") }
            }
            testResult?.let { Text(it, color = if (it.startsWith("\u2713")) SolomonColors.Incoming else SolomonColors.Error) }

            SolHairlineDivider()
            Text("Cost estimat", style = MaterialTheme.typography.titleSmall, color = SolomonColors.TextPrimary)
            Text(
                "Mistral Small: ~0.10 EUR per 1M tokeni input, 0.30 EUR output. O conversa\u021bie medie = 500 tokeni = 0.0001 EUR. 1000 conversa\u021bii = ~0.10 EUR.",
                color = SolomonColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
