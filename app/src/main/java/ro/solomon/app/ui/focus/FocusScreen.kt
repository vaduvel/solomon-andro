package ro.solomon.app.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ro.solomon.analytics.BucketBreakdown
import ro.solomon.analytics.FocusOverview
import ro.solomon.analytics.FocusPlan
import ro.solomon.app.ui.components.AllocationSegment
import ro.solomon.app.ui.components.EmptyStateView
import ro.solomon.app.ui.components.SolAllocationBar
import ro.solomon.app.ui.components.SolChip
import ro.solomon.app.ui.components.SolHairlineDivider
import ro.solomon.app.ui.components.SolHeroAmount
import ro.solomon.app.ui.components.SolHeroCard
import ro.solomon.app.ui.components.SolHeroLabel
import ro.solomon.app.ui.components.SolLinearProgress
import ro.solomon.app.ui.components.SolListCard
import ro.solomon.app.ui.components.SolLoadingIndicator
import ro.solomon.app.ui.components.SolPrimaryButton
import ro.solomon.app.ui.components.SolSecondaryButton
import ro.solomon.app.ui.components.SolSectionHeaderRow
import ro.solomon.app.ui.theme.SolAccent
import ro.solomon.app.ui.theme.SolRadius
import ro.solomon.app.ui.theme.SolSpacing
import ro.solomon.app.ui.theme.SolomonColors
import ro.solomon.core.domain.FocusFeasibility
import ro.solomon.core.domain.FocusType
import ro.solomon.core.domain.Money

@Composable
fun FocusScreen(
    vm: FocusViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val overview = state.overview

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(SolSpacing.base),
        verticalArrangement = Arrangement.spacedBy(SolSpacing.md)
    ) {
        item { FocusHeader() }

        when {
            state.loading -> item { SolLoadingIndicator(accent = SolAccent.Mint, label = "Calculez Focus din date reale...") }
            overview == null -> item {
                EmptyStateView(
                    icon = "🎯",
                    title = "Focus nu e gata încă",
                    subtitle = "Adaugă tranzacții și obiective ca să pot calcula ce încape realist.",
                    accent = SolAccent.Mint
                )
            }
            else -> {
                item { FocusHero(overview) }
                item { BucketCard(overview.buckets) }
                item { ActiveFocusesCard(overview.plans, onMakePrimary = vm::makePrimary, onRemove = vm::remove) }
                item { QuickAddCard(onRunway = { vm.quickAdd(FocusType.runway, makePrimary = true) }, onDetox = { vm.quickAdd(FocusType.moft_detox, detoxPercent = 30) }) }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun FocusHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Focus", style = MaterialTheme.typography.headlineLarge, color = SolomonColors.TextPrimary)
        Text(
            "Prioritizare reală: Necesar, Moft, Prioritate — fără să opresc obiectivele.",
            style = MaterialTheme.typography.bodyMedium,
            color = SolomonColors.TextSecondary
        )
    }
}

@Composable
private fun FocusHero(overview: FocusOverview) {
    SolHeroCard(accent = if (overview.disponibil.isNegative) SolAccent.Rose else SolAccent.Mint) {
        SolHeroLabel("Disponibil după Necesar + obiective + Focus")
        SolHeroAmount(
            amount = leiWhole(overview.disponibil),
            currency = "RON",
            accent = if (overview.disponibil.isNegative) SolAccent.Rose else SolAccent.Mint
        )
        Text(
            overview.coachingRO,
            style = MaterialTheme.typography.bodyMedium,
            color = SolomonColors.TextSecondary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
            SolChip("${leiWhole(overview.dailyAllowance)} RON / zi", SolAccent.Blue)
            SolChip("${overview.daysUntilPayday} zile", SolAccent.Mint)
        }
    }
}

@Composable
private fun BucketCard(buckets: BucketBreakdown) {
    val segments = bucketSegments(buckets)
    SolListCard {
        Column(modifier = Modifier.padding(SolSpacing.lg), verticalArrangement = Arrangement.spacedBy(SolSpacing.md)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("BUCKETS LUNARE", style = MaterialTheme.typography.labelSmall, color = SolomonColors.Primary)
                Text("Total ${leiWhole(buckets.total)} RON", style = MaterialTheme.typography.titleSmall, color = SolomonColors.TextPrimary)
            }
            if (segments.isNotEmpty()) {
                SolAllocationBar(segments = segments, height = 9)
            }
            BucketLegend("Necesar", buckets.necesar, SolAccent.Mint, "protejat")
            BucketLegend("Moft", buckets.moft, SolAccent.Amber, "se taie primul")
            BucketLegend("Prioritate", buckets.prioritate, SolAccent.Blue, "creștere")
        }
    }
}

@Composable
private fun BucketLegend(label: String, amount: Money, accent: SolAccent, meta: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(50))
                .background(accent.color)
        )
        Text(label, color = SolomonColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(meta, color = SolomonColors.TextTertiary, fontSize = 12.sp)
        Text("${leiWhole(amount)} RON", color = accent.color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActiveFocusesCard(
    plans: List<FocusPlan>,
    onMakePrimary: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
        SolSectionHeaderRow(title = "Focusuri active", meta = "max 2-3")
        if (plans.isEmpty()) {
            EmptyStateView(
                icon = "🎯",
                title = "Niciun Focus activ",
                subtitle = "Pornește cu “Rămâi pe plus” sau un detox de mofturi.",
                accent = SolAccent.Mint
            )
        } else {
            plans.forEach { plan ->
                FocusPlanCard(plan = plan, onMakePrimary = onMakePrimary, onRemove = onRemove)
            }
        }
    }
}

@Composable
private fun FocusPlanCard(
    plan: FocusPlan,
    onMakePrimary: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    val focus = plan.focus
    SolListCard {
        Column(modifier = Modifier.padding(SolSpacing.lg), verticalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(focus.title, color = SolomonColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(focus.type.displayNameRO, color = SolomonColors.TextSecondary, fontSize = 13.sp)
                }
                SolChip(plan.feasibility.displayNameRO, feasibilityAccent(plan.feasibility))
            }

            Text(plan.coachingRO, color = SolomonColors.TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)

            if (focus.type.hasMonetaryTarget) {
                SolLinearProgress(progress = focus.progressFraction.toFloat(), accent = SolAccent.Mint)
                Text(
                    "Țintă ${leiWhole(focus.targetAmount)} RON · rămas ${leiWhole(focus.amountRemaining)} RON",
                    color = SolomonColors.TextTertiary,
                    fontSize = 12.sp
                )
            }

            if (plan.recommendedMonthlyContribution.amount > 0) {
                Text(
                    "Recomandare: ${leiWhole(plan.recommendedMonthlyContribution)} RON / lună",
                    color = SolomonColors.Primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                if (!focus.isPrimary) {
                    FocusMiniAction("Principal", SolAccent.Mint) { onMakePrimary(focus.id) }
                } else {
                    SolChip("principal", SolAccent.Mint)
                }
                FocusMiniAction("Șterge", SolAccent.Rose) { onRemove(focus.id) }
            }
        }
    }
}

@Composable
private fun QuickAddCard(onRunway: () -> Unit, onDetox: () -> Unit) {
    SolListCard {
        Column(modifier = Modifier.padding(SolSpacing.lg), verticalArrangement = Arrangement.spacedBy(SolSpacing.md)) {
            Text("Adaugă rapid", style = MaterialTheme.typography.titleMedium, color = SolomonColors.TextPrimary)
            Text(
                "Acestea sunt Focusuri reale: se salvează local și recalculează disponibilul pe baza tranzacțiilor, obiectivelor și bucket-urilor.",
                color = SolomonColors.TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            SolPrimaryButton(title = "Rămâi pe plus", accent = SolAccent.Mint, fullWidth = true, onClick = onRunway)
            SolSecondaryButton(title = "Detox mofturi 30%", fullWidth = true, onClick = onDetox)
        }
    }
}

@Composable
private fun FocusMiniAction(label: String, accent: SolAccent, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(SolRadius.pill))
            .background(accent.color.copy(alpha = 0.10f))
            .border(1.dp, accent.color.copy(alpha = 0.22f), RoundedCornerShape(SolRadius.pill))
            .clickable(onClick = onClick)
            .padding(horizontal = SolSpacing.sm, vertical = 6.dp)
    ) {
        Text(label, color = accent.color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun bucketSegments(buckets: BucketBreakdown): List<AllocationSegment> {
    val total = buckets.total.amount.toFloat().takeIf { it > 0f } ?: return emptyList()
    return buildList {
        if (buckets.necesar.amount > 0) add(AllocationSegment(buckets.necesar.amount / total, SolAccent.Mint))
        if (buckets.moft.amount > 0) add(AllocationSegment(buckets.moft.amount / total, SolAccent.Amber))
        if (buckets.prioritate.amount > 0) add(AllocationSegment(buckets.prioritate.amount / total, SolAccent.Blue))
    }
}

private fun feasibilityAccent(feasibility: FocusFeasibility): SolAccent = when (feasibility) {
    FocusFeasibility.realist -> SolAccent.Mint
    FocusFeasibility.strans -> SolAccent.Amber
    FocusFeasibility.nerealist -> SolAccent.Rose
}

private fun leiWhole(money: Money): String = (money.amount / 100).toString()
