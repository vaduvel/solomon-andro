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
import ro.solomon.core.domain.GoalKind

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalEditScreen(
    goalId: String?,
    onClose: () -> Unit,
    vm: GoalEditViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GoalEditViewModel(goalId) }
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
            topLeftAccent = SolAccent.Mint,
            midRightAccent = SolAccent.Blue,
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
                title = if (state.isNew) "Obiectiv nou" else "Editează obiectiv",
                canDelete = !state.isNew,
                onBack = onClose,
                onDelete = { showDeleteConfirm = true }
            )

            Text("TIP OBIECTIV", color = SolomonColors.TextTertiary, fontSize = 11.sp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(SolSpacing.sm)
            ) {
                GoalKind.values().forEach { k ->
                    SolPill(label = k.displayNameRO, isActive = state.kind == k, onClick = { vm.onKind(k) })
                }
            }

            WalletField(
                value = state.destination,
                onValueChange = vm::onDestination,
                label = "Destinație / detalii (opțional)",
                placeholder = "ex: Vacanță în Grecia"
            )
            WalletField(
                value = state.amountTargetText,
                onValueChange = vm::onTarget,
                label = "Sumă țintă",
                placeholder = "0",
                keyboardType = KeyboardType.Number,
                suffix = "RON"
            )
            WalletField(
                value = state.amountSavedText,
                onValueChange = vm::onSaved,
                label = "Sumă economisită deja",
                placeholder = "0",
                keyboardType = KeyboardType.Number,
                suffix = "RON"
            )

            Text("TERMEN: ${state.deadlineLabel}", color = SolomonColors.TextTertiary, fontSize = 11.sp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(SolSpacing.sm)
            ) {
                listOf(1 to "1 lună", 3 to "3 luni", 6 to "6 luni", 12 to "1 an", 24 to "2 ani").forEach { (m, l) ->
                    SolPill(label = l, isActive = state.deadlineLabel == l, onClick = { vm.onDeadlinePreset(m, l) })
                }
            }

            val target = state.amountTargetText.toIntOrNull() ?: 0
            if (target > 0) {
                val saved = state.amountSavedText.toIntOrNull() ?: 0
                val months = (((state.deadlineEpochMillis - System.currentTimeMillis()) / (30L * 86_400_000L)).toInt()).coerceAtLeast(1)
                val monthly = if (target > saved) (target - saved) / months else 0
                SolInsightCard(label = "Solomon · Proiecție", accent = SolAccent.Mint) {
                    Text(
                        if (monthly > 0)
                            "Pentru a strânge $target RON până în ${state.deadlineLabel}, pune deoparte ~$monthly RON pe lună."
                        else
                            "Ai atins deja suma țintă. Poți marca obiectivul ca finalizat.",
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
                title = if (state.isNew) "Adaugă obiectiv" else "Salvează",
                accent = SolAccent.Mint,
                fullWidth = true,
                onClick = { vm.save() }
            )
            Spacer(Modifier.height(SolSpacing.xxl))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Ștergi obiectivul?") },
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
