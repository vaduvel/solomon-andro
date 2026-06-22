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
import ro.solomon.analytics.SubscriptionAuditReport
import ro.solomon.analytics.SubscriptionAuditor
import ro.solomon.core.domain.Subscription
import ro.solomon.core.format.RomanianMoneyFormatter
class SubscriptionAuditViewModel : ViewModel() {
    data class State(val loading: Boolean = true, val report: SubscriptionAuditReport? = null)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init { viewModelScope.launch { load() } }

    private suspend fun load() {
        val subs = ServiceLocator.subRepo.fetchAll()
        val txns = ServiceLocator.txnRepo.fetchAll()
        val enriched = ro.solomon.analytics.SubscriptionUsageDetector().enrichWithUsage(subs, txns)
        val report = SubscriptionAuditor().audit(enriched)
        _state.value = State(loading = false, report = report)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionAuditScreen(onClose: () -> Unit) {
    val vm: SubscriptionAuditViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audit Abonamente", color = SolomonColors.Amber) },
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
                state.loading -> SolLoadingIndicator(SolAccent.Amber, "Scanăm ultimele 90 de zile…")
                state.report == null -> Text("Eroare", modifier = Modifier.padding(SolSpacing.lg), color = SolomonColors.TextTertiary)
                else -> Content(state.report!!, onClose)
            }
        }
    }
}

@Composable
private fun Content(report: SubscriptionAuditReport, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SolSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SolSpacing.base)
    ) {
        Hero(report)
        Insight(report)
        if (report.ghostSubscriptions.isNotEmpty()) {
            SolSectionHeaderRow("Nefolosite · anulează", "${report.ghostSubscriptions.size} detectate")
            SolListCard {
                report.ghostSubscriptions.forEachIndexed { i, s ->
                    if (i > 0) SolHairlineDivider()
                    GhostRow(s)
                }
            }
        }
        if (report.activeSubscriptions.isNotEmpty()) {
            SolSectionHeaderRow("Folosite · OK", "${report.activeSubscriptions.size} active")
            SolListCard {
                report.activeSubscriptions.forEachIndexed { i, s ->
                    if (i > 0) SolHairlineDivider()
                    ActiveRow(s)
                }
            }
        }
        if (report.ghostSubscriptions.isEmpty() && report.activeSubscriptions.isEmpty()) {
            EmptyState(onClose)
        }
    }
}

@Composable
private fun Hero(report: SubscriptionAuditReport) {
    SolHeroCard(accent = SolAccent.Amber, badge = if (report.ghostSubscriptions.isNotEmpty()) "RECUPERABIL" else null) {
        SolHeroLabel(if (report.ghostSubscriptions.isEmpty()) "ZERO GHOST · TOATE OK" else "${report.ghostSubscriptions.size} NEFOLOSITE · 90 ZILE")
        Spacer(Modifier.height(SolSpacing.sm))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = RomanianMoneyFormatter.format(report.monthlyRecoverable.amount, RomanianMoneyFormatter.Style.bareNumber),
                color = SolomonColors.Amber,
                fontSize = 38.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(SolSpacing.sm))
            Text("RON / lună", color = SolomonColors.TextTertiary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
        }
        Spacer(Modifier.height(SolSpacing.xs))
        Text("= ${report.annualRecoverable.amount} RON / an", color = SolomonColors.Amber, fontSize = 13.sp)
    }
}

@Composable
private fun Insight(report: SubscriptionAuditReport) {
    SolInsightCard(label = "Solomon · Detecție", timestamp = "scan azi", accent = SolAccent.Amber) {
        if (report.ghostSubscriptions.isEmpty()) {
            Text(
                "Nu am detectat abonamente nefolosite. Toate cele ${report.activeSubscriptions.size} sunt folosite recent.",
                color = SolomonColors.TextSecondary,
                fontSize = 14.sp
            )
        } else {
            val names = report.ghostSubscriptions.take(3).joinToString(", ") { it.name }
            val verb = if (report.ghostSubscriptions.size == 1) "n-a fost deschis" else "n-au fost deschise"
            Text(
                "$names $verb în 90+ zile. Recuperabil: ${report.monthlyRecoverable.amount} RON/lună.",
                color = SolomonColors.TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun GhostRow(sub: Subscription) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(SolSpacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SolSpacing.md)
    ) {
        BrandLogo(sub.name)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
                Text(sub.name, color = SolomonColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                SolChip(
                    label = sub.lastUsedDaysAgo?.let { "$it zile" } ?: "ghost",
                    accent = if ((sub.lastUsedDaysAgo ?: 0) >= 60) SolAccent.Rose else SolAccent.Amber
                )
            }
            Text(
                "Ultima folosire: ${sub.lastUsedDaysAgo ?: "?"} zile",
                color = SolomonColors.Rose,
                fontSize = 11.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${sub.amountMonthly.amount}", color = SolomonColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("RON / lună", color = SolomonColors.TextTertiary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ActiveRow(sub: Subscription) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(SolSpacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SolSpacing.md)
    ) {
        BrandLogo(sub.name)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
                Text(sub.name, color = SolomonColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                SolChip("activ", accent = SolAccent.Mint)
            }
            Text("Folosit recent", color = SolomonColors.TextTertiary, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${sub.amountMonthly.amount}", color = SolomonColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("RON / lună", color = SolomonColors.TextTertiary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun BrandLogo(name: String) {
    val first = name.firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(first, color = SolomonColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyState(onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(SolSpacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✓", color = SolomonColors.Primary, fontSize = 56.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(SolSpacing.md))
        Text("Niciun abonament încă", color = SolomonColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(SolSpacing.sm))
        Text(
            "Adaugă primul tău abonament și Solomon îți va arăta ce nu mai folosești.",
            color = SolomonColors.TextTertiary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(SolSpacing.lg))
        OutlinedButton(onClick = onClose) { Text("Închide") }
    }
}
