package ro.solomon.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.ui.components.EmptyStateView
import ro.solomon.app.ui.components.SolHairlineDivider
import ro.solomon.app.ui.components.SolLinearProgress
import ro.solomon.app.ui.components.SolListCard
import ro.solomon.app.ui.components.SolSectionHeaderRow
import ro.solomon.app.ui.components.SolStatCard
import ro.solomon.app.ui.theme.SolAccent
import ro.solomon.app.ui.theme.SolRadius
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

    fun fmt(v: Int) = RomanianMoneyFormatter.format(v, RomanianMoneyFormatter.Style.bareNumber) + " RON"

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SolSpacing.base)
    ) {
        Text("Analiză", style = MaterialTheme.typography.headlineLarge, color = SolomonColors.TextPrimary)
        Text(state.monthLabel, color = SolomonColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(SolSpacing.base))
        Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
            SolStatCard(
                label = "30Z",
                name = "Venit",
                value = fmt(state.totalIncome.amount),
                icon = Icons.Filled.ArrowDownward,
                iconAccent = SolAccent.Success,
                modifier = Modifier.weight(1f)
            )
            SolStatCard(
                label = "30Z",
                name = "Cheltuit",
                value = fmt(state.totalSpent.amount),
                icon = Icons.Filled.ArrowUpward,
                iconAccent = SolAccent.Error,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(SolSpacing.sm))
        SolStatCard(
            label = "30Z",
            name = "Net",
            value = fmt(state.net.amount),
            icon = Icons.Filled.AccountBalanceWallet,
            iconAccent = if (state.net.amount >= 0) SolAccent.Success else SolAccent.Error,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(SolSpacing.lg))
        SolSectionHeaderRow(title = "Pe categorii")
        if (state.categories.isEmpty()) {
            EmptyStateView(
                icon = "📊",
                title = "Nu sunt date încă",
                subtitle = "Importă tranzacții sau așteaptă notificările bancare.",
                accent = SolAccent.Blue
            )
        } else {
            SolListCard {
                state.categories.forEachIndexed { i, c ->
                    CategoryBar(c)
                    if (i < state.categories.lastIndex) SolHairlineDivider()
                }
            }
        }
        Spacer(Modifier.height(SolSpacing.lg))
        SolSectionHeaderRow(title = "Top comercianți")
        if (state.topMerchants.isEmpty()) {
            EmptyStateView(
                icon = "🏷",
                title = "Niciun comerciant evidențiat",
                subtitle = "Apare aici după ce înregistrezi câteva cheltuieli.",
                accent = SolAccent.Violet
            )
        } else {
            SolListCard {
                state.topMerchants.forEachIndexed { i, (m, amt) ->
                    Row(
                        Modifier.fillMaxWidth().padding(SolSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${i + 1}", color = SolomonColors.TextTertiary, modifier = Modifier.width(28.dp))
                        Text(m, color = SolomonColors.TextPrimary, modifier = Modifier.weight(1f), maxLines = 1)
                        Text(fmt(amt), color = SolomonColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    if (i < state.topMerchants.lastIndex) SolHairlineDivider()
                }
            }
        }
        Spacer(Modifier.height(SolSpacing.lg))
        SolSectionHeaderRow(title = "Instrumente Solomon")
        ToolTile("⚠", "Alertă spirală", "Verifică presiunea financiară", SolAccent.Rose) { showSpiral = true }
        Spacer(Modifier.height(SolSpacing.sm))
        ToolTile("🪒", "Audit abonamente", "Găsește abonamente nefolosite", SolAccent.Amber) { showAudit = true }
        Spacer(Modifier.height(SolSpacing.sm))
        ToolTile("🔍", "Tranzacții suspecte", "Sume mari, burst-uri, nocturn", SolAccent.Amber) { showSuspicious = true }
        Spacer(Modifier.height(SolSpacing.sm))
        ToolTile("◎", "Obiective", "Progres + proiecție per obiectiv", SolAccent.Mint) { showGoals = true }
        Spacer(Modifier.height(SolSpacing.sm))
        ToolTile("✉", "Importă email", "Parsează Glovo/Netflix/Enel din text", SolAccent.Mint) { showEmailParser = true }
        Spacer(Modifier.height(SolSpacing.sm))
        ToolTile("⚙", "Model AI", "Alege model lingvistic · Mistral cloud (UE)", SolAccent.Mint) { showModelDownload = true }
        Spacer(Modifier.height(SolSpacing.xl))
    }
}

@Composable
private fun ToolTile(icon: String, title: String, subtitle: String, accent: SolAccent, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SolRadius.lg))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(SolRadius.lg))
            .clickable(onClick = onClick)
            .padding(SolSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accent.color.copy(alpha = 0.15f))
                .border(1.dp, accent.color.copy(alpha = 0.25f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, color = accent.color, fontSize = 16.sp)
        }
        Spacer(Modifier.width(SolSpacing.md))
        Column(Modifier.weight(1f)) {
            Text(title, color = SolomonColors.TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = SolomonColors.TextTertiary, style = MaterialTheme.typography.bodySmall)
        }
        Text("›", color = SolomonColors.TextTertiary, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun CategoryBar(c: AnalysisViewModel.CategoryStat) {
    Column(Modifier.fillMaxWidth().padding(horizontal = SolSpacing.base, vertical = SolSpacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.category.displayNameRO, color = SolomonColors.TextPrimary, modifier = Modifier.weight(1f))
            Text(RomanianMoneyFormatter.format(c.amount.amount, RomanianMoneyFormatter.Style.bareNumber) + " RON", color = SolomonColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(6.dp))
        SolLinearProgress(progress = c.percent.coerceIn(0f, 1f), accent = SolAccent.Mint, height = 8, modifier = Modifier.fillMaxWidth())
    }
}
