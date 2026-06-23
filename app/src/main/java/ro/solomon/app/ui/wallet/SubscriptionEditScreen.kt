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
import ro.solomon.core.domain.CancellationDifficulty

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubscriptionEditScreen(
    subscriptionId: String?,
    onClose: () -> Unit,
    vm: SubscriptionEditViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SubscriptionEditViewModel(subscriptionId) }
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
            topLeftAccent = SolAccent.Violet,
            midRightAccent = SolAccent.Blue,
            bottomLeftAccent = SolAccent.Rose,
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
                title = if (state.isNew) "Abonament nou" else "Editează abonamentul",
                canDelete = !state.isNew,
                onBack = onClose,
                onDelete = { showDeleteConfirm = true }
            )

            WalletField(
                value = state.name,
                onValueChange = vm::onName,
                label = "Nume",
                placeholder = "ex: Netflix, Spotify, Sala"
            )
            WalletField(
                value = state.amountText,
                onValueChange = vm::onAmount,
                label = "Cost lunar",
                placeholder = "0",
                keyboardType = KeyboardType.Number,
                suffix = "RON"
            )
            WalletField(
                value = state.lastUsedDaysAgo,
                onValueChange = vm::onLastUsed,
                label = "Folosit ultima dată (zile în urmă)",
                placeholder = "ex: 14",
                keyboardType = KeyboardType.Number
            )

            Text("CÂT DE GREU E DE ANULAT", color = SolomonColors.TextTertiary, fontSize = 11.sp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(SolSpacing.sm)
            ) {
                CancellationDifficulty.values().forEach { d ->
                    SolPill(label = d.displayNameRO, isActive = state.difficulty == d, onClick = { vm.onDifficulty(d) })
                }
            }

            WalletField(
                value = state.cancellationUrl,
                onValueChange = vm::onUrl,
                label = "Link de anulare (opțional)",
                placeholder = "https://…",
                keyboardType = KeyboardType.Uri
            )
            WalletField(
                value = state.notes,
                onValueChange = vm::onNotes,
                label = "Pași de anulare / note (opțional)",
                placeholder = "ex: Cont → Abonament → Anulează",
                singleLine = false
            )

            val amt = state.amountText.toIntOrNull() ?: 0
            val daysUnused = state.lastUsedDaysAgo.toIntOrNull()
            if (amt > 0) {
                val ghost = daysUnused != null && daysUnused > 30
                SolInsightCard(
                    label = if (ghost) "Posibil abonament-fantomă" else "Cost anual",
                    accent = if (ghost) SolAccent.Rose else SolAccent.Blue
                ) {
                    Text(
                        buildString {
                            append("Te costă ${amt * 12} RON pe an.")
                            if (ghost) append(" Nu l-ai folosit de $daysUnused zile — poate merită anulat.")
                        },
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
                title = if (state.isNew) "Adaugă abonamentul" else "Salvează",
                accent = SolAccent.Violet,
                fullWidth = true,
                onClick = { vm.save() }
            )
            Spacer(Modifier.height(SolSpacing.xxl))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Ștergi abonamentul?") },
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
