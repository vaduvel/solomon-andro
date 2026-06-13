package ro.solomon.app.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.services.MomentEntry
import ro.solomon.app.services.MomentHistoryStore
import ro.solomon.app.ui.components.SolAccent
import ro.solomon.app.ui.components.SolChip
import ro.solomon.app.ui.components.SolSectionHeaderRow
import ro.solomon.app.ui.components.SolBackButton
import ro.solomon.app.ui.components.EmptyStateView
import ro.solomon.app.ui.theme.SolRadius
import ro.solomon.app.ui.theme.SolSpacing
import ro.solomon.app.ui.theme.SolomonColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DisplayAlert(
    val id: String,
    val typeKey: String,
    val title: String,
    val body: String,
    val generatedAt: Long,
    val badge: String? = null
)

class AlertsViewModel : ViewModel() {

    private val _alerts = MutableStateFlow<List<DisplayAlert>>(emptyList())
    val alerts: StateFlow<List<DisplayAlert>> = _alerts.asStateFlow()

    private val _currentAlert = MutableStateFlow<DisplayAlert?>(null)
    val currentAlert: StateFlow<DisplayAlert?> = _currentAlert.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        viewModelScope.launch {
            val ctx = ServiceLocator.appContext
            val all = withContext(Dispatchers.IO) { MomentHistoryStore.list(ctx) }
            val mapped = all.sortedByDescending { it.generatedAt }.map { it.toDisplay() }
            _currentAlert.value = mapped.firstOrNull { isToday(it.generatedAt) }
            _alerts.value = mapped.filter { it.id != _currentAlert.value?.id }
            _loading.value = false
        }
    }

    fun dismiss(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { MomentHistoryStore.dismiss(ServiceLocator.appContext, id) }
            _currentAlert.value = _currentAlert.value?.takeIf { it.id != id }
            _alerts.value = _alerts.value.filter { it.id != id }
        }
    }

    private fun isToday(epochSeconds: Long): Boolean {
        val cal = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = epochSeconds * 1000 }
        return cal.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    }

    private fun MomentEntry.toDisplay(): DisplayAlert = DisplayAlert(
        id = id,
        typeKey = type,
        title = title,
        body = body,
        generatedAt = generatedAt,
        badge = badgeFor(type)
    )
}

private fun badgeFor(typeKey: String): String? = when (typeKey) {
    "spiral_alert" -> "URGENT"
    "upcoming_obligation" -> "PLATĂ"
    "can_i_afford" -> "DECIZIE"
    "payday" -> "VENIT"
    "pattern_alert" -> "INSIGHT"
    "subscription_audit" -> "ECONOMIE"
    "weekly_summary" -> "WEEKLY"
    "wow_moment" -> "BRAVO"
    else -> null
}

private fun accentForTypeKey(typeKey: String): SolAccent = when (typeKey) {
    "spiral_alert" -> SolAccent.Rose
    "upcoming_obligation" -> SolAccent.Amber
    "can_i_afford" -> SolAccent.Blue
    "payday" -> SolAccent.Mint
    "pattern_alert" -> SolAccent.Violet
    "subscription_audit" -> SolAccent.Amber
    "weekly_summary" -> SolAccent.Blue
    "wow_moment" -> SolAccent.Mint
    else -> SolAccent.Mint
}

private fun iconFor(typeKey: String): String = when (typeKey) {
    "spiral_alert" -> "!"
    "upcoming_obligation" -> "€"
    "can_i_afford" -> "?"
    "payday" -> "+"
    "pattern_alert" -> "∑"
    "subscription_audit" -> "↺"
    "weekly_summary" -> "7"
    "wow_moment" -> "★"
    else -> "•"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsSheet(onDismiss: () -> Unit, vm: AlertsViewModel = viewModel()) {
    val alerts by vm.alerts.collectAsStateWithLifecycle()
    val current by vm.currentAlert.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SolomonColors.Background,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = SolSpacing.screenHorizontal, vertical = SolSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SolBackButton(onClick = onDismiss)
                Spacer(Modifier.width(SolSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "SOLOMON · ALERTE",
                        color = SolomonColors.TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        greetingText(alerts.size, current != null),
                        color = SolomonColors.TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SolSpacing.screenHorizontal)
            ) {
                Spacer(Modifier.height(SolSpacing.sm))

                when {
                    loading -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = SolomonColors.Primary, strokeWidth = 2.dp)
                        }
                    }
                    current != null -> {
                        SolSectionHeaderRow("AZI", meta = (1 + alerts.count { isToday(it.generatedAt) }).toString())
                        Spacer(Modifier.height(SolSpacing.sm))
                        AlertCard(current!!)
                        alerts.filter { isToday(it.generatedAt) }.forEach { AlertCard(it) }
                    }
                    alerts.isNotEmpty() -> {
                        SolSectionHeaderRow("AZI", meta = alerts.count { isToday(it.generatedAt) }.toString())
                        Spacer(Modifier.height(SolSpacing.sm))
                        alerts.filter { isToday(it.generatedAt) }.forEach { AlertCard(it) }
                    }
                    else -> {
                        Spacer(Modifier.height(SolSpacing.lg))
                        EmptyStateView(
                            icon = "🔕",
                            title = "Nicio alertă activă",
                            subtitle = "Solomon monitorizează datele tale și te avertizează când detectează ceva important."
                        )
                    }
                }

                val older = alerts.filter { !isToday(it.generatedAt) }
                if (older.isNotEmpty()) {
                    Spacer(Modifier.height(SolSpacing.lg))
                    SolSectionHeaderRow("ANTERIOARE", meta = older.size.toString())
                    Spacer(Modifier.height(SolSpacing.sm))
                    older.forEach { AlertCard(it, dimmed = true) }
                }

                Spacer(Modifier.height(SolSpacing.xl))
            }
        }
    }
}

private fun greetingText(totalAlerts: Int, hasCurrent: Boolean): String {
    val total = totalAlerts + (if (hasCurrent) 1 else 0)
    return when {
        total == 0 -> "Liniște"
        total == 1 -> "1 nouă"
        else -> "$total noi"
    }
}

private fun isToday(epochSeconds: Long): Boolean {
    val cal = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = epochSeconds * 1000 }
    return cal.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
}

@Composable
private fun AlertCard(alert: DisplayAlert, dimmed: Boolean = false) {
    val accent = accentForTypeKey(alert.typeKey)
    val titleColor = if (dimmed) SolomonColors.TextSecondary else SolomonColors.TextPrimary
    val bodyColor = if (dimmed) SolomonColors.TextTertiary else SolomonColors.TextSecondary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(SolRadius.lg))
            .background(SolomonColors.Surface)
            .border(1.dp, accent.color.copy(alpha = if (dimmed) 0.10f else 0.20f), RoundedCornerShape(SolRadius.lg))
            .clickable { }
            .padding(horizontal = SolSpacing.base, vertical = SolSpacing.md)
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(SolSpacing.md)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.color.copy(alpha = 0.15f))
                    .border(1.dp, accent.color.copy(alpha = 0.30f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(iconFor(alert.typeKey), color = accent.color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        alert.title,
                        color = titleColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        timeAgo(alert.generatedAt),
                        color = SolomonColors.TextTertiary,
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(alert.body, color = bodyColor, fontSize = 13.sp, lineHeight = 18.sp)
                if (alert.badge != null) {
                    Spacer(Modifier.height(SolSpacing.sm))
                    SolChip(alert.badge, accent = accent)
                }
            }
        }
    }
}

private fun timeAgo(epochSeconds: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - epochSeconds
    return when {
        diff < 60 -> "acum"
        diff < 3600 -> "${diff / 60}m"
        diff < 86400 -> "${diff / 3600}h"
        diff < 7 * 86400 -> "${diff / 86400}z"
        else -> {
            val sdf = SimpleDateFormat("d MMM", Locale("ro", "RO"))
            sdf.format(Date(epochSeconds * 1000))
        }
    }
}
