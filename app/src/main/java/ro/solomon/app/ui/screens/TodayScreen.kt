package ro.solomon.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import ro.solomon.app.ui.components.IngestionToast
import ro.solomon.app.ui.components.SolInsightCard
import ro.solomon.app.ui.theme.SolAccent
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
    vm: TodayViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
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

    Box(modifier = Modifier.fillMaxSize().background(SolomonColors.Background)) {
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
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.md)) {
                    StatPill("Venituri azi", "+${state.incomingToday} RON", SolomonColors.Incoming, modifier = Modifier.weight(1f))
                    StatPill("Cheltuieli azi", "-${state.outgoingToday} RON", SolomonColors.Outgoing, modifier = Modifier.weight(1f))
                }
            }
            val topCat = state.topCategory
            if (topCat != null) {
                item { QuickStatsCard(topCat.displayNameRO, state.topCategoryAmount, state.avgDailySpending30d) }
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
            ro.solomon.app.services.CoachMicroLessons.forDate(System.currentTimeMillis())?.let { lesson ->
                item { CoachLessonCard(lesson) }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.md)) {
                    ActionTile("Pot s\u0103-mi permit?", Icons.Filled.Search, onClick = onOpenCanIAfford, modifier = Modifier.weight(1f))
                    ActionTile("\u00CEntreab\u0103 Solomon", Icons.Filled.Chat, onClick = { showChat = true }, modifier = Modifier.weight(1f))
                }
            }
            item { Text("Ultimele tranzac\u021bii", style = MaterialTheme.typography.titleMedium, color = SolomonColors.TextPrimary) }
            if (state.recentTransactions.isEmpty()) {
                item { EmptyHint("Adaug\u0103 prima tranzac\u021bie din Chat sau manual din Portofel.") }
            } else {
                items(state.recentTransactions, key = { it.id }) { tx -> TransactionRow(tx) }
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
            Icon(androidx.compose.material.icons.Icons.Filled.Add, "Adauga tranzactie")
        }
    }
}

@Composable
private fun GreetingHeader(name: String, hasAlert: Boolean, onAlertsClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Bun\u0103${if (name.isNotBlank()) ", $name" else ""} \uD83D\uDC4B", style = MaterialTheme.typography.headlineLarge, color = SolomonColors.TextPrimary)
            Text("Azi e", style = MaterialTheme.typography.bodyMedium, color = SolomonColors.TextSecondary)
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SolomonColors.SurfaceVariant)
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
private fun QuickStatsCard(topCategory: String, topAmount: Int, avgDaily: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SolomonColors.Surface),
        shape = RoundedCornerShape(SolSpacing.lg)
    ) {
        Row(modifier = Modifier.padding(SolSpacing.base), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SolSpacing.base)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("TOP CATEGORIE \u00B7 30Z", style = MaterialTheme.typography.labelSmall, color = SolomonColors.TextTertiary)
                Spacer(Modifier.height(2.dp))
                Text(topCategory, color = SolomonColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("$topAmount RON", color = SolomonColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Box(modifier = Modifier.width(1.dp).height(36.dp).background(SolomonColors.Hairline))
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
private fun SafeToSpendCard(perDay: Int, daysLeft: Int, available: Int, paydayDay: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SolomonColors.Surface),
        shape = RoundedCornerShape(SolSpacing.lg)
    ) {
        Column(modifier = Modifier.padding(SolSpacing.lg)) {
            Text("DISPONIBIL LIBER \u00B7 URM\u0102TORUL SALARIU ZIUA $paydayDay", style = MaterialTheme.typography.labelSmall, color = SolomonColors.TextTertiary)
            Spacer(Modifier.height(SolSpacing.xs))
            Text(RomanianMoneyFormatter.format(perDay, RomanianMoneyFormatter.Style.bareNumber), style = MaterialTheme.typography.displayMedium, color = SolomonColors.Primary)
            Text("RON / zi \u00B7 $daysLeft zile p\u00E2n\u0103 la salariu", style = MaterialTheme.typography.titleSmall, color = SolomonColors.TextSecondary)
            Spacer(Modifier.height(SolSpacing.md))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
                Text("Total disponibil acum:", style = MaterialTheme.typography.bodySmall, color = SolomonColors.TextSecondary)
                Text(RomanianMoneyFormatter.format(available), style = MaterialTheme.typography.titleSmall, color = SolomonColors.TextPrimary)
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SolomonColors.Surface),
        shape = RoundedCornerShape(SolSpacing.md)
    ) {
        Column(Modifier.padding(SolSpacing.md)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = SolomonColors.TextSecondary)
            Spacer(Modifier.height(SolSpacing.xs))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = accent)
        }
    }
}

@Composable
private fun MomentCard(text: String, generating: Boolean, onTap: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
        colors = CardDefaults.cardColors(containerColor = SolomonColors.Surface),
        shape = RoundedCornerShape(SolSpacing.lg)
    ) {
        Row(Modifier.padding(SolSpacing.md), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(SolSpacing.sm))
                    .background(SolomonColors.Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) { Text("\uD83D\uDCA1", style = MaterialTheme.typography.titleLarge) }
            Spacer(Modifier.width(SolSpacing.md))
            Column(Modifier.weight(1f)) {
                Text("Solomon \u00ee\u021bi spune", style = MaterialTheme.typography.labelSmall, color = SolomonColors.Primary)
                if (generating) {
                    Text("Se g\u00e2nde\u0219te...", style = MaterialTheme.typography.bodyMedium, color = SolomonColors.TextSecondary)
                } else {
                    Text(text, style = MaterialTheme.typography.bodyMedium, color = SolomonColors.TextPrimary, maxLines = 4)
                }
            }
        }
    }
}

@Composable
private fun DailyTipCard() {
    val tip = ro.solomon.core.util.FinancialEducationTip.today
    SolInsightCard(
        label = "SFATUL ZILEI \u00B7 ${tip.category.label}",
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
        label = "ANTRENAMENT SOLOMON \u00B7 ${lesson.title}",
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
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(SolSpacing.md)
    ) {
        Icon(icon, null, tint = SolomonColors.Primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(SolSpacing.xs))
        Text(label, color = SolomonColors.TextPrimary)
    }
}

@Composable
private fun EmptyHint(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SolomonColors.Surface),
        shape = RoundedCornerShape(SolSpacing.md)
    ) {
        Text(text, modifier = Modifier.padding(SolSpacing.md), color = SolomonColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TransactionRow(tx: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SolomonColors.Surface),
        shape = RoundedCornerShape(SolSpacing.md)
    ) {
        Row(Modifier.padding(SolSpacing.md), verticalAlignment = Alignment.CenterVertically) {
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
                    "${tx.category.displayNameRO} \u00B7 ${RomanianDateFormatter.format(tx.date * 1000L, RomanianDateFormatter.Style.dayMonth)}",
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
}

@Composable
private fun Next7DaysCard(bills: List<TodayViewModel.UpcomingBillItem>, total: Int, count: Int, paydayDay: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SolomonColors.Surface),
        shape = RoundedCornerShape(SolSpacing.lg)
    ) {
        Column(modifier = Modifier.padding(SolSpacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("URM\u0102TOARELE 7 ZILE \u00B7 $count facturi", style = MaterialTheme.typography.labelSmall, color = SolomonColors.Primary)
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
                        Text("Ziua ${bill.dayOfMonth} \u00B7 ${bill.kindLabel}", style = MaterialTheme.typography.bodySmall, color = SolomonColors.TextSecondary)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SolomonColors.Surface),
        shape = RoundedCornerShape(SolSpacing.lg)
    ) {
        Column(Modifier.padding(SolSpacing.lg)) {
            Text("MISIUNEA TA \u00B7 ${mission.daysRemaining(now)} ZILE R\u0102MASE",
                style = MaterialTheme.typography.labelSmall,
                color = SolomonColors.Primary)
            Spacer(Modifier.height(SolSpacing.xs))
            Text(mission.title, style = MaterialTheme.typography.titleLarge, color = SolomonColors.TextPrimary)
            Spacer(Modifier.height(SolSpacing.xs))
            Text(mission.description, style = MaterialTheme.typography.bodyMedium, color = SolomonColors.TextSecondary)
            Spacer(Modifier.height(SolSpacing.md))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = SolomonColors.Primary,
                trackColor = SolomonColors.SurfaceVariant
            )
            Spacer(Modifier.height(SolSpacing.xs))
            Text("\u021aint\u0103: ${mission.targetSavingsRON} RON \u00B7 ${daysLeft} zile r\u0103mase",
                style = MaterialTheme.typography.bodySmall,
                color = SolomonColors.TextTertiary)
            if (mission.isCompleted) {
                Spacer(Modifier.height(SolSpacing.sm))
                Text("Misiune \u00eendeplinit\u0103 \uD83C\uDF89", color = SolomonColors.Incoming, style = MaterialTheme.typography.titleSmall)
            } else if (daysLeft == 0) {
                Spacer(Modifier.height(SolSpacing.sm))
                TextButton(onClick = onComplete) { Text("Marcheaz\u0103 ca terminat\u0103") }
            }
        }
    }
}

@Composable
private fun PendingMissionCard(mission: SolomonMission, onAccept: () -> Unit, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SolomonColors.SurfaceVariant),
        shape = RoundedCornerShape(SolSpacing.lg)
    ) {
        Column(Modifier.padding(SolSpacing.lg)) {
            Text("MISIUNE NOU\u0102", style = MaterialTheme.typography.labelSmall, color = SolomonColors.Primary)
            Spacer(Modifier.height(SolSpacing.xs))
            Text(mission.title, style = MaterialTheme.typography.titleLarge, color = SolomonColors.TextPrimary)
            Spacer(Modifier.height(SolSpacing.xs))
            Text(mission.description, style = MaterialTheme.typography.bodyMedium, color = SolomonColors.TextSecondary)
            Spacer(Modifier.height(SolSpacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
                Button(onClick = onAccept) { Text("Accept") }
                OutlinedButton(onClick = onDismiss) { Text("Nu acum") }
            }
        }
    }
}
