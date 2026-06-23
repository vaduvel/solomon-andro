package ro.solomon.app.ui.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ro.solomon.app.ui.components.MeshBackground
import ro.solomon.app.ui.components.SolInsightCard
import ro.solomon.app.ui.components.SolPill
import ro.solomon.app.ui.components.SolPrimaryButton
import ro.solomon.app.ui.theme.SolAccent
import ro.solomon.app.ui.theme.SolSpacing
import ro.solomon.app.ui.theme.SolomonColors
import ro.solomon.core.domain.ObligationKind

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ObligationEditScreen(
    obligationId: String?,
    onClose: () -> Unit,
    vm: ObligationEditViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ObligationEditViewModel(obligationId) }
        }
    )
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved, state.isDeleted) {
        if (state.isSaved || state.isDeleted) onClose()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MeshBackground(
            topLeftAccent = SolAccent.Amber,
            midRightAccent = SolAccent.Rose,
            bottomLeftAccent = SolAccent.Violet,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(SolSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(SolSpacing.base)
        ) {
            WalletEditHeader(
                title = if (state.isNew) "Obligație nouă" else "Editează obligația",
                canDelete = !state.isNew,
                onBack = onClose,
                onDelete = { showDeleteConfirm = true }
            )

            WalletField(
                value = state.name,
                onValueChange = vm::onName,
                label = "Nume",
                placeholder = "ex: Chirie, rată bancă"
            )
            WalletField(
                value = state.amountText,
                onValueChange = vm::onAmount,
                label = "Sumă lunară",
                placeholder = "0",
                keyboardType = KeyboardType.Number,
                suffix = "RON"
            )
            WalletField(
                value = if (state.dayOfMonth > 0) state.dayOfMonth.toString() else "",
                onValueChange = { vm.onDay(it.filter { c -> c.isDigit() }.toIntOrNull() ?: 1) },
                label = "Ziua scadenței (1-31)",
                placeholder = "1",
                keyboardType = KeyboardType.Number
            )

            Text("TIP", color = SolomonColors.TextTertiary, fontSize = 11.sp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(SolSpacing.sm)
            ) {
                ObligationKind.values().forEach { k ->
                    SolPill(label = k.displayNameRO, isActive = state.kind == k, onClick = { vm.onKind(k) })
                }
            }

            val amt = state.amountText.toIntOrNull() ?: 0
            if (amt > 0) {
                SolInsightCard(label = "Impact anual", accent = SolAccent.Amber) {
                    Text(
                        "$amt RON pe lună înseamnă ${amt * 12} RON pe an. Scadență în ziua ${state.dayOfMonth} a fiecărei luni.",
                        color = SolomonColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            state.error?.let {
                Text("Eroare: $it", color = SolomonColors.Error, fontSize = 13.sp)
            }

            Spacer(Modifier.height(SolSpacing.xs))
            SolPrimaryButton(
                title = if (state.isNew) "Adaugă obligația" else "Salvează",
                accent = SolAccent.Amber,
                fullWidth = true,
                onClick = { vm.save() }
            )
            Spacer(Modifier.height(SolSpacing.xxl))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Ștergi obligația?") },
            text = { Text("Această acțiune nu poate fi anulată.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; vm.delete() }) {
                    Text("Șterge", color = SolomonColors.Rose)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Anulează") }
            }
        )
    }
}
