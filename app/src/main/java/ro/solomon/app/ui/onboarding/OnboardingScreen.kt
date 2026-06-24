package ro.solomon.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ro.solomon.app.services.CoachProfileStore
import ro.solomon.app.services.MoneyScript
import ro.solomon.app.ui.theme.SolSpacing
import ro.solomon.app.ui.theme.SolomonColors
import ro.solomon.app.ui.util.rememberHaptics
import ro.solomon.core.domain.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    vm: OnboardingViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.finished) { if (state.finished) onFinished() }

    Scaffold(
        containerColor = SolomonColors.Background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            OnboardingBottomBar(
                state = state,
                onBack = { haptics.light(); vm.back() },
                onNext = {
                    haptics.medium()
                    val s = state
                    if (s.currentStep == 6 && s.processingTasks.all { it.state == OnboardingViewModel.TaskState.Done }) {
                        vm.next()
                    } else if (s.currentStep == 7) {
                        vm.next()
                        vm.generateWowMoment()
                    } else if (s.isLastStep) {
                        vm.finish()
                    } else {
                        vm.next()
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            ProgressHeader(current = state.currentStep, total = 9)
            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding-step"
            ) { step ->
                when (step) {
                    0 -> WelcomeStep(onStart = vm::next)
                    1 -> IdentityStep(state, vm)
                    2 -> IncomeStep(state, vm)
                    3 -> BankStep(state, vm)
                    4 -> ObligationsStep(state, vm)
                    5 -> GoalsStep(state, vm)
                    6 -> PermissionsStep(state, vm)
                    7 -> ProcessingStep(state, vm)
                    8 -> WowStep(state)
                }
            }
        }
    }

    LaunchedEffect(state.currentStep) {
        if (state.currentStep == 7) vm.runProcessing()
    }
}

@Composable
private fun ProgressHeader(current: Int, total: Int) {
    Column(Modifier.padding(horizontal = SolSpacing.base, vertical = SolSpacing.md)) {
        LinearProgressIndicator(
            progress = { (current + 1).toFloat() / total },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = SolomonColors.Primary,
            trackColor = SolomonColors.SurfaceVariant,
        )
        Spacer(Modifier.height(SolSpacing.xs))
        Text(
            "Pas ${current + 1} din $total",
            style = MaterialTheme.typography.labelSmall,
            color = SolomonColors.TextTertiary
        )
    }
}

@Composable
private fun OnboardingBottomBar(
    state: OnboardingViewModel.State,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val label = when (state.currentStep) {
        0 -> "Începe"
        7 -> "Continuă"
        8 -> "Finalizează"
        else -> "Continuă"
    }
    Surface(
        color = SolomonColors.Background,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SolSpacing.base),
            horizontalArrangement = Arrangement.spacedBy(SolSpacing.md)
        ) {
            if (state.canGoBack) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(SolSpacing.xs))
                    Text("Înapoi")
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
            Button(
                onClick = onNext,
                enabled = state.canProceed,
                modifier = Modifier.weight(2f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SolomonColors.Primary,
                    contentColor = SolomonColors.OnPrimary
                )
            ) {
                Text(label)
                Spacer(Modifier.width(SolSpacing.xs))
                Icon(Icons.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ScreenScaffold(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SolSpacing.base, vertical = SolSpacing.lg)
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge, color = SolomonColors.TextPrimary)
        Spacer(Modifier.height(SolSpacing.sm))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = SolomonColors.TextSecondary)
        Spacer(Modifier.height(SolSpacing.xl))
        content()
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SolSpacing.base),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Solomon", style = MaterialTheme.typography.displayLarge, color = SolomonColors.Primary)
        Spacer(Modifier.height(SolSpacing.md))
        Text(
            "Consilierul tău financiar calm și direct.",
            style = MaterialTheme.typography.titleLarge,
            color = SolomonColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(SolSpacing.sm))
        Text(
            "Fără dramă. Fără judecată. Doar date clare și un plan pe zile.",
            style = MaterialTheme.typography.bodyMedium,
            color = SolomonColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(SolSpacing.xl))
        listOf(
            "Îți citesc emailurile și notificările bancare" to "📨",
            "Găsesc tranzacții, abonamente, pattern-uri" to "🔍",
            "Generez primul tău raport în ~60 secunde" to "⚡"
        ).forEach { (line, emoji) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = SolSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(emoji, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(SolSpacing.md))
                Text(line, style = MaterialTheme.typography.bodyLarge, color = SolomonColors.TextPrimary)
            }
        }
    }
}

@Composable
private fun IdentityStep(state: OnboardingViewModel.State, vm: OnboardingViewModel) {
    ScreenScaffold(
        "Cum te cheamă?",
        "Folosesc numele ca să fiu mai natural, nu să te urmăresc."
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = vm::setName,
            label = { Text("Numele tău") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors()
        )
        Spacer(Modifier.height(SolSpacing.xl))
        Text("Cum vrei să-ți vorbesc?", style = MaterialTheme.typography.titleMedium, color = SolomonColors.TextPrimary)
        Spacer(Modifier.height(SolSpacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
            Addressing.entries.forEach { a ->
                FilterChip(
                    selected = state.addressing == a,
                    onClick = { vm.setAddressing(a) },
                    label = { Text(a.label) }
                )
            }
        }
        Spacer(Modifier.height(SolSpacing.xl))
        Text("Ce vârstă ai aproximativ?", style = MaterialTheme.typography.titleMedium, color = SolomonColors.TextPrimary)
        Spacer(Modifier.height(SolSpacing.sm))
        Column(verticalArrangement = Arrangement.spacedBy(SolSpacing.xs)) {
            AgeRange.entries.forEach { ar ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = state.ageRange == ar,
                            onClick = { vm.setAgeRange(ar) }
                        )
                        .padding(vertical = SolSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = state.ageRange == ar, onClick = { vm.setAgeRange(ar) })
                    Text(ar.displayNameRO, style = MaterialTheme.typography.bodyLarge, color = SolomonColors.TextPrimary)
                }
            }
        }
    }
}

private val Addressing.label: String get() = when (this) {
    Addressing.tu -> "Tu"
    Addressing.dumneavoastra -> "Dumneavoastră"
}

@Composable
private fun IncomeStep(state: OnboardingViewModel.State, vm: OnboardingViewModel) {
    ScreenScaffold(
        "Cât câștigi pe lună?",
        "Nu salvez suma exactă — folosesc intervalul ca să calibrez planul."
    ) {
        Text("Interval salarial net (RON)", style = MaterialTheme.typography.titleMedium, color = SolomonColors.TextPrimary)
        Spacer(Modifier.height(SolSpacing.sm))
        Column(verticalArrangement = Arrangement.spacedBy(SolSpacing.xs)) {
            SalaryRange.entries.forEach { sr ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = state.salaryRange == sr,
                            onClick = { vm.setSalaryRange(sr) }
                        )
                        .padding(vertical = SolSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = state.salaryRange == sr, onClick = { vm.setSalaryRange(sr) })
                    Text(sr.label, style = MaterialTheme.typography.bodyLarge, color = SolomonColors.TextPrimary)
                }
            }
        }
        Spacer(Modifier.height(SolSpacing.xl))
        Text("În ce zi a lunii primești salariul?", style = MaterialTheme.typography.titleMedium, color = SolomonColors.TextPrimary)
        Spacer(Modifier.height(SolSpacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ziua: ${state.paydayDay}", style = MaterialTheme.typography.bodyLarge, color = SolomonColors.TextPrimary, modifier = Modifier.width(80.dp))
            Slider(
                value = state.paydayDay.toFloat(),
                onValueChange = { vm.setPaydayDay(it.toInt()) },
                valueRange = 1f..31f,
                steps = 29,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(SolSpacing.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = state.hasSecondaryIncome, onCheckedChange = vm::setHasSecondaryIncome)
            Spacer(Modifier.width(SolSpacing.md))
            Text("Mai am și alte venituri (freelance, chirii etc.)", color = SolomonColors.TextPrimary)
        }
        if (state.hasSecondaryIncome) {
            Spacer(Modifier.height(SolSpacing.sm))
            OutlinedTextField(
                value = if (state.secondaryIncomeApprox == 0) "" else state.secondaryIncomeApprox.toString(),
                onValueChange = { vm.setSecondaryIncomeApprox(it.toIntOrNull() ?: 0) },
                label = { Text("Aproximativ (RON/lună)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )
        }
    }
}

private val SalaryRange.label: String get() = when (this) {
    SalaryRange.under3k -> "Sub 3.000 RON"
    SalaryRange.range3to5 -> "3.000 – 5.000 RON"
    SalaryRange.range5to8 -> "5.000 – 8.000 RON"
    SalaryRange.range8to15 -> "8.000 – 15.000 RON"
    SalaryRange.over15k -> "Peste 15.000 RON"
}

@Composable
private fun BankStep(state: OnboardingViewModel.State, vm: OnboardingViewModel) {
    ScreenScaffold(
        "La ce bancă ai contul principal?",
        "Îl folosesc ca să recunosc mai bine tranzacțiile din notificări."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SolSpacing.xs)) {
            Bank.entries.forEach { b ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = state.primaryBank == b,
                            onClick = { vm.setPrimaryBank(b) }
                        )
                        .padding(vertical = SolSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = state.primaryBank == b, onClick = { vm.setPrimaryBank(b) })
                    Text(b.displayNameRO, style = MaterialTheme.typography.bodyLarge, color = SolomonColors.TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun ObligationsStep(state: OnboardingViewModel.State, vm: OnboardingViewModel) {
    ScreenScaffold(
        "Ce plăți fixe ai lunar?",
        "Opțional. Dacă nu introduci nimic, le găsesc eu din emailuri/notificări."
    ) {
        state.draftObligations.forEach { o ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SolomonColors.Surface),
                modifier = Modifier.fillMaxWidth().padding(vertical = SolSpacing.xs)
            ) {
                Column(Modifier.padding(SolSpacing.md)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = o.name,
                            onValueChange = { v -> vm.updateDraftObligation(o.id) { it.copy(name = v) } },
                            label = { Text("Nume (ex: Chirie, Internet)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = textFieldColors()
                        )
                        IconButton(onClick = { vm.removeDraftObligation(o.id) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Șterge", tint = SolomonColors.Error)
                        }
                    }
                    Spacer(Modifier.height(SolSpacing.sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = if (o.amountRON == 0) "" else o.amountRON.toString(),
                            onValueChange = { v -> vm.updateDraftObligation(o.id) { it.copy(amountRON = v.toIntOrNull() ?: 0) } },
                            label = { Text("Sumă (RON)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = textFieldColors()
                        )
                        Spacer(Modifier.width(SolSpacing.md))
                        OutlinedTextField(
                            value = o.dayOfMonth.toString(),
                            onValueChange = { v -> vm.updateDraftObligation(o.id) { it.copy(dayOfMonth = v.toIntOrNull()?.coerceIn(1, 31) ?: 1) } },
                            label = { Text("Ziua") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.5f),
                            colors = textFieldColors()
                        )
                    }
                    Spacer(Modifier.height(SolSpacing.sm))
                    ExposedChipGroup(
                        items = ObligationKind.entries.map { it to it.displayNameRO },
                        selected = o.kind,
                        onSelect = { v -> vm.updateDraftObligation(o.id) { it.copy(kind = v) } }
                    )
                }
            }
        }
        Spacer(Modifier.height(SolSpacing.md))
        OutlinedButton(
            onClick = vm::addDraftObligation,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ Adaugă obligație")
        }
    }
}

@Composable
private fun GoalsStep(state: OnboardingViewModel.State, vm: OnboardingViewModel) {
    ScreenScaffold(
        "Ce vrei să rezolvi?",
        "Alege unul sau mai multe. Mă ajută să prioritizez ce-ți arăt."
    ) {
        FlowRowWrap {
            OnboardingViewModel.GoalChip.entries.forEach { g ->
                FilterChip(
                    selected = g in state.selectedGoals,
                    onClick = { vm.toggleGoal(g) },
                    label = { Text(g.label) }
                )
            }
        }
        Spacer(Modifier.height(SolSpacing.xl))
        Text("Primul tău obiectiv concret (opțional)", style = MaterialTheme.typography.titleMedium, color = SolomonColors.TextPrimary)
        Spacer(Modifier.height(SolSpacing.sm))
        ExposedChipGroup(
            items = GoalKind.entries.map { it to it.displayNameRO },
            selected = state.firstGoalKind,
            onSelect = vm::setFirstGoalKind
        )
        Spacer(Modifier.height(SolSpacing.md))
        OutlinedTextField(
            value = state.firstGoalDestination,
            onValueChange = vm::setFirstGoalDestination,
            label = { Text("Destinație (ex: Vacanță Grecia)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors()
        )
        Spacer(Modifier.height(SolSpacing.sm))
        OutlinedTextField(
            value = state.firstGoalTargetText,
            onValueChange = vm::setFirstGoalTargetText,
            label = { Text("Sumă țintă (RON)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors()
        )
        Spacer(Modifier.height(SolSpacing.xl))
        Text("Cum te simți de obicei cu banii?", style = MaterialTheme.typography.titleMedium, color = SolomonColors.TextPrimary)
        Spacer(Modifier.height(SolSpacing.xs))
        Text(
            "Opțional. Mă ajută să-ți vorbesc pe limba ta — fără judecată. Dacă sari peste, învăț din cum cheltui.",
            style = MaterialTheme.typography.bodySmall,
            color = SolomonColors.TextSecondary
        )
        Spacer(Modifier.height(SolSpacing.sm))
        val ctx = LocalContext.current
        Column(verticalArrangement = Arrangement.spacedBy(SolSpacing.xs)) {
            MoneyScript.entries.forEach { ms ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = state.moneyScript == ms,
                            onClick = {
                                vm.setMoneyScript(ms)
                                CoachProfileStore.setMoneyScript(ctx, ms)
                            }
                        )
                        .padding(vertical = SolSpacing.xs),
                    verticalAlignment = Alignment.Top
                ) {
                    RadioButton(
                        selected = state.moneyScript == ms,
                        onClick = {
                            vm.setMoneyScript(ms)
                            CoachProfileStore.setMoneyScript(ctx, ms)
                        }
                    )
                    Spacer(Modifier.width(SolSpacing.xs))
                    Column {
                        Text(
                            ms.labelRo.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyLarge,
                            color = SolomonColors.TextPrimary
                        )
                        Text(
                            ms.signatureRo,
                            style = MaterialTheme.typography.bodySmall,
                            color = SolomonColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionsStep(state: OnboardingViewModel.State, vm: OnboardingViewModel) {
    ScreenScaffold(
        "Ce permisiuni îmi dai?",
        "Poți să le schimbi oricând din Setări."
    ) {
        PermissionRow(
            title = "Emailuri",
            description = "Citesc emailurile de la bănci și magazine ca să găsesc tranzacții automat.",
            checked = state.gmailConnected,
            onChange = vm::setGmailConnected
        )
        PermissionRow(
            title = "Notificări",
            description = "Îți arăt alerte și confirmări pe ecranul blocat.",
            checked = state.pushAllowed,
            onChange = vm::setPushAllowed
        )
        PermissionRow(
            title = "Notificări bancare (Listener)",
            description = "Solomon poate importa tranzacții din notificările de la BT Pay, George, ING, Revolut etc. Datele rămân pe telefon. Activezi accesul din Setări → \"Acces la notificări\".",
            checked = state.pushAllowed,
            onChange = { }
        )
        PermissionRow(
            title = "Antrenare model",
            description = "Datele tale ajută la îmbunătățirea recomandărilor (opțional, anonim).",
            checked = state.trainingOptIn,
            onChange = vm::setTrainingOptIn
        )
    }
}

@Composable
private fun PermissionRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SolomonColors.Surface),
        modifier = Modifier.fillMaxWidth().padding(vertical = SolSpacing.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(SolSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = SolomonColors.TextPrimary)
                Spacer(Modifier.height(SolSpacing.xs))
                Text(description, style = MaterialTheme.typography.bodySmall, color = SolomonColors.TextSecondary)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun ProcessingStep(state: OnboardingViewModel.State, vm: OnboardingViewModel) {
    LaunchedEffect(Unit) { vm.runProcessing() }
    ScreenScaffold(
        "Construiesc primul tău tablou…",
        "Mai durează puțin. Poți să te uiți — animația e scurtă."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SolSpacing.md)) {
            state.processingTasks.forEach { t ->
                val color = when (t.state) {
                    OnboardingViewModel.TaskState.Done -> SolomonColors.Success
                    OnboardingViewModel.TaskState.Running -> SolomonColors.Primary
                    OnboardingViewModel.TaskState.Pending -> SolomonColors.TextTertiary
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SolomonColors.Surface, RoundedCornerShape(SolSpacing.md))
                        .padding(SolSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (t.state == OnboardingViewModel.TaskState.Done) {
                        Icon(Icons.Filled.CheckCircle, null, tint = color)
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = color
                        )
                    }
                    Spacer(Modifier.width(SolSpacing.md))
                    Text(t.title, style = MaterialTheme.typography.bodyLarge, color = SolomonColors.TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun WowStep(state: OnboardingViewModel.State) {
    ScreenScaffold(
        "Primul tău tablou",
        "Pe baza a ce ai introdus, iată cum arată situația ta acum."
    ) {
        if (state.isGeneratingWow) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(SolSpacing.xxl),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SolomonColors.Primary)
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = SolomonColors.Surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    state.wowMomentText.ifBlank { "Gata — contul tău e configurat. Apasă Finalizează ca să intri în Solomon." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = SolomonColors.TextPrimary,
                    modifier = Modifier.padding(SolSpacing.lg)
                )
            }
        }
    }
}

@Composable
private fun <T> ExposedChipGroup(
    items: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    FlowRowWrap {
        items.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowWrap(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(SolSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(SolSpacing.xs)
    ) { content() }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SolomonColors.Primary,
    unfocusedBorderColor = SolomonColors.Outline,
    focusedTextColor = SolomonColors.TextPrimary,
    unfocusedTextColor = SolomonColors.TextPrimary,
    focusedLabelColor = SolomonColors.Primary,
    unfocusedLabelColor = SolomonColors.TextSecondary,
    cursorColor = SolomonColors.Primary
)
