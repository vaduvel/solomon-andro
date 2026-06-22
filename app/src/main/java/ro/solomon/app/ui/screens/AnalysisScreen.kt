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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import ro.solomon.app.ui.components.*
import ro.solomon.app.ui.theme.SolAccent
import ro.solomon.app.ui.theme.SolSpacing
import ro.solomon.app.ui.theme.SolomonColors
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.Transaction
import ro.solomon.core.domain.TransactionCategory
import ro.solomon.core.domain.FlowDirection
import ro.solomon.core.format.RomanianMoneyFormatter
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max

class AnalysisViewModel : ViewModel() {
    data class CategoryStat(val category: TransactionCategory, val amount: Int, val percent: Float)
    data class MonthBar(val label: String, val amount: Int, val isProjected: Boolean, val isCurrent: Boolean, val isRisk: Boolean)
    data class State(
        val monthLabel: String = "",
        val totalSpent: Int = 0,
        val totalIncome: Int = 0,
        val net: Int = 0,
        val perDay: Int = 0,
        val deltaPercent: String = "",
        val deltaIsWarning: Boolean = false,
        val categories: List<CategoryStat> = emptyList(),
        val topMerchants: List<Pair<String, Int>> = emptyList(),
        val monthlyBars: List<MonthBar> = emptyList(),
        val estimatedIncome: Int = 0,
        val obligationsMonthly: Int = 0,
        val detectionCategory: TransactionCategory? = null,
        val detectionAmount: Int = 0,
        val detectionBody: String = ""
    )

    val state: StateFlow<State> = ServiceLocator.txnRepo.observeAll()
        .map { computeState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State())

    private fun computeState(txns: List<Transaction>): State {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis
        val monthStart = cal.apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val prevCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val prevMonthStart = prevCal.timeInMillis
        val prevMonthEnd = monthStart - 1

        val recent = txns.filter { it.date >= monthStart }
        val prevRecent = txns.filter { it.date >= prevMonthStart && it.date <= prevMonthEnd }

        val spent = recent.filter { it.direction == FlowDirection.outgoing }.sumOf { it.amount.amount } / 100
        val prevSpent = prevRecent.filter { it.direction == FlowDirection.outgoing }.sumOf { it.amount.amount } / 100
        val income = recent.filter { it.direction == FlowDirection.incoming }.sumOf { it.amount.amount } / 100

        val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val perDay = if (spent > 0 && dayOfMonth > 0) spent / dayOfMonth else 0

        val deltaPct = if (prevSpent > 0) {
            val diff = ((spent - prevSpent).toDouble() / prevSpent * 100).toInt()
            "${abs(diff)}%"
        } else ""
        val deltaIsWarning = spent > prevSpent

        val grouped = recent
            .filter { it.direction == FlowDirection.outgoing }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { t -> t.amount.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(8)
        val totalForPct = max(grouped.sumOf { it.second }, 1)
        val stats = grouped.map { (cat, amt) ->
            CategoryStat(cat, amt / 100, amt.toFloat() / totalForPct)
        }

        val merchants = recent
            .filter { it.direction == FlowDirection.outgoing && !it.merchant.isNullOrBlank() }
            .groupBy { it.merchant!! }
            .mapValues { it.value.sumOf { it.amount.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { (m, amt) -> m to amt / 100 }

        val monthlyBars = buildMonthlyBars(txns)

        val obligations = kotlinx.coroutines.runBlocking { ServiceLocator.obligationRepo.fetchAll() }
        val obligationsMonthly = obligations.sumOf { it.amount.amount } / 100
        val profile = kotlinx.coroutines.runBlocking { ServiceLocator.userRepo.fetchProfile() }
        val estimatedIncome = (profile?.financials?.salaryRange?.midpointRON ?: 0) +
            (if (profile?.financials?.hasSecondaryIncome == true) (profile.financials.secondaryIncomeAvg?.amount?.let { it / 100 } ?: 0) else 0)

        val detection = stats.firstOrNull { it.percent >= 0.40f }
        val detectionCategory = detection?.category
        val detectionAmount = detection?.amount ?: 0
        val detectionBody = if (detection != null) {
            "${(detection.percent * 100).toInt()}% din cheltuielile lunii.Merit\u0103 o limit\u0103."
        } else ""

        val monthLabel = java.text.SimpleDateFormat("MMMM", java.util.Locale("ro", "RO")).format(java.util.Date())

        return State(
            monthLabel = monthLabel.replaceFirstChar { it.uppercase() },
            totalSpent = spent, totalIncome = income, net = income - spent,
            perDay = perDay, deltaPercent = deltaPct, deltaIsWarning = deltaIsWarning,
            categories = stats, topMerchants = merchants, monthlyBars = monthlyBars,
            estimatedIncome = estimatedIncome, obligationsMonthly = obligationsMonthly,
            detectionCategory = detectionCategory, detectionAmount = detectionAmount,
            detectionBody = detectionBody
        )
    }

    private fun buildMonthlyBars(txns: List<Transaction>): List<MonthBar> {
        val bars = mutableListOf<MonthBar>()
        val cal = Calendar.getInstance()
        val labels = listOf("Ian", "Feb", "Mar", "Apr", "Mai", "Iun", "Iul", "Aug", "Sep", "Oct", "Noi", "Dec")
        for (i in 6 downTo 1) {
            val c = Calendar.getInstance().apply { add(Calendar.MONTH, -i) }
            val ms = c.apply {
                set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val me = Calendar.getInstance().apply {
                add(Calendar.MONTH, -i + 1)
                set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis - 1
            val spent = txns.filter { it.date >= ms && it.date <= me && it.direction == FlowDirection.outgoing }
                .sumOf { it.amount.amount } / 100
            bars.add(MonthBar(labels[c.get(Calendar.MONTH)], spent, false, false, false))
        }
        val currentSpent = txns.filter { it.date >= cal.apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis && it.direction == FlowDirection.outgoing }.sumOf { it.amount.amount } / 100
        bars.add(MonthBar(labels[Calendar.getInstance().get(Calendar.MONTH)], currentSpent, false, true, false))
        val avgMonthly = (bars.filter { !it.isCurrent }.sumOf { it.amount } / max(1, bars.size - 1))
        for (i in 1..3) {
            val projected = avgMonthly
            val c = Calendar.getInstance().apply { add(Calendar.MONTH, i) }
            bars.add(MonthBar(labels[c.get(Calendar.MONTH)], projected, true, false, projected > avgMonthly * 1.2))
        }
        return bars
    }
}

@Composable
fun AnalysisScreen(vm: AnalysisViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedRange by remember { mutableStateOf("Lun\u0103") }
    val ranges = listOf("S\u0103pt.", "Lun\u0103", "3 luni", "An")
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
        Text("SOLOMON \u00B7 ANALIZ\u0102", color = SolomonColors.TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(2.dp))
        Text(state.monthLabel.ifEmpty { "Analiz\u0103" }, style = MaterialTheme.typography.headlineLarge, color = SolomonColors.TextPrimary)
        Spacer(Modifier.height(SolSpacing.base))

        Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
            ranges.forEach { r ->
                SolPill(label = r, isActive = selectedRange == r, onClick = { selectedRange = r })
            }
        }
        Spacer(Modifier.height(SolSpacing.md))

        SolHeroCard(accent = SolAccent.Blue, badge = "CHELTUIT") {
            SolHeroLabel("TOTAL ${state.monthLabel.uppercase()}")
            Spacer(Modifier.height(SolSpacing.sm))
            SolHeroAmount(
                amount = if (state.totalSpent > 0) formatThousands(state.totalSpent) else "\u2014",
                accent = SolAccent.Blue
            )
            Spacer(Modifier.height(SolSpacing.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                if (state.deltaPercent.isNotEmpty()) {
                    val arrow = if (state.deltaIsWarning) "\u2191" else "\u2193"
                    Text("$arrow ${state.deltaPercent} vs luna trecut\u0103",
                        color = if (state.deltaIsWarning) SolomonColors.Rose else SolomonColors.Primary,
                        fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Box(Modifier.width(1.dp).height(9.dp).background(Color.White.copy(alpha = 0.10f)))
                }
                Text("${state.perDay} RON/zi", color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
            }
            Spacer(Modifier.height(SolSpacing.md))
            BarsChart(state.monthlyBars)
        }

        val detCat = state.detectionCategory
        if (detCat != null) {
            Spacer(Modifier.height(SolSpacing.md))
            SolInsightCard(
                label = "SOLOMON \u00B7 DETEC\u021AIE",
                accent = SolAccent.Rose
            ) {
                Text(
                    "Cheltuieli pe ${detCat.displayNameRO}: ${state.detectionAmount} RON \u2014 ${state.detectionBody}",
                    color = SolomonColors.TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(Modifier.height(SolSpacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.md)) {
            SolStatCard(
                label = "VENIT",
                name = "Estimat",
                value = if (state.estimatedIncome > 0) "${formatThousands(state.estimatedIncome)} RON" else "\u2014",
                icon = Icons.Filled.AccountBalance,
                iconAccent = SolAccent.Mint,
                modifier = Modifier.weight(1f)
            )
            SolStatCard(
                label = "OBLIGA\u021AII",
                name = "Lunar",
                value = "${formatThousands(state.obligationsMonthly)} RON",
                icon = Icons.Filled.Assessment,
                iconAccent = SolAccent.Amber,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(SolSpacing.lg))
        SolSectionHeaderRow("CATEGORII \u00B7 ${state.monthLabel.uppercase()}", meta = "${state.categories.size} active")
        Spacer(Modifier.height(SolSpacing.sm))
        if (state.categories.isEmpty()) {
            Text("Nu sunt date \u00EEnc\u0103. Import\u0103 tranzac\u021Bii sau a\u0219teapt\u0103 notific\u0103rile bancare.", color = SolomonColors.TextSecondary)
        } else {
            state.categories.forEach { c ->
                CategoryBar(c)
                Spacer(Modifier.height(SolSpacing.sm))
            }
        }

        Spacer(Modifier.height(SolSpacing.lg))
        SolSectionHeaderRow("TOP COMERCIAN\u021AI")
        Spacer(Modifier.height(SolSpacing.sm))
        if (state.topMerchants.isEmpty()) {
            Text("\u00CEnc\u0103 nu avem comercian\u021Bi eviden\u021Bia\u021Bi.", color = SolomonColors.TextSecondary)
        } else {
            SolListCard {
                state.topMerchants.forEachIndexed { i, (m, amt) ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = SolSpacing.base, vertical = SolSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}.", color = SolomonColors.TextTertiary, modifier = Modifier.width(28.dp))
                        Text(m, color = SolomonColors.TextPrimary, modifier = Modifier.weight(1f))
                        Text("$amt RON", color = SolomonColors.TextSecondary, fontSize = 13.sp)
                    }
                    if (i < state.topMerchants.lastIndex) SolHairlineDivider()
                }
            }
        }

        Spacer(Modifier.height(SolSpacing.lg))
        SolSectionHeaderRow("INSTRUMENTE SOLOMON")
        Spacer(Modifier.height(SolSpacing.sm))
        ToolTile("Alert\u0103 spiral\u0103", "Verific\u0103 presiunea financiar\u0103", SolAccent.Rose) { showSpiral = true }
        Spacer(Modifier.height(SolSpacing.sm))
        ToolTile("Audit abonamente", "G\u0103se\u0219te abonamente nefolosite", SolAccent.Amber) { showAudit = true }
        Spacer(Modifier.height(SolSpacing.sm))
        ToolTile("Tranzac\u021Bii suspecte", "Sume mari, burst-uri, nocturn", SolAccent.Amber) { showSuspicious = true }
        Spacer(Modifier.height(SolSpacing.sm))
        ToolTile("Obiective", "Progres + proiec\u021Bie per obiectiv", SolAccent.Mint) { showGoals = true }
        Spacer(Modifier.height(SolSpacing.sm))
        ToolTile("Import\u0103 email", "Parseaz\u0103 Glovo/Netflix/Enel din text", SolAccent.Mint) { showEmailParser = true }
        Spacer(Modifier.height(SolSpacing.sm))
        ToolTile("Model AI", "Alege model lingvistic \u00B7 Mistral / on-device", SolAccent.Mint) { showModelDownload = true }
        Spacer(Modifier.height(SolSpacing.xl))
    }
}

@Composable
private fun BarsChart(bars: List<AnalysisViewModel.MonthBar>) {
    if (bars.isEmpty()) return
    val maxAmt = bars.maxOf { it.amount }.coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { m ->
            val pct = (m.amount.toFloat() / maxAmt).coerceIn(0.15f, 1f)
            val color = when {
                m.isRisk -> SolomonColors.Rose
                m.isCurrent -> SolomonColors.Blue
                m.isProjected -> SolomonColors.Blue.copy(alpha = 0.35f)
                else -> SolomonColors.Blue.copy(alpha = 0.55f)
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((120 * pct).dp.coerceAtLeast(20.dp))
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(color, color.copy(alpha = 0.15f))
                            )
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (m.isProjected) "${m.label}\u2192" else m.label,
                    color = if (m.isProjected) SolomonColors.Blue.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.35f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun ToolTile(title: String, subtitle: String, accent: SolAccent, onClick: () -> Unit) {
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
                    .background(accent.color.copy(alpha = 0.18f))
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(title.take(1), color = accent.color, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.width(SolSpacing.md))
            Column(Modifier.weight(1f)) {
                Text(title, color = SolomonColors.TextPrimary, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, color = SolomonColors.TextTertiary, style = MaterialTheme.typography.bodySmall)
            }
            Text("\u203A", color = SolomonColors.TextTertiary, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun CategoryBar(c: AnalysisViewModel.CategoryStat) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.category.displayNameRO, color = SolomonColors.TextPrimary, modifier = Modifier.weight(1f), fontSize = 14.sp)
            Text("${c.amount} RON", color = SolomonColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.06f))
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

private fun formatThousands(v: Int): String {
    return java.text.DecimalFormat("#,###", java.text.DecimalFormatSymbols(java.util.Locale("ro", "RO"))).format(v)
}
