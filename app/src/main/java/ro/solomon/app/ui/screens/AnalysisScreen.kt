package ro.solomon.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.ui.theme.SolSpacing
import ro.solomon.app.ui.theme.SolomonColors
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.Transaction
import ro.solomon.core.domain.TransactionCategory
import ro.solomon.core.domain.FlowDirection
import ro.solomon.core.format.RomanianMoneyFormatter
import kotlin.math.max

class AnalysisViewModel : ViewModel() {
    data class CategoryStat(val category: TransactionCategory, val amount: Money, val percent: Float)
    data class State(
        val monthLabel: String = "",
        val totalSpent: Money = Money(0),
        val totalIncome: Money = Money(0),
        val net: Money = Money(0),
        val categories: List<CategoryStat> = emptyList(),
        val topMerchants: List<Pair<String, Int>> = emptyList()
    )

    val state: StateFlow<State> = ServiceLocator.txnRepo.observeAll()
        .map { computeState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State())

    private fun computeState(txns: List<Transaction>): State {
        val now = System.currentTimeMillis() / 1000
        val monthStart = now - 30L * 24 * 3600
        val recent = txns.filter { it.date >= monthStart }
        val spent = recent.filter { it.direction == FlowDirection.outgoing }.sumOf { it.amount.amount }
        val income = recent.filter { it.direction == FlowDirection.incoming }.sumOf { it.amount.amount }
        val grouped = recent
            .filter { it.direction == FlowDirection.outgoing }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(8)
        val totalForPct = max(grouped.sumOf { it.second }, 1)
        val stats = grouped.map { (cat, amt) ->
            CategoryStat(cat, Money(amt), amt.toFloat() / totalForPct)
        }
        val merchants = recent
            .filter { it.direction == FlowDirection.outgoing && !it.merchant.isNullOrBlank() }
            .groupBy { it.merchant!! }
            .mapValues { (_, list) -> list.sumOf { it.amount.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { (m, amt) -> m to amt }
        return State(
            monthLabel = "Ultimele 30 zile",
            totalSpent = Money(spent),
            totalIncome = Money(income),
            net = Money(income - spent),
            categories = stats,
            topMerchants = merchants
        )
    }
}

@Composable
fun AnalysisScreen(vm: AnalysisViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showSpiral by remember { mutableStateOf(false) }
    var showAudit by remember { mutableStateOf(false) }
    var showSuspicious by remember { mutableStateOf(false) }
    var showGoals by remember { mutableStateOf(false) }
    var showEmailParser by remember { mutableStateOf(false) }
    var showModelDownload by remember { mutableStateOf(false) }

    if (showSpiral) { SpiralAlertScreen(onClose = { showSpiral = false }); return }
    if (showAudit) { SubscriptionAuditScreen(onClose = { showAudit = false }); return }
    if (showSuspicious) { SuspiciousTransactionsScreen(onClose = { showSuspicious = false }); return }
    if (showGoals) { GoalsListScreen(onClose = { showGoals = false }); return }
    if (showEmailParser) { EmailParserScreen(onClose = { showEmailParser = false }); return }
    if (showModelDownload) { ModelDownloadScreen(onClose = { showModelDownload = false }); return }

    Column(
        Modifier
            .fillMaxSize()
            .background(SolomonColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(SolSpacing.base)
    ) {
        Text("Analiză", style = MaterialTheme.typography.headlineLarge, color = SolomonColors.TextPrimary)
        Text(state.monthLabel, color = SolomonColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(SolSpacing.base))
        Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
            StatTile("Venit", RomanianMoneyFormatter.format(state.totalIncome.amount, RomanianMoneyFormatter.Style.bareNumber) + " RON", SolomonColors.Incoming, Modifier.weight(1f))
            StatTile("Cheltuit", RomanianMoneyFormatter.format(state.totalSpent.amount, RomanianMoneyFormatter.Style.bareNumber) + " RON", SolomonColors.Outgoing, Modifier.weight(1f))
        }
        Spacer(Modifier.height(SolSpacing.sm))
        val netColor = if (state.net.amount >= 0) SolomonColors.Incoming else SolomonColors.Outgoing
        StatTile("Net", RomanianMoneyFormatter.format(state.net.amount, RomanianMoneyFormatter.Style.bareNumber) + " RON", netColor, Modifier.fillMaxWidth())
        Spacer(Modifier.height(SolSpacing.lg))
        Text("Pe categorii", style = MaterialTheme.typography.titleLarge, color = SolomonColors.TextPrimary)
        Spacer(Modifier.height(SolSpacing.sm))
        if (state.categories.isEmpty()) {
            Text("Nu sunt date încă. Importă tranzacții sau aşteaptă notificările bancare.", color = SolomonColors.TextSecondary)
        } else {
            state.categories.forEach { c ->
                CategoryBar(c)
                Spacer(Modifier.height(SolSpacing.sm))
            }
        }
        Spacer(Modifier.height(SolSpacing.lg))
        Text("Top comercianți", style = MaterialTheme.typography.titleLarge, color = SolomonColors.TextPrimary)
        Spacer(Modifier.height(SolSpacing.sm))
        if (state.topMerchants.isEmpty()) {
            Text("Încă nu avem comercianți evidențiați.", color = SolomonColors.TextSecondary)
        } else {
            state.topMerchants.forEachIndexed { i, (m, amt) ->
                Row(Modifier.fillMaxWidth().padding(vertical = SolSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}.", color = SolomonColors.TextTertiary, modifier = Modifier.width(28.dp))
                    Text(m, color = SolomonColors.TextPrimary, modifier = Modifier.weight(1f))
                    Text(RomanianMoneyFormatter.format(amt, RomanianMoneyFormatter.Style.bareNumber) + " RON", color = SolomonColors.TextSecondary)
                }
                HorizontalDivider(color = SolomonColors.Hairline)
            }
        }
        Spacer(Modifier.height(SolSpacing.lg))
        Text("Instrumente Solomon", style = MaterialTheme.typography.titleLarge, color = SolomonColors.TextPrimary)
        Spacer(Modifier.height(SolSpacing.sm))
        ToolTile("⚠ Alertă spirală", "Verifică presiunea financiară", SolomonColors.Rose) { showSpiral = true }
        Spacer(Modifier.height(SolSpacing.sm))
        ToolTile("🪒 Audit abonamente", "Găseşte abonamente nefolosite", SolomonColors.Amber) { showAudit = true }
        Spacer(Modifier.height(SolSpacing.sm))
        ToolTile("🔍 Tranzacții suspecte", "Sume mari, burst-uri, nocturn", SolomonColors.Amber) { showSuspicious = true }
        Spacer(Modifier.height(SolSpacing.sm))
        ToolTile("◎ Obiective", "Progres + proiecție per obiectiv", SolomonColors.Primary) { showGoals = true }
        Spacer(Modifier.height(SolSpacing.sm))
        ToolTile("✉ Importă email", "Parsează Glovo/Netflix/Enel din text", SolomonColors.Primary) { showEmailParser = true }
        Spacer(Modifier.height(SolSpacing.sm))
        ToolTile("⚙ Model AI", "Alege model lingvistic · Mistral cloud (UE)", SolomonColors.Primary) { showModelDownload = true }
        Spacer(Modifier.height(SolSpacing.xl))
    }
}

@Composable
private fun ToolTile(title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SolomonColors.Surface),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(SolSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.18f))
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(title.take(1), color = accent, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.width(SolSpacing.md))
            Column(Modifier.weight(1f)) {
                Text(title.removePrefix("⚠ ").removePrefix("🪒 ").removePrefix("🔍 ").removePrefix("◎ "),
                    color = SolomonColors.TextPrimary, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, color = SolomonColors.TextTertiary, style = MaterialTheme.typography.bodySmall)
            }
            Text("›", color = SolomonColors.TextTertiary, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SolomonColors.Surface),
        modifier = modifier
    ) {
        Column(Modifier.padding(SolSpacing.md)) {
            Text(label, color = SolomonColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(2.dp))
            Text(value, color = accent, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun CategoryBar(c: AnalysisViewModel.CategoryStat) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.category.displayNameRO, color = SolomonColors.TextPrimary, modifier = Modifier.weight(1f))
            Text(RomanianMoneyFormatter.format(c.amount.amount, RomanianMoneyFormatter.Style.bareNumber) + " RON", color = SolomonColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SolomonColors.SurfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(c.percent.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SolomonColors.Primary)
            )
        }
    }
}
