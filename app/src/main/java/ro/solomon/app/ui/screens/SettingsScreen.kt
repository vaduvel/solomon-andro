package ro.solomon.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
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
import ro.solomon.app.ui.profile.ProfileEditScreen
import ro.solomon.app.ui.theme.SolSpacing
import ro.solomon.app.ui.theme.SolomonColors
import ro.solomon.core.domain.TransactionCategory
import ro.solomon.storage.UserConsent

class SettingsViewModel : ViewModel() {
    data class State(
        val displayName: String = "—",
        val ageRange: String = "—",
        val salaryRange: String = "—",
        val primaryBank: String = "—",
        val emailAccess: Boolean = false,
        val notifications: Boolean = false,
        val datasetOptIn: Boolean = false,
        val onboardingComplete: Boolean = false
    )

    val state: StateFlow<State> = combine(
        ServiceLocator.userRepo.observeProfile(),
        ServiceLocator.userRepo.observeConsent()
    ) { p, c ->
        State(
            displayName = p?.demographics?.name ?: "—",
            ageRange = p?.demographics?.ageRange?.displayNameRO ?: "—",
            salaryRange = p?.financials?.salaryRange?.name ?: "—",
            primaryBank = p?.financials?.primaryBank?.displayNameRO ?: "—",
            emailAccess = c?.emailAccessGranted ?: false,
            notifications = c?.notificationsGranted ?: false,
            datasetOptIn = c?.datasetOptIn ?: false,
            onboardingComplete = c?.onboardingComplete ?: false
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
            .background(SolomonColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(SolSpacing.base)
    ) {
        Text("Setări", style = MaterialTheme.typography.headlineLarge, color = SolomonColors.TextPrimary)
        Spacer(Modifier.height(SolSpacing.base))

        SectionTitle("Profil")
        SettingsRow("Editează profilul", "→") { showProfile = true }
        SettingsRow("Nume", state.displayName) {}
        SettingsRow("Vârstă", state.ageRange) {}
        SettingsRow("Venit", state.salaryRange) {}
        SettingsRow("Banca principală", state.primaryBank) {}

        Spacer(Modifier.height(SolSpacing.base))
        SectionTitle("Consimțăminte")
        ConsentRow("Acces email-uri tranzacții", state.emailAccess) { scope.launch { vm.setEmail(it) } }
        ConsentRow("Notificări Solomon", state.notifications) { scope.launch { vm.setNotifications(it) } }
        ConsentRow("Date anonime pentru model (opt-in)", state.datasetOptIn) { scope.launch { vm.setDatasetOptIn(it) } }

        Spacer(Modifier.height(SolSpacing.base))
        SectionTitle("Conectivitate")
        SettingsRow("Conectează banca (Open Banking)", "8 bănci RO") { showBank = true }
        SettingsRow("Automatizare plăți (Tasker/Macrodroid)", "ghid pas-cu-pas") { showShortcuts = true }
        SettingsRow("Import email-uri tranzacții", "configurează") { showMistral = true }

        Spacer(Modifier.height(SolSpacing.base))
        SectionTitle("Surse de date")
        SettingsRow("Status import (Notif/SMS/Share)", "vezi acoperire") { showDataSources = true }

        Spacer(Modifier.height(SolSpacing.base))
        SectionTitle("Sistem Android")
        SettingsRow("Acces la notificări", "Pornește listener bancar") {
            ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        SettingsRow("Permisiuni aplicație", "Gestionează") {
            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + ctx.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }

        Spacer(Modifier.height(SolSpacing.base))
        SectionTitle("Limite de cheltuieli")
        SettingsRow("Limite per categorie", "configurează →") { showLimits = true }

        Spacer(Modifier.height(SolSpacing.base))
        SectionTitle("Model lingvistic")
        SettingsRow("Model activ", "Template local (placeholder)") {}
        SettingsRow("Descărcare model on-device", "În curând") {}
        SettingsRow("Mistral AI (cloud, EU, GDPR)", "configurează →") { showMistral = true }

        Spacer(Modifier.height(SolSpacing.base))
        SectionTitle("Legal")
        SettingsRow("Termeni și condiții", "citește") {}
        SettingsRow("Politica de confidențialitate", "citește") {}
        SettingsRow("Licențe open-source", "vezi") {}

        Spacer(Modifier.height(SolSpacing.xl))
        Text("Solomon v0.1.0-android", color = SolomonColors.TextTertiary, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(SolSpacing.xl))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text.uppercase(), color = SolomonColors.Primary, style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(SolSpacing.xs))
}

@Composable
private fun SettingsRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = SolSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = SolomonColors.TextPrimary, modifier = Modifier.weight(1f))
        Text(value, color = SolomonColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(SolSpacing.xs))
        Icon(Icons.Filled.ChevronRight, null, tint = SolomonColors.TextTertiary)
    }
    HorizontalDivider(color = SolomonColors.Hairline)
}

@Composable
private fun ConsentRow(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SolSpacing.xs),
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
    HorizontalDivider(color = SolomonColors.Hairline)
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Înapoi")
                    }
                }
            )
        }
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
                "Setează cât vrei să cheltui per categorie. La 80% din limită, Solomon te atenționează în Today. La 100% te oprește.",
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
                    if (current > 0) "$current RON / lună" else "nelimitat",
                    color = if (current > 0) SolomonColors.Primary else SolomonColors.TextTertiary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (editing) {
                Spacer(Modifier.height(SolSpacing.xs))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() } },
                    label = { Text("Limită RON / lună") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(SolSpacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.xs)) {
                    Button(onClick = {
                        onSave(text.toIntOrNull() ?: 0)
                        editing = false
                    }) { Text("Salvează") }
                    OutlinedButton(onClick = { editing = false }) { Text("Renunță") }
                }
            } else {
                Spacer(Modifier.height(SolSpacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.xs)) {
                    TextButton(onClick = { editing = true }) {
                        Text(if (current > 0) "Modifică" else "Setează")
                    }
                    if (current > 0) {
                        TextButton(onClick = onRemove) { Text("Șterge") }
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Înapoi")
                    }
                }
            )
        }
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
                "Mistral procesează datele în UE (Paris), e GDPR-native și ISO 27001. Când e activ, chat-ul și comenzile de tip „adaugă chirie 1500 RON" folosesc Mistral în loc de template-uri locale.",
                color = SolomonColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Model implicit: mistral-small-latest. Poți schimba în mistral-large-latest pt răspunsuri mai bune (costă mai mult).",
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
                Text("Activează Mistral", color = SolomonColors.TextPrimary, modifier = Modifier.weight(1f))
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
                ) { Text("Salvează") }
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
                                    systemPrompt = "Răspunde scurt, în română.",
                                    userContext = "Spune 'OK'.",
                                    maxWords = 20
                                )
                                "✓ $r"
                            } catch (e: Throwable) {
                                "✗ ${e.message?.take(80) ?: "eroare"}"
                            }
                            testing = false
                        }
                    },
                    enabled = !testing && apiKey.isNotBlank()
                ) { Text(if (testing) "Testez..." else "Testează") }
            }
            testResult?.let { Text(it, color = if (it.startsWith("✓")) SolomonColors.Incoming else SolomonColors.Error) }

            HorizontalDivider(color = SolomonColors.Hairline)
            Text("Cost estimat", style = MaterialTheme.typography.titleSmall, color = SolomonColors.TextPrimary)
            Text(
                "Mistral Small: ~0.10 EUR per 1M tokeni input, 0.30 EUR output. O conversație medie = 500 tokeni = 0.0001 EUR. 1000 conversații = ~0.10 EUR.",
                color = SolomonColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
