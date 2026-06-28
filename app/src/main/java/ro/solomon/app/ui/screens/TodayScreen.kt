package ro.solomon.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.delay
import ro.solomon.app.services.IngestionEvent
import ro.solomon.app.services.IngestionEventBus
import ro.solomon.app.services.SolomonMission
import ro.solomon.app.ui.chat.ChatSheet
import ro.solomon.app.ui.components.EmptyStateView
import ro.solomon.app.ui.components.IngestionToast
import ro.solomon.app.ui.components.MeshBackground
import ro.solomon.app.ui.components.SolChip
import ro.solomon.app.ui.components.SolHairlineDivider
import ro.solomon.app.ui.components.SolHeroAmount
import ro.solomon.app.ui.components.SolHeroCard
import ro.solomon.app.ui.components.SolHeroLabel
import ro.solomon.app.ui.components.SolInsightCard
import ro.solomon.app.ui.components.SolLinearProgress
import ro.solomon.app.ui.components.SolListCard
import ro.solomon.app.ui.components.SolPrimaryButton
import ro.solomon.app.ui.components.SolSecondaryButton
import ro.solomon.app.ui.components.SolSectionHeaderRow
import ro.solomon.app.ui.components.SolStatCard
import ro.solomon.app.ui.theme.SolAccent
import ro.solomon.app.ui.theme.SolRadius
import ro.solomon.app.ui.theme.SolSpacing
import ro.solomon.app.ui.theme.SolomonColors
import ro.solomon.core.domain.Transaction
import ro.solomon.core.domain.FlowDirection
import ro.solomon.core.format.RomanianDateFormatter
import ro.solomon.core.format.RomanianMoneyFormatter

@Composable
fun TodayScreen(
    onOpenChat: () -> Unit = {},
    onOpenCanIAfford: () -> Unit = {},
    onOpenFocus: () -> Unit = {},
    vm: TodayViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val coachLesson by produceState<ro.solomon.app.services.CoachMicroLessons.MicroLesson?>(
        initialValue = ro.solomon.app.services.CoachMicroLessons.forDate(System.currentTimeMillis()),
        context
    ) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val vuln = ro.solomon.app.services.SolomonCoachMemory.vulnerability(context)
            val script = ro.solomon.app.services.CoachProfileStore.load(context).moneyScript
            ro.solomon.app.services.CoachMicroLessons.forContext(System.currentTimeMillis(), vuln, script)
        }
    }
    var showChat by remember { mutableStateOf(false) }
    var showManualTxn by remember { mutableStateOf(false) }
    var showAlerts by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastDetail by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.offerMissionIfReady() }

    LaunchedEffect(Unit) {
        IngestionEventBus.events.collect { event ->
            when (event) {
                is IngestionEvent.BankNotificationIngested -> {
                    toastMessage = "Notificare important\u0103"
                    toastDetail = "${event.count} ${if (event.count == 1) "tranzac\u021bie" else "tranzac\u021bii"}"
                }
                is IngestionEvent.ShareIntentIngested -> {
                    toastMessage = "Receipt partajat"
                    toastDetail = "${event.count} ${if (event.count == 1) "tranzac\u021bie ad\u0103ugat\u0103" else "tranzac\u021bii ad\u0103ugate"}"
                }
                is IngestionEvent.ErrorOccurred -> {
                    toastMessage = "Eroare ${event.source}"
                    toastDetail = event.message
                }
            }
            delay(3500)
            toastMessage = null
            toastDetail = null
        }
    }

    if (showAlerts) {
        ro.solomon.app.ui.alerts.AlertsSheet(onDismiss = { showAlerts = false })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MeshBackground()

        toastMessage?.let { msg ->
            Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = SolSpacing.base).fillMaxWidth()) {
                IngestionToast(title = msg, detail = toastDetail)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(SolSpacing.base),
            verticalArrangement = Arrangement.spacedBy(SolSpacing.md)
        ) {
            item { GreetingHeader(state.userName, state.hasUnreadAlert, onAlertsClick = { showAlerts = true }) }
            item { SafeToSpendCard(state.safeToSpendPerDay, state.daysUntilPayday, state.balanceAvailable, state.paydayDayOfMonth) }
            item { FocusEntryCard(onOpenFocus = onOpenFocus) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.md)) {
                    SolStatCard(
                        label = "Azi",
                        name = "Venituri",
                        value = "+${state.incomingToday} RON",
                        icon = Icons.Filled.ArrowDownward,
                        iconAccent = SolAccent.Success,
                        modifier = Modifier.weight(1f)
                    )
                    SolStatCard(
                        label = "Azi",
                        name = "Cheltuieli",
                        value = "-${state.outgoingToday} RON",
                        icon = Icons.Filled.ArrowUpward,
                        iconAccent = SolAccent.Error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            val topCat = state.topCategory
            if (topCat != null) {
                item { QuickStatsCard(topCat.displayNameRO, state.topCategoryAmount, state.avgDailySpending30d) }
                if (state.topCategoryAmount > 0) {
                    item { ReflectionCard(category = topCat.displayNameRO, onTap = { showChat = true }) }
                }
            }
            if (state.upcomingBills.isNotEmpty()) {
                item {
                    Next7DaysCard(
                        bills = state.upcomingBills,
                        total = state.upcomingBillsTotal,
                        count = state.upcomingBillsCount,
                        paydayDay = state.paydayDayOfMonth
                    )
                }
            }
            state.activeMission?.let { mission ->
                item { ActiveMissionCard(mission, onComplete = { vm.completeMission() }) }
            }
            state.pendingMission?.let { mission ->
                item {
                    PendingMissionCard(
                        mission = mission,
                        onAccept = { vm.acceptMission() },
                        onDismiss = { vm.dismissMission() }
                    )
                }
            }
            if (state.activeMission == null) {
                state.lastCommitment?.takeIf { it.isNotBlank() }?.let { commitment ->
                    item { LastCommitmentCard(commitment) }
                }
            }
            item {
                MomentCard(
                    text = state.momentText,
                    generating = state.generatingMoment,
                    onTap = { showChat = true }
                )
            }
            item { DailyTipCard() }
            coachLesson?.let { lesson ->
                item { CoachLessonCard(lesson) }
            }
            if (state.commitmentCount > 0 || state.hasEngagementHistory) {
                item {
                    CoachProgressCard(
                        respectRate = state.commitmentRespectRate,
                        resolvedCount = state.resolvedCommitmentCount,
                        commitmentCount = state.commitmentCount,
                        engagementRatio = state.engagementRatio,
                        hasEngagementHistory = state.hasEngagementHistory
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.md)) {
                    ActionTile("Pot s\u0103-mi permit?", Icons.Filled.Search, onClick = onOpenCanIAfford, modifier = Modifier.weight(1f))
                    ActionTile("\u00centreab\u0103 Solomon", Icons.Filled.Chat, onClick = { showChat = true }, modifier = Modifier.weight(1f))
                }
            }
            item { SolSectionHeaderRow(title = "Ultimele tranzac\u021bii") }
            if (state.recentTransactions.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = "\uD83D\uDCB3",
                        title = "Nicio tranzac\u021bie \u00eenc\u0103",
                        subtitle = "Adaug\u0103 prima tranzac\u021bie din Chat sau manual din Portofel.",
                        accent = SolAccent.Mint
                    )
                }
            } else {
                item {
                    SolListCard {
                        state.recentTransactions.forEachIndexed { index, tx ->
                            TransactionRow(tx)
                            if (index < state.recentTransactions.lastIndex) SolHairlineDivider()
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }

        if (showChat) {
            ChatSheet(onDismiss = { showChat = false })
        }
        if (showManualTxn) {
            ro.solomon.app.ui.edit.ManualTransactionScreen(onDismiss = { showManualTxn = false })
        }

        FloatingActionButton(
            onClick = { showManualTxn = true },
            containerColor = SolomonColors.Primary,
            contentColor = SolomonColors.OnPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(SolSpacing.base)
        ) {
            Icon(Icons.Filled.Add, "Adauga tranzactie")
        }
    }
}

@Composable
private fun GreetingHeader(name: String, hasAlert: Boolean, onAlertsClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Ast\u0103zi", style = MaterialTheme.typography.headlineLarge, color = SolomonColors.TextPrimary)
            Text(
                if (name.isNotBlank()) "Bun\u0103, $name" else "Bun\u0103",
                style = MaterialTheme.typography.bodyMedium,
                color = SolomonColors.TextSecondary
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .clickable { onAlertsClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "Alerte",
                tint = if (hasAlert) SolomonColors.Primary else SolomonColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            if (hasAlert) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(SolomonColors.Rose)
                )
            }
        }
    }
}

@Composable
private fun SafeToSpendCard(perDay: Int, daysLeft: Int, available: Int, paydayDay: Int) {
    SolHeroCard(accent = SolAccent.Mint) {
        SolHeroLabel("Disponibil liber \u00b7 salariu ziua $paydayDay")
        SolHeroAmount(
            amount = RomanianMoneyFormatter.format(perDay, RomanianMoneyFormatter.Style.bareNumber),
            currency = "RON / zi",
            accent = SolAccent.Mint
        )
        Text(
            "Aproximativ at\u00e2t pe zi \u00ee\u021bi po\u021bi permite \u2014 nu trebuie s\u0103-i cheltui, e doar perimetrul \u00een care n-ai grij\u0103.",
            style = MaterialTheme.typography.bodySmall,
            color = SolomonColors.TextSecondary
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
            Text("$daysLeft zile p\u00e2n\u0103 la salariu", style = MaterialTheme.typography.bodySmall, color = SolomonColors.TextTertiary)
            Text("\u00b7", color = SolomonColors.TextTertiary)
            Text("Disponibil: ${RomanianMoneyFormatter.format(available)}", style = MaterialTheme.typography.titleSmall, color = SolomonColors.TextPrimary)
        }
    }
}

@Composable
private fun FocusEntryCard(onOpenFocus: () -> Unit) {
    SolListCard(modifier = Modifier.clickable(onClick = onOpenFocus)) {
        Column(modifier = Modifier.padding(SolSpacing.lg), verticalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("SOLOMON FOCUS", style = MaterialTheme.typography.labelSmall, color = SolomonColors.Primary)
                SolChip("nou", SolAccent.Mint)
            }
            Text("Ține-mă pe plus", color = SolomonColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Deschide prioritizarea pe Necesar / Moft / Prioritate. Focus rulează peste bugetare și obiective, fără să le oprească.",
                color = SolomonColors.TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            SolLinearProgress(progress = 0.62f, accent = SolAccent.Mint)
            Text("Atinge cardul ca să vezi calculul real", color = SolomonColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun QuickStatsCard(topCategory: String, topAmount: Int, avgDaily: Int) {
    SolListCard {
        Row(
            modifier = Modifier.padding(SolSpacing.base),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SolSpacing.base)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("TOP CATEGORIE \u00b7 30Z", style = MaterialTheme.typography.labelSmall, color = SolomonColors.TextTertiary)
                Spacer(Modifier.height(2.dp))
                Text(topCategory, color = SolomonColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("$topAmount RON", color = SolomonColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color.White.copy(alpha = 0.08f)))
            Column(modifier = Modifier.weight(1f)) {
                Text("MEDIE ZILNIC\u0102", style = MaterialTheme.typography.labelSmall, color = SolomonColors.TextTertiary)
                Spacer(Modifier.height(2.dp))
                Text("$avgDaily RON", color = SolomonColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("ultimele 30 zile", color = SolomonColors.TextTertiary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun MomentCard(text: String, generating: Boolean, onTap: () -> Unit) {
    SolInsightCard(
        label = "Solomon \u00ee\u021bi spune",
        timestamp = "acum",
        accent = SolAccent.Mint,
        modifier = Modifier.clickable(onClick = onTap)
    ) {
        if (generating) {
            Text("Se g\u00e2nde\u0219te...", style = MaterialTheme.typography.bodyMedium, color = SolomonColors.TextSecondary)
        } else {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = SolomonColors.TextPrimary)
        }
        Text("Apas\u0103 pentru detalii", color = SolomonColors.Primary, fontSize = 12.sp)
    }
}

@Composable
private fun DailyTipCard() {
    val tip = ro.solomon.core.util.FinancialEducationTip.today
    SolInsightCard(
        label = "SFATUL ZILEI \u00b7 ${tip.category.label}",
        timestamp = null,
        accent = SolAccent.Violet
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(tip.emoji, fontSize = 24.sp)
            Spacer(Modifier.width(SolSpacing.sm))
            Text(
                tip.text,
                color = SolomonColors.TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun CoachLessonCard(lesson: ro.solomon.app.services.CoachMicroLessons.MicroLesson) {
    SolInsightCard(
        label = "ANTRENAMENT SOLOMON \u00b7 ${lesson.title}",
        timestamp = null,
        accent = SolAccent.Blue
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text("\uD83E\uDDE0", fontSize = 24.sp)
            Spacer(Modifier.width(SolSpacing.sm))
            Text(
                lesson.text,
                color = SolomonColors.TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun ReflectionCard(category: String, onTap: () -> Unit) {
    SolInsightCard(
        label = "REFLEC\u021aIE \u00b7 $category",
        timestamp = null,
        accent = SolAccent.Amber
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.clickable(onClick = onTap)) {
            Text("\uD83E\uDD14", fontSize = 24.sp)
            Spacer(Modifier.width(SolSpacing.sm))
            Column {
                Text(
                    ro.solomon.app.services.CoachingVoice.worthItQuestion(category),
                    color = SolomonColors.TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(SolSpacing.xs))
                Text(
                    "Apas\u0103 ca s\u0103 reflect\u0103m \u00eempreun\u0103",
                    color = SolomonColors.Primary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun CoachProgressCard(
    respectRate: Double,
    resolvedCount: Int,
    commitmentCount: Int,
    engagementRatio: Double,
    hasEngagementHistory: Boolean
) {
    val pct = (respectRate * 100).toInt()
    val engagePct = (engagementRatio * 100).toInt()
    SolInsightCard(
        label = "SOLOMON TE OBSERV\u0102",
        timestamp = null,
        accent = SolAccent.Violet
    ) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Text("\uD83D\uDCC8", fontSize = 24.sp)
                Spacer(Modifier.width(SolSpacing.sm))
                Text(
                    if (resolvedCount > 0)
                        "Ai respectat $pct% din angajamente ($resolvedCount din $commitmentCount). Te v\u0103d."
                    else if (commitmentCount > 0)
                        "Ai luat $commitmentCount ${if (commitmentCount == 1) "angajament" else "angajamente"}. Termin\u0103 unul \u0219i \u00eencep s\u0103 num\u0103r respectarea."
                    else
                        "\u00cencep s\u0103 \u00een\u021beleg cum reac\u021bionezi la sugestiile mele.",
                    color = SolomonColors.TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
            if (hasEngagementHistory) {
                Spacer(Modifier.height(SolSpacing.sm))
                SolLinearProgress(
                    progress = engagementRatio.toFloat(),
                    accent = SolAccent.Violet,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(SolSpacing.xs))
                Text(
                    "Dai curs la $engagePct% din ce-\u021bi propun.",
                    color = SolomonColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun LastCommitmentCard(commitment: String) {
    SolInsightCard(
        label = "ULTIMUL T\u0102U ANGAJAMENT",
        timestamp = null,
        accent = SolAccent.Amber
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text("\uD83E\uDD1D", fontSize = 24.sp)
            Spacer(Modifier.width(SolSpacing.sm))
            Text(
                commitment,
                color = SolomonColors.TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun ActionTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(SolRadius.md))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(SolRadius.md))
            .clickable(onClick = onClick)
            .padding(horizontal = SolSpacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)
    ) {
        Icon(icon, null, tint = SolomonColors.Primary, modifier = Modifier.size(18.dp))
        Text(label, color = SolomonColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun TransactionRow(tx: Transaction) {
    Row(Modifier.fillMaxWidth().padding(SolSpacing.md), verticalAlignment = Alignment.CenterVertically) {
        val accent = if (tx.direction == FlowDirection.incoming) SolomonColors.Incoming else SolomonColors.Outgoing
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(SolSpacing.sm))
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (tx.direction == FlowDirection.incoming) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                null,
                tint = accent
            )
        }
        Spacer(Modifier.width(SolSpacing.md))
        Column(Modifier.weight(1f)) {
            Text(tx.merchant ?: tx.category.displayNameRO, style = MaterialTheme.typography.titleSmall, color = SolomonColors.TextPrimary)
            Text(
                "${tx.category.displayNameRO} \u00b7 ${RomanianDateFormatter.format(tx.date * 1000L, RomanianDateFormatter.Style.dayMonth)}",
                style = MaterialTheme.typography.bodySmall,
                color = SolomonColors.TextSecondary
            )
        }
        val sign = if (tx.direction == FlowDirection.incoming) "+" else "-"
        Text(
            "$sign${RomanianMoneyFormatter.format(tx.amount.amount)}",
            style = MaterialTheme.typography.titleMedium,
            color = accent
        )
    }
}

@Composable
private fun Next7DaysCard(bills: List<TodayViewModel.UpcomingBillItem>, total: Int, count: Int, paydayDay: Int) {
    SolListCard {
        Column(modifier = Modifier.padding(SolSpacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("URM\u0102TOARELE 7 ZILE \u00b7 $count facturi", style = MaterialTheme.typography.labelSmall, color = SolomonColors.Primary)
                Text("Total: ${RomanianMoneyFormatter.format(total)}", style = MaterialTheme.typography.titleSmall, color = SolomonColors.TextPrimary)
            }
            Spacer(Modifier.height(SolSpacing.md))
            bills.forEach { bill ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SolSpacing.md)
                ) {
                    val accent = if (bill.isEssential) SolAccent.Amber else SolAccent.Blue
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accent.color.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${bill.daysRemaining}", color = accent.color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(bill.name, style = MaterialTheme.typography.titleSmall, color = SolomonColors.TextPrimary, maxLines = 1)
                        Text("Ziua ${bill.dayOfMonth} \u00b7 ${bill.kindLabel}", style = MaterialTheme.typography.bodySmall, color = SolomonColors.TextSecondary)
                    }
                    Text(RomanianMoneyFormatter.format(bill.amount), style = MaterialTheme.typography.titleSmall, color = if (bill.isEssential) SolomonColors.Amber else SolomonColors.TextPrimary)
                }
            }
            Spacer(Modifier.height(SolSpacing.sm))
            Text("Salariu: ziua $paydayDay", style = MaterialTheme.typography.bodySmall, color = SolomonColors.TextTertiary)
        }
    }
}

@Composable
private fun ActiveMissionCard(mission: SolomonMission, onComplete: () -> Unit) {
    val now = System.currentTimeMillis() / 1000L
    val progress = mission.progressFraction(now).toFloat()
    val daysLeft = mission.daysRemaining(now)
    SolHeroCard(accent = SolAccent.Mint, badge = "Misiune") {
        SolHeroLabel("Misiunea ta \u00b7 $daysLeft zile r\u0103mase")
        Text(mission.title, style = MaterialTheme.typography.titleLarge, color = SolomonColors.TextPrimary)
        Text(mission.description, style = MaterialTheme.typography.bodyMedium, color = SolomonColors.TextSecondary)
        SolLinearProgress(progress = progress, accent = SolAccent.Mint, height = 8, modifier = Modifier.fillMaxWidth())
        Text(
            "\u021aint\u0103: ${mission.targetSavingsRON} RON \u00b7 $daysLeft zile r\u0103mase",
            style = MaterialTheme.typography.bodySmall,
            color = SolomonColors.TextTertiary
        )
        if (mission.isCompleted) {
            Text("Misiune \u00eendeplinit\u0103 \uD83C\uDF89", color = SolomonColors.Incoming, style = MaterialTheme.typography.titleSmall)
        } else if (daysLeft == 0) {
            SolPrimaryButton(title = "Marcheaz\u0103 ca terminat\u0103", onClick = onComplete)
        }
    }
}

@Composable
private fun PendingMissionCard(mission: SolomonMission, onAccept: () -> Unit, onDismiss: () -> Unit) {
    SolInsightCard(label = "Misiune nou\u0103", accent = SolAccent.Mint) {
        Text(mission.title, style = MaterialTheme.typography.titleLarge, color = SolomonColors.TextPrimary)
        Text(mission.description, style = MaterialTheme.typography.bodyMedium, color = SolomonColors.TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
            SolPrimaryButton(title = "Accept", onClick = onAccept)
            SolSecondaryButton(title = "Nu acum", onClick = onDismiss)
        }
    }
}
