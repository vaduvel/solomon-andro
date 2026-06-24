package ro.solomon.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ro.solomon.app.services.CoachProfileStore
import ro.solomon.app.services.MoneyScript
import ro.solomon.app.ui.components.SolInsightCard
import ro.solomon.app.ui.components.SolLinearProgress
import ro.solomon.app.ui.theme.SolAccent
import ro.solomon.app.ui.theme.SolRadius
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
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(SolRadius.lg))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(SolRadius.lg))
    ) { content() }
}

@Composable
private fun ProgressHeader(current: Int, total: Int) {
    Column(Modifier.padding(horizontal = SolSpacing.base, vertical = SolSpacing.md)) {
        SolLinearProgress(
            progress = ((current + 1).toFloat() / total).coerceIn(0f, 1f),
            accent = SolAccent.Mint,
            height = 4,
            modifier = Modifier.fillMaxWidth()
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
        0 -> "\u00cencepe"
        7 -> "Continu\u0103"
        8 -> "Finalizeaz\u0103"
        else -> "Continu\u0103"
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
                    Text("\u00cenapoi")
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
            "Consilierul t\u0103u financiar calm \u0219i direct.",
            style = MaterialTheme.typography.titleLarge,
            color = SolomonColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(SolSpacing.sm))
        Text(
            "F\u0103r\u0103 dram\u0103. F\u0103r\u0103 judecat\u0103. Doar date clare \u0219i un plan pe zile.",
            style = MaterialTheme.typography.bodyMedium,
            color = SolomonColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(SolSpacing.xl))
        listOf(
            "\u00ce\u021bi citesc emailurile \u0219i notific\u0103rile bancare" to "\ud83d\udce8",
            "G\u0103sesc tranzac\u021bii, abonamente, pattern-uri" to "\ud83d\udd0d",
            "Generez primul t\u0103u raport \u00een ~60 secunde" to "\u26a1"
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
        "Cum te cheam\u0103?",
        "Folosesc numele ca s\u0103 fiu mai natural, nu s\u0103 te urm\u0103resc."
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = vm::setName,
            label = { Text("Numele t\u0103u") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors()
        )
        Spacer(Modifier.height(SolSpacing.xl))
        Text("Cum vrei s\u0103-\u021bi vorbesc?", style = MaterialTheme.typography.titleMedium, color = SolomonColors.TextPrimary)
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
        Text("Ce v\u00e2rst\u0103 ai aproximativ?", style = MaterialTheme.typography.titleMedium, color = SolomonColors.TextPrimary)
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
    Addressing.dumneavoastra -> "Dumneavoastr\u0103"
}

@Composable
private fun IncomeStep(state: OnboardingViewModel.State, vm: OnboardingViewModel) {
    ScreenScaffold(
        "C\u00e2t c\u00e2\u0219tigi pe lun\u0103?",
        "Nu salvez suma exact\u0103 \u2014 folosesc intervalul ca s\u0103 calibrez planul."
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
        Text("\u00cen ce zi a lunii prime\u0219ti salariul?", style = MaterialTheme.typography.titleMedium, color = SolomonColors.TextPrimary)
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
            Text("Mai am \u0219i alte venituri (freelance, chirii etc.)", color = SolomonColors.TextPrimary)
        }
        if (state.hasSecondaryIncome) {
            Spacer(Modifier.height(SolSpacing.sm))
            OutlinedTextField(
                value = if (state.secondaryIncomeApprox == 0) "" else state.secondaryIncomeApprox.toString(),
                onValueChange = { vm.setSecondaryIncomeApprox(it.toIntOrNull() ?: 0) },
                label = { Text("Aproximativ (RON/lun\u0103)") },
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
    SalaryRange.range3to5 -> "3.000 \u2013 5.000 RON"
    SalaryRange.range5to8 -> "5.000 \u2013 8.000 RON"
    SalaryRange.range8to15 -> "8.000 \u2013 15.000 RON"
    SalaryRange.over15k -> "Peste 15.000 RON"
}

@Composable
private fun BankStep(state: OnboardingViewModel.State, vm: OnboardingViewModel) {
    ScreenScaffold(
        "La ce banc\u0103 ai contul principal?",
        "\u00cel folosesc ca s\u0103 recunosc mai bine tranzac\u021biile din notific\u0103ri."
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
        "Ce pl\u0103\u021bi fixe ai lunar?",
        "Op\u021bional. Dac\u0103 nu introduci nimic, le g\u0103sesc eu din emailuri/notific\u0103ri."
    ) {
        state.draftObligations.forEach { o ->
            GlassCard(Modifier.fillMaxWidth().padding(vertical = SolSpacing.xs)) {
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "\u0218terge", tint = SolomonColors.Error)
                        }
                    }
                    Spacer(Modifier.height(SolSpacing.sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = if (o.amountRON == 0) "" else o.amountRON.toString(),
                            onValueChange = { v -> vm.updateDraftObligation(o.id) { it.copy(amountRON = v.toIntOrNull() ?: 0) } },
                            label = { Text("Sum\u0103 (RON)") },
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
            Text("+ Adaug\u0103 obliga\u021bie")
        }
    }
}

@Composable
private fun GoalsStep(state: OnboardingViewModel.State, vm: OnboardingViewModel) {
    ScreenScaffold(
        "Ce vrei s\u0103 rezolvi?",
        "Alege unul sau mai multe. M\u0103 ajut\u0103 s\u0103 prioritizez ce-\u021bi ar\u0103t."
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
        Text("Primul t\u0103u obiectiv concret (op\u021bional)", style = MaterialTheme.typography.titleMedium, color = SolomonColors.TextPrimary)
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
            label = { Text("Destina\u021bie (ex: Vacan\u021b\u0103 Grecia)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors()
        )
        Spacer(Modifier.height(SolSpacing.sm))
        OutlinedTextField(
            value = state.firstGoalTargetText,
            onValueChange = vm::setFirstGoalTargetText,
            label = { Text("Sum\u0103 \u021bint\u0103 (RON)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors()
        )
        Spacer(Modifier.height(SolSpacing.xl))
        Text("Cum te sim\u021bi de obicei cu banii?", style = MaterialTheme.typography.titleMedium, color = SolomonColors.TextPrimary)
        Spacer(Modifier.height(SolSpacing.xs))
        Text(
            "Op\u021bional. M\u0103 ajut\u0103 s\u0103-\u021bi vorbesc pe limba ta \u2014 f\u0103r\u0103 judecat\u0103. Dac\u0103 sari peste, \u00eenv\u0103\u021b din cum cheltui.",
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
        "Ce permisiuni \u00eemi dai?",
        "Po\u021bi s\u0103 le schimbi oric\u00e2nd din Set\u0103ri."
    ) {
        PermissionRow(
            title = "Emailuri",
            description = "Citesc emailurile de la b\u0103nci \u0219i magazine ca s\u0103 g\u0103sesc tranzac\u021bii automat.",
            checked = state.gmailConnected,
            onChange = vm::setGmailConnected
        )
        PermissionRow(
            title = "Notific\u0103ri",
            description = "\u00ce\u021bi ar\u0103t alerte \u0219i confirm\u0103ri pe ecranul blocat.",
            checked = state.pushAllowed,
            onChange = vm::setPushAllowed
        )
        PermissionRow(
            title = "Notific\u0103ri bancare (Listener)",
            description = "Solomon poate importa tranzac\u021bii din notific\u0103rile de la BT Pay, George, ING, Revolut etc. Datele r\u0103m\u00e2n pe telefon. Activezi accesul din Set\u0103ri \u2192 \"Acces la notific\u0103ri\".",
            checked = state.pushAllowed,
            onChange = { }
        )
        PermissionRow(
            title = "Antrenare model",
            description = "Datele tale ajut\u0103 la \u00eembun\u0103t\u0103\u021birea recomand\u0103rilor (op\u021bional, anonim).",
            checked = state.trainingOptIn,
            onChange = vm::setTrainingOptIn
        )
    }
}

@Composable
private fun PermissionRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    GlassCard(Modifier.fillMaxWidth().padding(vertical = SolSpacing.xs)) {
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
        "Construiesc primul t\u0103u tablou\u2026",
        "Mai dureaz\u0103 pu\u021bin. Po\u021bi s\u0103 te ui\u021bi \u2014 anima\u021bia e scurt\u0103."
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
                        .clip(RoundedCornerShape(SolRadius.md))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(SolRadius.md))
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
        "Primul t\u0103u tablou",
        "Pe baza a ce ai introdus, iat\u0103 cum arat\u0103 situa\u021bia ta acum."
    ) {
        if (state.isGeneratingWow) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(SolSpacing.xxl),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SolomonColors.Primary)
            }
        } else {
            SolInsightCard(label = "Solomon \u00b7 Primul tablou", accent = SolAccent.Mint) {
                Text(
                    state.wowMomentText.ifBlank { "Gata \u2014 contul t\u0103u e configurat. Apas\u0103 Finalizeaz\u0103 ca s\u0103 intri \u00een Solomon." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = SolomonColors.TextPrimary
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
