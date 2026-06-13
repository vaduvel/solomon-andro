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
import ro.solomon.analytics.Suspicion
import ro.solomon.analytics.SuspiciousTransactionDetector
import ro.solomon.core.domain.Transaction
import ro.solomon.core.format.RomanianMoneyFormatter

class SuspiciousTxViewModel : ViewModel() {
    data class Pair(val suspicion: Suspicion, val transaction: Transaction)
    data class State(val loading: Boolean = true, val pairs: List<Pair> = emptyList())

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init { viewModelScope.launch { load() } }

    private suspend fun load() {
        val txns = ServiceLocator.txnRepo.fetchAll()
        val detector = SuspiciousTransactionDetector()
        val suspicions = detector.detect(txns)
        val byId = txns.associateBy { it.id }
        val pairs = suspicions.mapNotNull { s -> byId[s.transactionId]?.let { Pair(s, it) } }
        _state.value = State(loading = false, pairs = pairs)
    }

    fun confirmAll() {
        viewModelScope.launch {
            val txRepo = ServiceLocator.txnRepo
            state.value.pairs.forEach { txRepo.delete(it.transaction.id) }
            _state.value = State(loading = false, pairs = emptyList())
        }
    }

    fun confirm(pair: Pair) {
        viewModelScope.launch {
            ServiceLocator.txnRepo.delete(pair.transaction.id)
            _state.value = _state.value.copy(pairs = _state.value.pairs.filter { it.transaction.id != pair.transaction.id })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuspiciousTransactionsScreen(onClose: () -> Unit) {
    val vm: SuspiciousTxViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tranzacții suspecte", color = SolomonColors.Amber) },
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
                state.loading -> SolLoadingIndicator(SolAccent.Amber, "Verificăm ultimele 30 de zile…")
                state.pairs.isEmpty() -> EmptyState(onClose)
                else -> Content(state.pairs, vm)
            }
        }
    }
}

@Composable
private fun Content(pairs: List<SuspiciousTxViewModel.Pair>, vm: SuspiciousTxViewModel) {
    val total = pairs.sumOf { it.transaction.amount.amount }
    val highCount = pairs.count { it.suspicion.severity == Suspicion.Severity.high }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SolSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SolSpacing.base)
    ) {
        SolHeroCard(accent = SolAccent.Amber, badge = "ATENȚIE") {
            SolHeroLabel("${pairs.size} DETECTATE · ULTIMELE 30 ZILE")
            Spacer(Modifier.height(SolSpacing.sm))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = RomanianMoneyFormatter.format(total, RomanianMoneyFormatter.Style.bareNumber),
                    color = SolomonColors.Amber,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(SolSpacing.sm))
                Text("RON SUSPECȚI", color = SolomonColors.TextTertiary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
            }
            Spacer(Modifier.height(SolSpacing.xs))
            Text(
                if (highCount > 0) "$highCount urgente · revizuiește acum"
                else "soft ping — verificare ușoară",
                color = SolomonColors.Amber,
                fontSize = 13.sp
            )
        }

        SolInsightCard(label = "De ce le-am marcat", timestamp = "scan azi", accent = SolAccent.Amber) {
            val triggers = pairs.map { it.suspicion.trigger }.toSet()
            val parts = buildList {
                if (Suspicion.Trigger.large_amount_vs_average in triggers) add("sumă peste tipar")
                if (Suspicion.Trigger.unusual_night_merchant in triggers) add("merchant nou la noapte")
                if (Suspicion.Trigger.burst_activity in triggers) add("burst de tranzacții")
            }
            val frag = if (parts.isEmpty()) "tipar neobișnuit" else parts.joinToString(", ")
            Text(
                "Solomon a detectat $frag în ultimele 30 zile. Tu ești cel care le-ai făcut?",
                color = SolomonColors.TextSecondary,
                fontSize = 14.sp
            )
        }

        SolSectionHeaderRow("Detaliate", "${pairs.size} tranzacții")
        SolListCard {
            pairs.forEachIndexed { i, p ->
                if (i > 0) SolHairlineDivider()
                Row(p, onConfirm = { vm.confirm(p) })
            }
        }

        Button(
            onClick = { vm.confirmAll() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = SolomonColors.Amber)
        ) { Text("Confirm tot ca normal", color = androidx.compose.ui.graphics.Color.Black) }
    }
}

@Composable
private fun Row(pair: SuspiciousTxViewModel.Pair, onConfirm: () -> Unit) {
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
            Text("!", color = SolomonColors.Rose, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
                Text(
                    pair.transaction.merchant ?: pair.transaction.category.displayNameRO,
                    color = SolomonColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                SolChip(
                    label = triggerLabel(pair.suspicion.trigger),
                    accent = if (pair.suspicion.severity == Suspicion.Severity.high) SolAccent.Rose else SolAccent.Amber
                )
            }
            Text(
                pair.suspicion.evidenceText,
                color = SolomonColors.TextTertiary,
                fontSize = 11.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("−${pair.transaction.amount.amount}", color = SolomonColors.Rose, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("RON", color = SolomonColors.TextTertiary, fontSize = 10.sp)
        }
        Spacer(Modifier.width(SolSpacing.xs))
        TextButton(onClick = onConfirm) { Text("OK", color = SolomonColors.Primary) }
    }
}

private fun triggerLabel(t: Suspicion.Trigger): String = when (t) {
    Suspicion.Trigger.large_amount_vs_average -> "Sumă mare"
    Suspicion.Trigger.unusual_night_merchant -> "Merchant nou"
    Suspicion.Trigger.burst_activity -> "Burst"
}

@Composable
private fun EmptyState(onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(SolSpacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✓", color = SolomonColors.Primary, fontSize = 56.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(SolSpacing.md))
        Text("Totul pare normal", color = SolomonColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(SolSpacing.sm))
        Text(
            "Solomon nu a detectat nicio tranzacție suspectă în ultimele 30 de zile.",
            color = SolomonColors.TextTertiary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(SolSpacing.lg))
        OutlinedButton(onClick = onClose) { Text("Închide") }
    }
}
