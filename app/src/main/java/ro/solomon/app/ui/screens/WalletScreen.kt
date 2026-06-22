package ro.solomon.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.ui.theme.SolSpacing
import ro.solomon.app.ui.theme.SolomonColors
import ro.solomon.app.ui.wallet.GoalEditScreen
import ro.solomon.app.ui.wallet.ObligationEditScreen
import ro.solomon.app.ui.wallet.SubscriptionEditScreen
import ro.solomon.core.domain.Goal
import ro.solomon.core.domain.Obligation
import ro.solomon.core.domain.Subscription
import ro.solomon.core.format.RomanianMoneyFormatter

class WalletViewModel : ViewModel() {
    data class State(
        val obligations: List<Obligation> = emptyList(),
        val goals: List<Goal> = emptyList(),
        val subscriptions: List<Subscription> = emptyList(),
        val monthlyDebt: Int = 0,
        val monthlySubs: Int = 0,
        val goalsSaved: Int = 0,
        val goalsTarget: Int = 0
    )

    val state: StateFlow<State> = combine(
        ServiceLocator.obligationRepo.observeAll(),
        ServiceLocator.goalRepo.observeAll(),
        ServiceLocator.subRepo.observeAll()
    ) { obl, goals, subs ->
        State(
            obligations = obl,
            goals = goals,
            subscriptions = subs,
            monthlyDebt = obl.sumOf { it.amount.amount },
            monthlySubs = subs.sumOf { it.amountMonthly.amount },
            goalsSaved = goals.sumOf { it.amountSaved.amount },
            goalsTarget = goals.sumOf { it.amountTarget.amount }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State())
}

private enum class WalletSection { Obligations, Goals, Subscriptions }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(vm: WalletViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var section by remember { mutableStateOf<WalletSection?>(null) }
    var editingObligation by remember { mutableStateOf<String?>(null) }
    var editingSubscription by remember { mutableStateOf<String?>(null) }
    var editingGoal by remember { mutableStateOf<String?>(null) }
    var showNewObligation by remember { mutableStateOf(false) }
    var showNewSubscription by remember { mutableStateOf(false) }
    var showNewGoal by remember { mutableStateOf(false) }

    if (showNewObligation) {
        ObligationEditScreen(obligationId = null, onClose = { showNewObligation = false })
        return
    }
    if (editingObligation != null) {
        ObligationEditScreen(obligationId = editingObligation, onClose = { editingObligation = null })
        return
    }
    if (showNewSubscription) {
        SubscriptionEditScreen(subscriptionId = null, onClose = { showNewSubscription = false })
        return
    }
    if (editingSubscription != null) {
        SubscriptionEditScreen(subscriptionId = editingSubscription, onClose = { editingSubscription = null })
        return
    }
    if (showNewGoal) {
        GoalEditScreen(goalId = null, onClose = { showNewGoal = false })
        return
    }
    if (editingGoal != null) {
        GoalEditScreen(goalId = editingGoal, onClose = { editingGoal = null })
        return
    }

    if (section != null) {
        SectionDetail(
            section = section!!,
            state = state,
            onBack = { section = null },
            onTapObligation = { editingObligation = it.id },
            onAddObligation = { showNewObligation = true },
            onTapSubscription = { editingSubscription = it.id },
            onAddSubscription = { showNewSubscription = true },
            onTapGoal = { editingGoal = it.id },
            onAddGoal = { showNewGoal = true }
        )
    } else {
        WalletOverview(state, onSection = { section = it })
    }
}

@Composable
private fun WalletOverview(state: WalletViewModel.State, onSection: (WalletSection) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(SolomonColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(SolSpacing.base)
    ) {
        Text("Portofel", style = MaterialTheme.typography.headlineLarge, color = SolomonColors.TextPrimary)
        Spacer(Modifier.height(SolSpacing.base))
        WalletCard(
            title = "Obligații lunare",
            subtitle = "${state.obligations.size} • ${RomanianMoneyFormatter.format(state.monthlyDebt, RomanianMoneyFormatter.Style.bareNumber)} RON",
            onClick = { onSection(WalletSection.Obligations) }
        )
        Spacer(Modifier.height(SolSpacing.sm))
        WalletCard(
            title = "Obiective",
            subtitle = "${state.goals.size} • ${RomanianMoneyFormatter.format(state.goalsSaved, RomanianMoneyFormatter.Style.bareNumber)} / ${RomanianMoneyFormatter.format(state.goalsTarget, RomanianMoneyFormatter.Style.bareNumber)} RON",
            onClick = { onSection(WalletSection.Goals) }
        )
        Spacer(Modifier.height(SolSpacing.sm))
        WalletCard(
            title = "Abonamente",
            subtitle = "${state.subscriptions.size} active • ${RomanianMoneyFormatter.format(state.monthlySubs, RomanianMoneyFormatter.Style.bareNumber)} RON/lună",
            onClick = { onSection(WalletSection.Subscriptions) }
        )
    }
}

@Composable
private fun WalletCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SolomonColors.Surface),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(SolSpacing.base),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = SolomonColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = SolomonColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = SolomonColors.TextTertiary)
        }
    }
}

@Composable
private fun SectionDetail(
    section: WalletSection,
    state: WalletViewModel.State,
    onBack: () -> Unit,
    onTapObligation: (Obligation) -> Unit,
    onAddObligation: () -> Unit,
    onTapSubscription: (Subscription) -> Unit,
    onAddSubscription: () -> Unit,
    onTapGoal: (Goal) -> Unit,
    onAddGoal: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(SolomonColors.Background)
            .padding(SolSpacing.base)
    ) {
        TextButton(onClick = onBack) { Text("← Înapoi", color = SolomonColors.Primary) }
        Spacer(Modifier.height(SolSpacing.sm))
        when (section) {
            WalletSection.Obligations -> ObligationsList(state.obligations, onTapObligation, onAddObligation)
            WalletSection.Goals -> GoalsList(state.goals, onTapGoal, onAddGoal)
            WalletSection.Subscriptions -> SubscriptionsList(state.subscriptions, onTapSubscription, onAddSubscription)
        }
    }
}

@Composable
private fun ObligationsList(
    items: List<Obligation>,
    onTap: (Obligation) -> Unit,
    onAdd: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Obligații", style = MaterialTheme.typography.headlineSmall, color = SolomonColors.TextPrimary, modifier = Modifier.weight(1f))
        TextButton(onClick = onAdd) { Text("+ Adaugă", color = SolomonColors.Primary) }
    }
    Spacer(Modifier.height(SolSpacing.base))
    if (items.isEmpty()) EmptyHint("Nu avem obligații înregistrate. Apasă + Adaugă.")
    items.forEach { o ->
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onTap(o) }
                .padding(vertical = SolSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(o.name, color = SolomonColors.TextPrimary)
                Text("Ziua ${o.dayOfMonth} • ${o.kind.displayNameRO}", color = SolomonColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Text(RomanianMoneyFormatter.format(o.amount, RomanianMoneyFormatter.Style.short), color = SolomonColors.TextPrimary, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.width(SolSpacing.xs))
            Icon(Icons.Filled.ChevronRight, null, tint = SolomonColors.TextTertiary)
        }
        HorizontalDivider(color = SolomonColors.Hairline)
    }
}

@Composable
private fun GoalsList(
    items: List<Goal>,
    onTap: (Goal) -> Unit,
    onAdd: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Obiective", style = MaterialTheme.typography.headlineSmall, color = SolomonColors.TextPrimary, modifier = Modifier.weight(1f))
        TextButton(onClick = onAdd) { Text("+ Adaugă", color = SolomonColors.Primary) }
    }
    Spacer(Modifier.height(SolSpacing.base))
    if (items.isEmpty()) EmptyHint("Nu ai obiective încă. Apasă + Adaugă.")
    items.forEach { g ->
        val pct = if (g.amountTarget.amount > 0) g.amountSaved.amount.toFloat() / g.amountTarget.amount else 0f
        Column(
            Modifier
                .fillMaxWidth()
                .clickable { onTap(g) }
                .padding(vertical = SolSpacing.sm)
        ) {
            Text(g.destination ?: g.kind.displayNameRO, color = SolomonColors.TextPrimary)
            Text("${RomanianMoneyFormatter.format(g.amountSaved.amount, RomanianMoneyFormatter.Style.bareNumber)} / ${RomanianMoneyFormatter.format(g.amountTarget.amount, RomanianMoneyFormatter.Style.bareNumber)} RON", color = SolomonColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { pct.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = SolomonColors.Primary,
                trackColor = SolomonColors.SurfaceVariant
            )
        }
        HorizontalDivider(color = SolomonColors.Hairline)
    }
}

@Composable
private fun SubscriptionsList(
    items: List<Subscription>,
    onTap: (Subscription) -> Unit,
    onAdd: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Abonamente", style = MaterialTheme.typography.headlineSmall, color = SolomonColors.TextPrimary, modifier = Modifier.weight(1f))
        TextButton(onClick = onAdd) { Text("+ Adaugă", color = SolomonColors.Primary) }
    }
    Spacer(Modifier.height(SolSpacing.base))
    if (items.isEmpty()) EmptyHint("Nu am găsit abonamente. Apasă + Adaugă.")
    items.forEach { s ->
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onTap(s) }
                .padding(vertical = SolSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(s.name, color = SolomonColors.TextPrimary)
                Text("${RomanianMoneyFormatter.format(s.amountMonthly.amount, RomanianMoneyFormatter.Style.bareNumber)} RON/lună", color = SolomonColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            AssistChip(onClick = {}, label = { Text(s.cancellationDifficulty.displayNameRO) })
            Spacer(Modifier.width(SolSpacing.xs))
            Icon(Icons.Filled.ChevronRight, null, tint = SolomonColors.TextTertiary)
        }
        HorizontalDivider(color = SolomonColors.Hairline)
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, color = SolomonColors.TextSecondary, modifier = Modifier.padding(vertical = SolSpacing.base))
}
