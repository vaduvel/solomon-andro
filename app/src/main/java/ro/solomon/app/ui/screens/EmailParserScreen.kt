package ro.solomon.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.ui.components.*
import ro.solomon.app.ui.theme.*
import ro.solomon.core.domain.Transaction
import ro.solomon.core.domain.TransactionSource
import ro.solomon.core.format.RomanianMoneyFormatter
import ro.solomon.email.EmailMessage
import ro.solomon.email.EmailTransactionParser
import ro.solomon.email.ParsedEmailTransaction

class EmailParserViewModel : ViewModel() {
    data class State(
        val from: String = "",
        val subject: String = "",
        val body: String = "",
        val parsed: ParsedEmailTransaction? = null,
        val saved: Boolean = false,
        val error: String? = null
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private val parser = EmailTransactionParser()

    fun update(transform: (State) -> State) { _state.value = transform(_state.value) }

    fun parse() {
        val s = _state.value
        if (s.from.isBlank() || s.subject.isBlank() || s.body.isBlank()) return
        val msg = EmailMessage(from = s.from, subject = s.subject, bodyText = s.body)
        val parsed = parser.parse(msg)
        _state.value = s.copy(parsed = parsed, error = null, saved = false)
        if (parsed == null) {
            _state.value = _state.value.copy(error = "Emailul nu pare financiar sau nu am putut extrage date.")
        }
    }

    fun save() {
        val parsed = _state.value.parsed ?: return
        val ron = parsed.amount?.moneyRON
        if (ron == null) {
            _state.value = _state.value.copy(error = "Suma nu a putut fi extrasă sau e în altă monedă (doar RON e suportat).")
            return
        }
        viewModelScope.launch {
            val id = "email-${System.currentTimeMillis()}"
            val tx = Transaction(
                id = id,
                // dateEpochSeconds is email metadata in SECONDS; Transaction.date is canonical MILLIS.
                date = parsed.dateEpochSeconds * 1000L,
                amount = ron,
                direction = parsed.direction,
                category = parsed.suggestedCategory,
                merchant = parsed.merchant,
                description = "[${parsed.amount?.currency ?: "RON"}] ${parsed.subject}",
                source = TransactionSource.email_parsed,
                categorizationConfidence = parsed.confidence
            )
            ServiceLocator.txnRepo.save(tx)
            _state.value = _state.value.copy(saved = true, error = null)
        }
    }

    fun reset() {
        _state.value = State()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailParserScreen(onClose: () -> Unit) {
    val vm: EmailParserViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Importă email", color = SolomonColors.Primary) },
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
                state.saved -> SuccessBlock(onClose, vm::reset)
                state.parsed != null -> PreviewBlock(state.parsed!!, state.error, vm::save) { vm.update { it.copy(parsed = null) } }
                else -> InputForm(state, vm)
            }
        }
    }
}

@Composable
private fun InputForm(state: EmailParserViewModel.State, vm: EmailParserViewModel) {
    val canParse = state.from.isNotBlank() && state.subject.isNotBlank() && state.body.isNotBlank()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SolSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SolSpacing.base)
    ) {
        Text("Importă din email", color = SolomonColors.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Text("Lipește un email financiar (Glovo, Netflix, Enel, etc.) — Solomon extrage automat tranzacția.", color = SolomonColors.TextTertiary, fontSize = 13.sp)

        OutlinedTextField(
            value = state.from,
            onValueChange = { v -> vm.update { it.copy(from = v) } },
            label = { Text("De la (from)") },
            placeholder = { Text("ex: no-reply@glovoapp.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.subject,
            onValueChange = { v -> vm.update { it.copy(subject = v) } },
            label = { Text("Subiect") },
            placeholder = { Text("ex: Comanda confirmată") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.body,
            onValueChange = { v -> vm.update { it.copy(body = v) } },
            label = { Text("Conținut email") },
            placeholder = { Text("Lipește aici textul emailului…") },
            minLines = 6,
            modifier = Modifier.fillMaxWidth()
        )
        state.error?.let { Text(it, color = SolomonColors.Error, fontSize = 13.sp) }
        Button(
            onClick = { vm.parse() },
            enabled = canParse,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = SolomonColors.Primary)
        ) { Text("Parsează email", color = SolomonColors.OnPrimary) }
    }
}

@Composable
private fun PreviewBlock(
    parsed: ParsedEmailTransaction,
    error: String?,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val amount = parsed.amount
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SolSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SolSpacing.base)
    ) {
        SolHeroCard(accent = SolAccent.Mint, badge = "PARSED") {
            SolHeroLabel(parsed.merchant ?: "TRANZACȚIE")
            Spacer(Modifier.height(SolSpacing.sm))
            if (amount != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        RomanianMoneyFormatter.format(amount.value, RomanianMoneyFormatter.Style.bareNumber),
                        color = SolomonColors.Primary,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(SolSpacing.sm))
                    Text(amount.currency.name, color = SolomonColors.TextTertiary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
                }
            } else {
                Text("Sumă indisponibilă", color = SolomonColors.TextSecondary, fontSize = 16.sp)
            }
            Spacer(Modifier.height(SolSpacing.xs))
            Text(parsed.suggestedCategory.displayNameRO, color = SolomonColors.TextSecondary, fontSize = 13.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            SolChip("Confidență ${(parsed.confidence * 100).toInt()}%", accent = if (parsed.confidence >= 0.8) SolAccent.Mint else if (parsed.confidence >= 0.5) SolAccent.Amber else SolAccent.Rose)
        }

        error?.let {
            SolInsightCard(label = "Eroare", timestamp = null, accent = SolAccent.Rose) {
                Text(it, color = SolomonColors.TextSecondary, fontSize = 14.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Înapoi") }
            Button(
                onClick = onSave,
                enabled = amount?.moneyRON != null,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SolomonColors.Primary)
            ) { Text("Salvează", color = SolomonColors.OnPrimary) }
        }
    }
}

@Composable
private fun SuccessBlock(onClose: () -> Unit, onReset: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(SolSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SolSpacing.base)
    ) {
        SolHeroCard(accent = SolAccent.Mint, badge = "SALVAT ✓") {
            SolHeroLabel("Confirmare")
            Spacer(Modifier.height(SolSpacing.sm))
            Text("Tranzacția a fost adăugată.", color = SolomonColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(SolSpacing.xs))
            Text("O găsești în Analiză sau în Wallet.", color = SolomonColors.TextSecondary, fontSize = 13.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("Mai parsez unul") }
            Button(
                onClick = onClose,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SolomonColors.Primary)
            ) { Text("Gata", color = SolomonColors.OnPrimary) }
        }
    }
}
