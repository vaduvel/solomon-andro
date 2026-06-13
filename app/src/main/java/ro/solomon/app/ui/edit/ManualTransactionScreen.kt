package ro.solomon.app.ui.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.ui.theme.SolSpacing
import ro.solomon.app.ui.theme.SolomonColors
import ro.solomon.core.domain.FlowDirection
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.Transaction
import ro.solomon.core.domain.TransactionCategory
import ro.solomon.core.domain.TransactionSource

class ManualTransactionViewModel : ViewModel() {
    data class State(
        val amountText: String = "",
        val amount: Int = 0,
        val merchant: String = "",
        val direction: FlowDirection = FlowDirection.outgoing,
        val category: TransactionCategory = TransactionCategory.food_grocery,
        val note: String = "",
        val saved: Boolean = false
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun setAmount(text: String) {
        val v = text.filter(Char::isDigit).toIntOrNull() ?: 0
        _state.value = _state.value.copy(amountText = text.filter(Char::isDigit), amount = v)
    }
    fun setMerchant(v: String) { _state.value = _state.value.copy(merchant = v) }
    fun setDirection(d: FlowDirection) { _state.value = _state.value.copy(direction = d) }
    fun setCategory(c: TransactionCategory) { _state.value = _state.value.copy(category = c) }
    fun setNote(n: String) { _state.value = _state.value.copy(note = n) }

    fun save() = viewModelScope.launch {
        val s = _state.value
        if (s.amount <= 0) return@launch
        val txn = Transaction(
            id = "manual-${System.currentTimeMillis()}",
            date = System.currentTimeMillis() / 1000,
            amount = Money(s.amount),
            direction = s.direction,
            category = s.category,
            merchant = s.merchant.ifBlank { null },
            description = s.note.ifBlank { null },
            source = TransactionSource.manual_entry,
            categorizationConfidence = 1.0
        )
        ServiceLocator.txnRepo.save(txn)
        _state.value = s.copy(saved = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManualTransactionScreen(
    onDismiss: () -> Unit,
    vm: ManualTransactionViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val haptics = ro.solomon.app.ui.util.rememberHaptics()

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SolomonColors.Background) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SolSpacing.base)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Inchide", tint = SolomonColors.TextPrimary) }
                Text("Adauga tranzactie", style = MaterialTheme.typography.headlineSmall, color = SolomonColors.TextPrimary)
            }
            Spacer(Modifier.height(SolSpacing.md))

            SegmentedButtons(
                options = listOf(FlowDirection.outgoing to "Cheltuiala", FlowDirection.incoming to "Venit"),
                selected = state.direction,
                onSelect = vm::setDirection
            )
            Spacer(Modifier.height(SolSpacing.md))

            OutlinedTextField(
                value = state.amountText,
                onValueChange = vm::setAmount,
                label = { Text("Suma (RON)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )
            Spacer(Modifier.height(SolSpacing.md))
            OutlinedTextField(
                value = state.merchant,
                onValueChange = vm::setMerchant,
                label = { Text("Comerciant (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )
            Spacer(Modifier.height(SolSpacing.md))
            Text("Categorie", color = SolomonColors.TextPrimary, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(SolSpacing.xs))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(SolSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(SolSpacing.xs)
            ) {
                TransactionCategory.entries.forEach { c ->
                    FilterChip(
                        selected = state.category == c,
                        onClick = { vm.setCategory(c) },
                        label = { Text(c.displayNameRO) }
                    )
                }
            }
            Spacer(Modifier.height(SolSpacing.md))
            OutlinedTextField(
                value = state.note,
                onValueChange = vm::setNote,
                label = { Text("Nota (optional)") },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )
            Spacer(Modifier.height(SolSpacing.lg))
            Button(
                onClick = { haptics.medium(); vm.save() },
                enabled = state.amount > 0,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SolomonColors.Primary, contentColor = SolomonColors.OnPrimary)
            ) {
                Text(if (state.saved) "Salvat ✓" else "Salveaza")
            }
            Spacer(Modifier.height(SolSpacing.lg))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SegmentedButtons(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.xs)) {
        options.forEach { (value, label) ->
            val isSel = value == selected
            FilterChip(
                selected = isSel,
                onClick = { onSelect(value) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SolomonColors.Primary,
                    selectedLabelColor = SolomonColors.OnPrimary
                )
            )
        }
    }
}

@ExperimentalMaterial3Api
@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SolomonColors.Primary,
    unfocusedBorderColor = SolomonColors.Outline,
    focusedTextColor = SolomonColors.TextPrimary,
    unfocusedTextColor = SolomonColors.TextPrimary,
    focusedLabelColor = SolomonColors.Primary,
    unfocusedLabelColor = SolomonColors.TextSecondary,
    cursorColor = SolomonColors.Primary
)
