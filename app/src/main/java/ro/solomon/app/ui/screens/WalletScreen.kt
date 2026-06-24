package ro.solomon.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import ro.solomon.app.ui.components.EmptyStateView
import ro.solomon.app.ui.components.SolBackButton
import ro.solomon.app.ui.components.SolChip
import ro.solomon.app.ui.components.SolHairlineDivider
import ro.solomon.app.ui.components.SolLinearProgress
import ro.solomon.app.ui.components.SolListCard
import ro.solomon.app.ui.components.SolSecondaryButton
import ro.solomon.app.ui.theme.SolAccent
import ro.solomon.app.ui.theme.SolRadius
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
            .verticalScroll(rememberScrollState())
            .padding(SolSpacing.base),
        verticalArrangement = Arrangement.spacedBy(SolSpacing.sm)
    ) {
        Text("Bani", style = MaterialTheme.typography.headlineLarge, color = SolomonColors.TextPrimary)
        Text("Obligații, obiective și abonamente", color = SolomonColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(SolSpacing.xs))
        WalletCard(
            icon = Icons.Filled.AccountBalance,
            accent = SolAccent.Rose,
            title = "Obligații lunare",
            subtitle = "${state.obligations.size} • ${RomanianMoneyFormatter.format(state.monthlyDebt, RomanianMoneyFormatter.Style.bareNumber)} RON",
            onClick = { onSection(WalletSection.Obligations) }
        )
        WalletCard(
            icon = Icons.Filled.Flag,
            accent = SolAccent.Mint,
            title = "Obiective",
            subtitle = "${state.goals.size} • ${RomanianMoneyFormatter.format(state.goalsSaved, RomanianMoneyFormatter.Style.bareNumber)} / ${RomanianMoneyFormatter.format(state.goalsTarget, RomanianMoneyFormatter.Style.bareNumber)} RON",
            onClick = { onSection(WalletSection.Goals) }
        )
        WalletCard(
            icon = Icons.Filled.Autorenew,
            accent = SolAccent.Blue,
            title = "Abonamente",
            subtitle = "${state.subscriptions.size} active • ${RomanianMoneyFormatter.format(state.monthlySubs, RomanianMoneyFormatter.Style.bareNumber)} RON/lună",
            onClick = { onSection(WalletSection.Subscriptions) }
        )
    }
}

@Composable
private fun WalletCard(icon: ImageVector, accent: SolAccent, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SolRadius.lg))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(SolRadius.lg))
            .clickable { onClick() }
            .padding(SolSpacing.base),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.color.copy(alpha = 0.15f))
                .border(1.dp, accent.color.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent.color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(SolSpacing.md))
        Column(Modifier.weight(1f)) {
            Text(title, color = SolomonColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = SolomonColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = SolomonColors.TextTertiary)
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
            .verticalScroll(rememberScrollState())
            .padding(SolSpacing.base),
        verticalArrangement = Arrangement.spacedBy(SolSpacing.base)
    ) {
        SolBackButton(onClick = onBack)
        when (section) {
            WalletSection.Obligations -> ObligationsList(state.obligations, onTapObligation, onAddObligation)
            WalletSection.Goals -> GoalsList(state.goals, onTapGoal, onAddGoal)
            WalletSection.Subscriptions -> SubscriptionsList(state.subscriptions, onTapSubscription, onAddSubscription)
        }
    }
}

@Composable
private fun SectionHeader(title: String, onAdd: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = SolomonColors.TextPrimary, modifier = Modifier.weight(1f))
        SolSecondaryButton(title = "+ Adaugă", onClick = onAdd)
    }
}

@Composable
private fun ObligationsList(
    items: List<Obligation>,
    onTap: (Obligation) -> Unit,
    onAdd: () -> Unit
) {
    SectionHeader("Obligații", onAdd)
    if (items.isEmpty()) {
        EmptyStateView(
            icon = "📄",
            title = "Nicio obligație",
            subtitle = "Apasă + Adaugă ca să începi.",
            accent = SolAccent.Amber
        )
    } else {
        SolListCard {
            items.forEachIndexed { i, o ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onTap(o) }
                        .padding(SolSpacing.base),
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
                if (i < items.lastIndex) SolHairlineDivider()
            }
        }
    }
}

@Composable
private fun GoalsList(
    items: List<Goal>,
    onTap: (Goal) -> Unit,
    onAdd: () -> Unit
) {
    SectionHeader("Obiective", onAdd)
    if (items.isEmpty()) {
        EmptyStateView(
            icon = "🎯",
            title = "Niciun obiectiv încă",
            subtitle = "Apasă + Adaugă ca să începi să economisești.",
            accent = SolAccent.Mint
        )
    } else {
        SolListCard {
            items.forEachIndexed { i, g ->
                val pct = if (g.amountTarget.amount > 0) g.amountSaved.amount.toFloat() / g.amountTarget.amount else 0f
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onTap(g) }
                        .padding(SolSpacing.base)
                ) {
                    Text(g.destination ?: g.kind.displayNameRO, color = SolomonColors.TextPrimary)
                    Text("${RomanianMoneyFormatter.format(g.amountSaved.amount, RomanianMoneyFormatter.Style.bareNumber)} / ${RomanianMoneyFormatter.format(g.amountTarget.amount, RomanianMoneyFormatter.Style.bareNumber)} RON", color = SolomonColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(6.dp))
                    SolLinearProgress(progress = pct.coerceIn(0f, 1f), accent = SolAccent.Mint, height = 8, modifier = Modifier.fillMaxWidth())
                }
                if (i < items.lastIndex) SolHairlineDivider()
            }
        }
    }
}

@Composable
private fun SubscriptionsList(
    items: List<Subscription>,
    onTap: (Subscription) -> Unit,
    onAdd: () -> Unit
) {
    SectionHeader("Abonamente", onAdd)
    if (items.isEmpty()) {
        EmptyStateView(
            icon = "🔁",
            title = "Niciun abonament",
            subtitle = "Apasă + Adaugă ca să le urmărești.",
            accent = SolAccent.Blue
        )
    } else {
        SolListCard {
            items.forEachIndexed { i, s ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onTap(s) }
                        .padding(SolSpacing.base),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(s.name, color = SolomonColors.TextPrimary)
                        Text("${RomanianMoneyFormatter.format(s.amountMonthly.amount, RomanianMoneyFormatter.Style.bareNumber)} RON/lună", color = SolomonColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    SolChip(s.cancellationDifficulty.displayNameRO, accent = SolAccent.Amber)
                    Spacer(Modifier.width(SolSpacing.xs))
                    Icon(Icons.Filled.ChevronRight, null, tint = SolomonColors.TextTertiary)
                }
                if (i < items.lastIndex) SolHairlineDivider()
            }
        }
    }
}
