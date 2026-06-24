package ro.solomon.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.ui.components.*
import ro.solomon.app.ui.theme.*
import ro.solomon.analytics.GoalProgress
import ro.solomon.analytics.GoalProgressReport
import ro.solomon.core.domain.GoalFeasibility
import ro.solomon.core.domain.GoalKind
import ro.solomon.core.format.RomanianMoneyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoalsListViewModel : ViewModel() {
    data class State(val loading: Boolean = true, val reports: List<GoalProgressReport> = emptyList())

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init { viewModelScope.launch { load() } }

    private suspend fun load() {
        val goals = ServiceLocator.goalRepo.fetchAll()
        val txns = ServiceLocator.txnRepo.fetchAll()
        val cashFlow = ServiceLocator.cashFlow.analyze(txns)
        val pace = cashFlow.monthlySavingsAvg
        val reports = goals.map { GoalProgress().evaluate(it, pace) }
        _state.value = State(loading = false, reports = reports)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsListScreen(onClose: () -> Unit) {
    val vm: GoalsListViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Obiective", color = SolomonColors.Primary) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Înapoi", tint = SolomonColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SolomonColors.Background)
            )
        },
        containerColor = SolomonColors.Background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> SolLoadingIndicator(SolAccent.Mint, "Calculăm proiecțiile…")
                state.reports.isEmpty() -> EmptyState(onClose)
                else -> Content(state.reports)
            }
        }
    }
}

@Composable
private fun Content(reports: List<GoalProgressReport>) {
    val totalSaved = reports.sumOf { it.goal.amountSaved.amount }
    val totalTarget = reports.sumOf { it.goal.amountTarget.amount }
    val avgProgress = if (totalTarget > 0) totalSaved.toFloat() / totalTarget.toFloat() else 0f
    val totalMonthly = reports.sumOf { it.monthlyRequired.amount }
    val maxMonths = reports.maxOfOrNull { it.monthsRemaining } ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SolSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SolSpacing.base)
    ) {
        SolHeroCard(accent = SolAccent.Mint, badge = "${reports.size} ACTIVE") {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(SolSpacing.lg)) {
                SolProgressRing(progress = avgProgress, label = "PROGRES", size = 120, lineWidth = 9, accent = SolAccent.Mint)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SolSpacing.xs)) {
                    SolHeroLabel("ACUMULAT TOTAL")
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = RomanianMoneyFormatter.format(totalSaved, RomanianMoneyFormatter.Style.bareNumber),
                            color = SolomonColors.TextPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("RON", color = SolomonColors.TextTertiary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Text(
                        "din ${RomanianMoneyFormatter.format(totalTarget, RomanianMoneyFormatter.Style.bareNumber)} țintă",
                        color = SolomonColors.TextTertiary,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(SolSpacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
                        if (totalMonthly > 0) SolChip("+${RomanianMoneyFormatter.format(totalMonthly, RomanianMoneyFormatter.Style.bareNumber)} lună", accent = SolAccent.Mint)
                        if (maxMonths > 0) SolChip("$maxMonths luni", accent = SolAccent.Mint)
                    }
                }
            }
        }

        SolInsightCard(label = "Solomon · Proiecție", timestamp = "recalc azi", accent = SolAccent.Mint) {
            val first = reports.firstOrNull()
            val text = if (first != null) {
                val monthly = first.monthlyRequired.amount
                val months = if (first.monthsRemaining > 0) "${first.monthsRemaining} luni" else "termen scurt"
                "Dacă ții ritmul de +${RomanianMoneyFormatter.format(monthly, RomanianMoneyFormatter.Style.bareNumber)} RON/lună, ajungi la ${first.goal.kind.displayNameRO} în $months."
            } else {
                "Setează contribuții lunare ca Solomon să proiecteze când ajungi la fiecare obiectiv."
            }
            Text(text, color = SolomonColors.TextSecondary, fontSize = 13.sp)
        }

        SolSectionHeaderRow("Obiectivele tale", "${reports.size} active")
        SolListCard {
            reports.forEachIndexed { i, r ->
                if (i > 0) SolHairlineDivider()
                GoalRow(r)
            }
        }
    }
}

@Composable
private fun GoalRow(report: GoalProgressReport) {
    val goal = report.goal
    val accent = accentFor(goal.kind)
    Column(
        modifier = Modifier.fillMaxWidth().padding(SolSpacing.base),
        verticalArrangement = Arrangement.spacedBy(SolSpacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SolSpacing.md)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent.color.copy(alpha = 0.18f))
                    .border(1.dp, accent.color.copy(alpha = 0.25f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(iconFor(goal.kind), color = accent.color, fontSize = 16.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    goal.destination ?: goal.kind.displayNameRO,
                    color = SolomonColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${formatMonthYear(goal.deadline)} · ${report.monthsRemaining} luni",
                    color = SolomonColors.TextTertiary,
                    fontSize = 11.sp
                )
            }
            SolChip(chipFor(report.feasibility).first, accent = chipFor(report.feasibility).second)
        }
        SolLinearProgress(
            progress = report.progressFraction.toFloat(),
            accent = accent,
            height = 6
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${RomanianMoneyFormatter.format(goal.amountSaved.amount, RomanianMoneyFormatter.Style.bareNumber)} / ${RomanianMoneyFormatter.format(goal.amountTarget.amount, RomanianMoneyFormatter.Style.bareNumber)} RON",
                color = SolomonColors.TextSecondary,
                fontSize = 11.sp
            )
            if (report.monthlyRequired.amount > 0) {
                Text(
                    "+${RomanianMoneyFormatter.format(report.monthlyRequired.amount, RomanianMoneyFormatter.Style.bareNumber)}/lună",
                    color = SolomonColors.TextTertiary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun accentFor(kind: GoalKind): SolAccent = when (kind) {
    GoalKind.emergency_fund -> SolAccent.Mint
    GoalKind.vacation -> SolAccent.Blue
    GoalKind.car, GoalKind.house -> SolAccent.Violet
    GoalKind.debt_payoff -> SolAccent.Rose
    GoalKind.custom -> SolAccent.Mint
}

private fun iconFor(kind: GoalKind): String = when (kind) {
    GoalKind.vacation -> "✈"
    GoalKind.car -> "🚗"
    GoalKind.house -> "🏠"
    GoalKind.emergency_fund -> "🛡"
    GoalKind.debt_payoff -> "💳"
    GoalKind.custom -> "◎"
}

private fun chipFor(feasibility: GoalFeasibility): Pair<String, SolAccent> = when (feasibility) {
    GoalFeasibility.easy, GoalFeasibility.on_track -> "on track" to SolAccent.Mint
    GoalFeasibility.challenging_but_possible -> "restant" to SolAccent.Amber
    GoalFeasibility.unrealistic -> "blocat" to SolAccent.Rose
}

private fun formatMonthYear(epochMillis: Long): String {
    val fmt = SimpleDateFormat("MMM yyyy", Locale("ro"))
    return fmt.format(Date(epochMillis))
}

@Composable
private fun EmptyState(onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(SolSpacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("◎", color = SolomonColors.Primary, fontSize = 56.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(SolSpacing.md))
        Text("Niciun obiectiv încă", color = SolomonColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(SolSpacing.sm))
        Text(
            "Adaugă primul tău obiectiv financiar și Solomon te ajută să ajungi acolo.",
            color = SolomonColors.TextTertiary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(SolSpacing.lg))
        OutlinedButton(onClick = onClose) { Text("Închide") }
    }
}
