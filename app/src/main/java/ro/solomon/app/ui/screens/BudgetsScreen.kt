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
import androidx.compose.ui.text.style.TextAlign
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
import ro.solomon.analytics.BudgetReport
import ro.solomon.analytics.BudgetStatusLevel
import ro.solomon.analytics.CategoryBudgetStatus
import ro.solomon.app.services.BudgetCoach
import ro.solomon.app.services.CategoryLimitsStore
import ro.solomon.app.ui.components.*
import ro.solomon.app.ui.theme.*
import ro.solomon.core.domain.TransactionCategory
import ro.solomon.core.format.RomanianMoneyFormatter

class BudgetsViewModel : ViewModel() {
    data class State(
        val loading: Boolean = true,
        val report: BudgetReport? = null,
        val limits: Map<TransactionCategory, Int> = emptyMap(),
        val suggesting: Boolean = false
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            reload()
            _state.value = _state.value.copy(loading = false)
        }
    }

    fun setLimit(cat: TransactionCategory, ron: Int) {
        viewModelScope.launch {
            if (ron <= 0) CategoryLimitsStore.remove(cat) else CategoryLimitsStore.setLimit(cat, ron)
            reload()
        }
    }

    fun removeLimit(cat: TransactionCategory) {
        viewModelScope.launch {
            CategoryLimitsStore.remove(cat)
            reload()
        }
    }

    fun applySuggestions() {
        viewModelScope.launch {
            _state.value = _state.value.copy(suggesting = true)
            val suggestions = BudgetCoach.suggestBudgetsRON()
            suggestions.forEach { (cat, ron) ->
                if (ron > 0 && (CategoryLimitsStore.limitFor(cat) ?: 0) <= 0) {
                    CategoryLimitsStore.setLimit(cat, ron)
                }
            }
            reload()
            _state.value = _state.value.copy(suggesting = false)
        }
    }

    private suspend fun reload() {
        val report = BudgetCoach.report()
        val limits = CategoryLimitsStore.limits()
        _state.value = _state.value.copy(report = report, limits = limits)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(onClose: (() -> Unit)? = null) {
    val vm: BudgetsViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<TransactionCategory?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bugete", color = SolomonColors.Primary) },
                navigationIcon = {
                    if (onClose != null) {
                        IconButton(onClick = onClose) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "\u00CEnapoi",
                                tint = SolomonColors.TextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SolomonColors.Background)
            )
        },
        containerColor = SolomonColors.Background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> SolLoadingIndicator(SolAccent.Mint, "Calculez bugetele pe ciclul de salariu\u2026")
                state.report == null -> Text(
                    "Eroare la calcul",
                    modifier = Modifier.padding(SolSpacing.lg),
                    color = SolomonColors.TextTertiary
                )
                else -> Content(
                    report = state.report!!,
                    limits = state.limits,
                    suggesting = state.suggesting,
                    onEdit = { editing = it },
                    onSuggest = { vm.applySuggestions() }
                )
            }
        }
    }

    val editingCat = editing
    if (editingCat != null) {
        var text by remember(editingCat) {
            mutableStateOf((state.limits[editingCat]?.takeIf { it > 0 })?.toString() ?: "")
        }
        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = SolomonColors.SurfaceElevated,
            title = { Text("Buget \u00B7 ${editingCat.displayNameRO}", color = SolomonColors.TextPrimary) },
            text = {
                Column {
                    Text(
                        "C\u00E2t vrei s\u0103 cheltui pe ${editingCat.displayNameRO} \u00EEntr-un ciclu de salariu? Solomon urm\u0103re\u015Fte \u015Fi te avertizeaz\u0103 din timp.",
                        color = SolomonColors.TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(SolSpacing.sm))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { v -> text = v.filter { c -> c.isDigit() } },
                        label = { Text("RON / ciclu") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setLimit(editingCat, text.toIntOrNull() ?: 0)
                    editing = null
                }) { Text("Salveaz\u0103", color = SolomonColors.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) {
                    Text("Renun\u021B\u0103", color = SolomonColors.TextTertiary)
                }
            }
        )
    }
}

@Composable
private fun Content(
    report: BudgetReport,
    limits: Map<TransactionCategory, Int>,
    suggesting: Boolean,
    onEdit: (TransactionCategory) -> Unit,
    onSuggest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SolSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SolSpacing.base)
    ) {
        Hero(report)
        Insight(report)

        if (report.hasAnyBudget) {
            SolSectionHeaderRow("Pe categorie", "${report.statuses.size} bugete")
            report.statuses.forEach { status ->
                BudgetStatusCard(status, onEdit = { onEdit(status.category) })
            }
        }

        val unbudgeted = TransactionCategory.values().filter { (limits[it] ?: 0) <= 0 }
        if (unbudgeted.isNotEmpty()) {
            SolSectionHeaderRow(
                if (report.hasAnyBudget) "Adaug\u0103 buget" else "\u00CEncepe cu un buget",
                "${unbudgeted.size} f\u0103r\u0103 buget"
            )
            SolPrimaryButton(
                title = if (suggesting) "Calculez\u2026" else "Sugereaz\u0103 din istoric (90 zile)",
                accent = SolAccent.Mint,
                fullWidth = true,
                onClick = { if (!suggesting) onSuggest() }
            )
            Spacer(Modifier.height(SolSpacing.xs))
            SolListCard {
                unbudgeted.forEachIndexed { i, cat ->
                    if (i > 0) SolHairlineDivider()
                    AddBudgetRow(cat, onEdit = { onEdit(cat) })
                }
            }
        }
    }
}

@Composable
private fun Hero(report: BudgetReport) {
    val accent = reportAccent(report)
    val badge = when {
        report.overCount > 0 -> "${report.overCount} DEP\u0102\u015EITE"
        report.projectedOverCount > 0 -> "${report.projectedOverCount} RISC"
        else -> null
    }
    SolHeroCard(accent = accent, badge = badge) {
        SolHeroLabel("CICLU SALARIU \u00B7 ZIUA ${report.daysElapsed} DIN ${report.daysTotal}")
        Spacer(Modifier.height(SolSpacing.sm))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = RomanianMoneyFormatter.format(report.totalSpent.amount, RomanianMoneyFormatter.Style.bareNumber),
                color = accent.color,
                fontSize = 38.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(SolSpacing.sm))
            Text(
                "din ${RomanianMoneyFormatter.format(report.totalBudget.amount, RomanianMoneyFormatter.Style.bareNumber)} RON",
                color = SolomonColors.TextTertiary,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Spacer(Modifier.height(SolSpacing.xs))
        Text(
            "Proiec\u021Bie sf\u00E2r\u015Fit ciclu: ${RomanianMoneyFormatter.format(report.totalProjected.amount, RomanianMoneyFormatter.Style.bareNumber)} RON \u00B7 ${report.daysRemaining} zile r\u0103mase",
            color = SolomonColors.TextSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun Insight(report: BudgetReport) {
    SolInsightCard(label = "Solomon \u00B7 Buget", timestamp = "acum", accent = reportAccent(report)) {
        Text(
            solomonInsight(report),
            color = SolomonColors.TextSecondary,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun BudgetStatusCard(status: CategoryBudgetStatus, onEdit: () -> Unit) {
    val accent = levelAccent(status.level)
    SolListCard {
        Column(Modifier.padding(SolSpacing.base)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
                Text(status.category.displayNameRO, color = SolomonColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                SolChip(levelLabel(status.level), accent = accent)
            }
            Spacer(Modifier.height(SolSpacing.sm))
            SolLinearProgress(progress = status.pctUsed.toFloat(), accent = accent, height = 8)
            Spacer(Modifier.height(SolSpacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${RomanianMoneyFormatter.format(status.spent.amount, RomanianMoneyFormatter.Style.bareNumber)} / ${RomanianMoneyFormatter.format(status.limit.amount, RomanianMoneyFormatter.Style.bareNumber)} RON",
                    color = SolomonColors.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Text("${(status.pctUsed * 100).toInt()}%", color = accent.color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(SolSpacing.xs))
            Text(
                projectionLine(status),
                color = SolomonColors.TextTertiary,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(SolSpacing.xs))
            TextButton(onClick = onEdit, contentPadding = PaddingValues(0.dp)) {
                Text("Modific\u0103", color = SolomonColors.Primary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AddBudgetRow(cat: TransactionCategory, onEdit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(SolSpacing.base),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(cat.displayNameRO, color = SolomonColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        SolSecondaryButton(title = "Seteaz\u0103", onClick = onEdit)
    }
}

private fun reportAccent(report: BudgetReport): SolAccent = when {
    report.overCount > 0 -> SolAccent.Error
    report.projectedOverCount > 0 -> SolAccent.Warning
    report.warningCount > 0 -> SolAccent.Amber
    else -> SolAccent.Mint
}

private fun levelAccent(level: BudgetStatusLevel): SolAccent = when (level) {
    BudgetStatusLevel.over -> SolAccent.Error
    BudgetStatusLevel.projected_over -> SolAccent.Warning
    BudgetStatusLevel.warning -> SolAccent.Amber
    BudgetStatusLevel.on_track -> SolAccent.Mint
}

private fun levelLabel(level: BudgetStatusLevel): String = when (level) {
    BudgetStatusLevel.over -> "DEP\u0102\u015EIT"
    BudgetStatusLevel.projected_over -> "PROIECTAT PESTE"
    BudgetStatusLevel.warning -> "ATEN\u021AIE"
    BudgetStatusLevel.on_track -> "OK"
}

private fun projectionLine(status: CategoryBudgetStatus): String = when (status.level) {
    BudgetStatusLevel.over ->
        "Ai dep\u0103\u015Fit cu ${RomanianMoneyFormatter.format((-status.remaining.amount), RomanianMoneyFormatter.Style.bareNumber)} RON."
    BudgetStatusLevel.projected_over ->
        "\u00CEn ritmul \u0103sta ajungi la ${(status.projectedPctUsed * 100).toInt()}% (~${RomanianMoneyFormatter.format(status.projectedSpend.amount, RomanianMoneyFormatter.Style.bareNumber)} RON) la final de ciclu."
    else ->
        "Mai ai ${RomanianMoneyFormatter.format(status.remaining.amount, RomanianMoneyFormatter.Style.bareNumber)} RON \u00B7 proiec\u021Bie ${(status.projectedPctUsed * 100).toInt()}%."
}

private fun solomonInsight(report: BudgetReport): String {
    if (!report.hasAnyBudget) {
        return "Seteaz\u0103 un buget pe categoriile tale \u015Fi \u00EEncep s\u0103 le urm\u0103resc zilnic \u2014 \u00EE\u021Bi spun din timp c\u00E2nd o iei razna, nu dup\u0103 ce s-a \u00EEnt\u00E2mplat."
    }
    val over = report.atRisk.firstOrNull { it.level == BudgetStatusLevel.over }
    val proj = report.atRisk.firstOrNull { it.level == BudgetStatusLevel.projected_over }
    return when {
        over != null ->
            "Ai dep\u0103\u015Fit bugetul la ${over.category.displayNameRO} (${(over.pctUsed * 100).toInt()}%). Mai sunt ${report.daysRemaining} zile p\u00E2n\u0103 la salariu \u2014 hai s\u0103 \u021Binem fr\u00E2na aici."
        proj != null ->
            "\u00CEn ritmul \u0103sta, ${proj.category.displayNameRO} ajunge la ${(proj.projectedPctUsed * 100).toInt()}% din buget p\u00E2n\u0103 la salariu. \u00CEnc\u0103 se poate corecta dac\u0103 \u00EEncetine\u015Fti pu\u021Bin."
        report.warningCount > 0 ->
            "E\u015Fti aproape de limit\u0103 la ${report.warningCount} ${if (report.warningCount == 1) "categorie" else "categorii"}. Le \u021Bin sub observa\u021Bie."
        else ->
            "Toate bugetele sunt pe drumul bun la ziua ${report.daysElapsed} din ${report.daysTotal}. Continu\u0103 a\u015Fa."
    }
}
