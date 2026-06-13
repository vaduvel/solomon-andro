package ro.solomon.app.ui.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ro.solomon.app.ui.theme.SolSpacing
import ro.solomon.core.domain.ObligationKind

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    LaunchedEffect(state.isSaved, state.isDeleted) {
        if (state.isSaved || state.isDeleted) onClose()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "Obligație nouă" else "Editează obligație") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Înapoi")
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { vm.delete() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Șterge")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(SolSpacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(SolSpacing.md)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = vm::onName,
                label = { Text("Nume (ex: Chirie, Enel, Vodafone)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.amountText,
                onValueChange = vm::onAmount,
                label = { Text("Sumă lunară (RON)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.dayOfMonth.toString(),
                onValueChange = { vm.onDay(it.toIntOrNull() ?: 0) },
                label = { Text("Ziua lunii (1-31)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Tip", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
                ObligationKind.values().forEach { k ->
                    FilterChip(
                        selected = state.kind == k,
                        onClick = { vm.onKind(k) },
                        label = { Text(k.displayNameRO) }
                    )
                }
            }

            state.error?.let { Text("Eroare: $it", color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = { vm.save() },
                enabled = state.canSave(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Salvează") }
        }
    }
}
