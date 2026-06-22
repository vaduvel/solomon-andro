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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import ro.solomon.app.ui.components.SolHeroCard
import ro.solomon.app.ui.components.SolHeroLabel
import ro.solomon.app.ui.components.SolInsightCard
import ro.solomon.app.ui.components.SolListCard
import ro.solomon.app.ui.components.SolLoadingIndicator
import ro.solomon.app.ui.components.SolHairlineDivider
import ro.solomon.app.ui.components.SolSectionHeaderRow
import ro.solomon.app.ui.theme.SolAccent
import ro.solomon.app.ui.theme.SolSpacing
import ro.solomon.app.ui.theme.SolomonColors
import ro.solomon.analytics.SpiralReport
import ro.solomon.core.domain.Money
import ro.solomon.core.format.RomanianMoneyFormatter
import ro.solomon.core.moments.SpiralFactor
import ro.solomon.core.moments.SpiralFactorKind
import ro.solomon.core.moments.SpiralSeverity

class SpiralAlertViewModel : ViewModel() {
    data class State(
        val loading: Boolean = true,
        val report: SpiralReport? = null,
        val monthlyBalance: List<Money> = emptyList()
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val txns = ServiceLocator.txnRepo.fetchAll()
        val obligs = ServiceLocator.obligationRepo.fetchAll()
        val cashFlow = ServiceLocator.cashFlow.analyze(
            transactions = txns,
            referenceDate = System.currentTimeMillis()
        )
        val history = ServiceLocator.cashFlow.groupByMonth(txns, ro.solomon.core.format.RomanianDateFormatter.gregorianROCalendar())
            .map { it.balance }
        val report = ServiceLocator.spiralDetector.detect(
            transactions = txns,
            obligations = obligs,
            monthlyIncomeAvg = cashFlow.monthlyIncomeAvg,
            monthlySpendingAvg = cashFlow.monthlySpendingAvg,
            monthlyBalanceHistory = history
        )
        _state.value = State(loading = false, report = report, monthlyBalance = history)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpiralAlertScreen(onClose: () -> Unit) {
    val vm: SpiralAlertViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alertă spirală", color = SolomonColors.Rose) },
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
                state.loading -> SolLoadingIndicator(SolAccent.Rose, "Analizăm ultimele 30 de zile…")
                state.report == null || state.report!!.factors.isEmpty() -> EmptySpiralState(onClose)
                else -> SpiralContent(state.report!!, state.monthlyBalance)
            }
        }
    }
}

@Composable
private fun EmptySpiralState(onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(SolSpacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✓", color = SolomonColors.Primary, fontSize = 56.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(SolSpacing.md))
        Text("Niciun semn de spirală", color = SolomonColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(SolSpacing.sm))
        Text(
            "Solomon nu detectează presiune financiară pe baza datelor curente.",
            color = SolomonColors.TextTertiary,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(SolSpacing.lg))
        OutlinedButton(onClick = onClose) { Text("Închide") }
    }
}

@Composable
private fun SpiralContent(report: SpiralReport, history: List<Money>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SolSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SolSpacing.base)
    ) {
        HeroBlock(report, history)
        if (report.factors.isNotEmpty()) {
            SolSectionHeaderRow("Factori contribuitori", "${report.factors.size} detectate")
            FactorsList(report.factors)
        }
        if (report.csalbRelevant) CsalbCard()
    }
}

@Composable
private fun HeroBlock(report: SpiralReport, history: List<Money>) {
    SolHeroCard(accent = SolAccent.Rose, badge = "CRITIC") {
        SolHeroLabel(heroLabel(report.severity))
        Spacer(Modifier.height(SolSpacing.sm))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "SPIRAL ${report.score}",
                color = SolomonColors.Rose,
                fontSize = 38.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(SolSpacing.sm))
            Text("/4", color = SolomonColors.TextTertiary, fontSize = 16.sp, modifier = Modifier.padding(bottom = 6.dp))
        }
        Spacer(Modifier.height(SolSpacing.xs))
        Text(
            text = severityDescription(report.severity),
            color = SolomonColors.TextSecondary,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(SolSpacing.base))
        SpiralBars(history)
    }
}

@Composable
private fun SpiralBars(history: List<Money>) {
    if (history.isEmpty()) return
    val last7 = history.takeLast(7)
    val maxAmount = last7.maxOf { it.amount }.coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        last7.forEachIndexed { i, money ->
            val fraction = money.amount.toFloat() / maxAmount.toFloat()
            val isLastTwo = i >= last7.size - 2
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction.coerceIn(0.05f, 1f))
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (isLastTwo) SolomonColors.Rose.copy(alpha = 0.7f)
                        else SolomonColors.Rose.copy(alpha = 0.3f)
                    )
            )
        }
    }
}

@Composable
private fun FactorsList(factors: List<SpiralFactor>) {
    SolListCard {
        factors.forEachIndexed { i, f ->
            if (i > 0) SolHairlineDivider()
            FactorRow(f)
        }
    }
}

@Composable
private fun FactorRow(factor: SpiralFactor) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(SolSpacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SolSpacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(SolomonColors.Rose.copy(alpha = 0.18f))
                .border(1.dp, SolomonColors.Rose.copy(alpha = 0.40f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("⚠", color = SolomonColors.Rose, fontSize = 18.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(factorName(factor.factor), color = SolomonColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(factor.evidence, color = SolomonColors.TextTertiary, fontSize = 12.sp)
        }
        factor.amount?.let {
            Text("−${RomanianMoneyFormatter.format(it.amount, RomanianMoneyFormatter.Style.bareNumber)}",
                color = SolomonColors.Rose, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CsalbCard() {
    SolInsightCard(
        label = "CSALB poate ajuta",
        timestamp = null,
        accent = SolAccent.Blue
    ) {
        Text(
            "Centrul de Soluționare Alternativă a Litigiilor Bancare mediază gratuit dispute cu bănci și IFN-uri. Procesul durează 30-90 zile.",
            color = SolomonColors.TextSecondary,
            fontSize = 14.sp
        )
        Button(
            onClick = { /* open URL */ },
            colors = ButtonDefaults.buttonColors(containerColor = SolomonColors.Blue, contentColor = Color.Black)
        ) { Text("Începe procedura CSALB") }
    }
}

private fun heroLabel(severity: SpiralSeverity): String = when (severity) {
    SpiralSeverity.none, SpiralSeverity.low -> "PRESIUNE FINANCIARĂ · 7 ZILE"
    SpiralSeverity.medium -> "PRESIUNE CRESCUTĂ · 7 ZILE"
    SpiralSeverity.high, SpiralSeverity.critical -> "PESTE BUGET · 7 ZILE CONSECUTIVE"
}

private fun severityDescription(s: SpiralSeverity): String = when (s) {
    SpiralSeverity.none -> "Balanța e stabilă. Continuă să monitorizezi."
    SpiralSeverity.low -> "Mici semne de presiune — monitorizează lunar."
    SpiralSeverity.medium -> "Mai mulți factori de risc. E timpul să ajustezi."
    SpiralSeverity.high -> "Situație serioasă. Acționează acum."
    SpiralSeverity.critical -> "E momentul pentru CSALB + tăiere abbonamente nefolosite."
}

private fun factorName(kind: SpiralFactorKind): String = when (kind) {
    SpiralFactorKind.balance_declining -> "Sold în scădere"
    SpiralFactorKind.card_credit_increasing -> "Card credit în creștere"
    SpiralFactorKind.ifn_active -> "IFN activ"
    SpiralFactorKind.bnpl_stacking -> "BNPL stack"
    SpiralFactorKind.obligations_exceed_income -> "Obligații peste venit"
    SpiralFactorKind.overdraft_frequent -> "Overdraft frecvent"
}
