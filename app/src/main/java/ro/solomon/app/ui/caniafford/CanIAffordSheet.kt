package ro.solomon.app.ui.caniafford

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ro.solomon.app.ui.theme.SolRadius
import ro.solomon.app.ui.theme.SolSpacing
import ro.solomon.app.ui.theme.SolomonColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CanIAffordSheet(
    onDismiss: () -> Unit,
    vm: CanIAffordViewModel = viewModel()
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
                IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Înapoi", tint = SolomonColors.TextPrimary) }
                Text("Pot să-mi permit?", style = MaterialTheme.typography.headlineSmall, color = SolomonColors.TextPrimary)
            }
            Spacer(Modifier.height(SolSpacing.md))
            Text("Cât vrei să cheltui și pe ce?", color = SolomonColors.TextSecondary)
            Spacer(Modifier.height(SolSpacing.md))
            OutlinedTextField(
                value = state.amountText,
                onValueChange = vm::setAmount,
                label = { Text("Sumă (RON)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                ro.solomon.core.domain.TransactionCategory.entries.forEach { c ->
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
                label = { Text("Notă opțională (de ce?)") },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )
            Spacer(Modifier.height(SolSpacing.lg))
            Button(
                onClick = { haptics.medium(); vm.evaluate() },
                enabled = state.amount > 0,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SolomonColors.Primary, contentColor = SolomonColors.OnPrimary)
            ) {
                Text("Verifică")
            }
            Spacer(Modifier.height(SolSpacing.lg))
            state.verdict?.let { v ->
                val accentColor = if (v.canAfford) SolomonColors.Incoming else SolomonColors.Outgoing
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(SolRadius.lg))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(SolRadius.lg))
                        .padding(SolSpacing.lg)
                ) {
                    Text(v.title, style = MaterialTheme.typography.titleLarge, color = accentColor)
                    Spacer(Modifier.height(SolSpacing.sm))
                    Text(v.message, color = SolomonColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
                }
            }
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
