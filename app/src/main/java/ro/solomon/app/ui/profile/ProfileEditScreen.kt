package ro.solomon.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ro.solomon.app.ui.theme.SolSpacing
import ro.solomon.core.domain.Addressing
import ro.solomon.core.domain.AgeRange
import ro.solomon.core.domain.Bank
import ro.solomon.core.domain.FinancialPersonality
import ro.solomon.core.domain.SalaryRange

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileEditScreen(
    onClose: () -> Unit,
    vm: ProfileEditViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.isSaved) { if (state.isSaved) onClose() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Înapoi")
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
            verticalArrangement = Arrangement.spacedBy(SolSpacing.lg)
        ) {
            SectionTitle("Identitate")
            OutlinedTextField(
                value = state.name,
                onValueChange = vm::onName,
                label = { Text("Nume") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Cum te adresez?")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.addressing == Addressing.tu,
                    onClick = { vm.onAddressing(Addressing.tu) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Tu") }
                SegmentedButton(
                    selected = state.addressing == Addressing.dumneavoastra,
                    onClick = { vm.onAddressing(Addressing.dumneavoastra) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Dumneavoastră") }
            }

            SectionTitle("Vârstă")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
                AgeRange.values().forEach { ar ->
                    FilterChip(
                        selected = state.ageRange == ar,
                        onClick = { vm.onAgeRange(ar) },
                        label = { Text(ar.displayNameRO) }
                    )
                }
            }

            SectionTitle("Venit")
            Text("Interval salarial")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
                SalaryRange.values().forEach { sr ->
                    FilterChip(
                        selected = state.salaryRange == sr,
                        onClick = { vm.onSalaryRange(sr) },
                        label = { Text(formatSalaryRange(sr)) }
                    )
                }
            }

            Text("Tip salariu")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.salaryType == "monthly",
                    onClick = { vm.onSalaryType("monthly") },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Lunar") }
                SegmentedButton(
                    selected = state.salaryType == "variable",
                    onClick = { vm.onSalaryType("variable") },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Variabil") }
            }

            if (state.salaryType == "monthly") {
                OutlinedTextField(
                    value = state.paydayDay.toString(),
                    onValueChange = { vm.onPaydayDay(it.toIntOrNull() ?: 0) },
                    label = { Text("Ziua lunii în care primești salariul (1-31)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (state.salaryType == "monthly")
                        "Cu salariu lunar de ${state.salaryRange.midpointRON} RON pe ${state.paydayDay}, ai un flux previzibil."
                    else
                        "Cu venit variabil în intervalul ${state.salaryRange.midpointRON} RON, Solomon îți calculează un buffer adaptiv.",
                    modifier = Modifier.padding(vertical = SolSpacing.xs)
                )
            }

            SectionTitle("Surse suplimentare")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !state.hasSecondary,
                    onClick = { vm.onHasSecondary(false) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Nu") }
                SegmentedButton(
                    selected = state.hasSecondary,
                    onClick = { vm.onHasSecondary(true) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Da") }
            }
            if (state.hasSecondary) {
                OutlinedTextField(
                    value = state.secondaryAvg,
                    onValueChange = vm::onSecondaryAvg,
                    label = { Text("Medie lunară RON") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionTitle("Banca principală")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
                Bank.values().forEach { b ->
                    FilterChip(
                        selected = state.bank == b,
                        onClick = { vm.onBank(b) },
                        label = { Text(b.displayNameRO) }
                    )
                }
            }

            SectionTitle("Personalitate financiară")
            Text("Opțional: Solomon ajustează recomandările în funcție de stilul tău.")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
                FinancialPersonality.values().forEach { p ->
                    FilterChip(
                        selected = state.personality == p,
                        onClick = {
                            vm.onPersonality(if (state.personality == p) null else p)
                        },
                        label = { Text("${p.emoji} ${p.displayNameRO}") }
                    )
                }
            }
            state.personality?.let { p ->
                Text(p.descriptionRO, modifier = Modifier.padding(vertical = SolSpacing.xs))
            }

            state.error?.let { Text("Eroare: $it", color = androidx.compose.material3.MaterialTheme.colorScheme.error) }

            Button(
                onClick = { vm.save() },
                enabled = state.canSave(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Salvează") }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = SolSpacing.sm)
    )
}

private fun formatSalaryRange(sr: SalaryRange): String = when (sr) {
    SalaryRange.under3k -> "sub 3.000"
    SalaryRange.range3to5 -> "3.000–5.000"
    SalaryRange.range5to8 -> "5.000–8.000"
    SalaryRange.range8to15 -> "8.000–15.000"
    SalaryRange.over15k -> "peste 15.000"
}
