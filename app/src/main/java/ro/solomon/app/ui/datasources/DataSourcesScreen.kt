package ro.solomon.app.ui.datasources

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ro.solomon.app.ui.components.SolAccent
import ro.solomon.app.ui.components.SolBackButton
import ro.solomon.app.ui.components.SolHairlineDivider
import ro.solomon.app.ui.components.SolHeroCard
import ro.solomon.app.ui.components.SolListCard
import ro.solomon.app.ui.components.SolSectionHeaderRow
import ro.solomon.app.ui.theme.SolRadius
import ro.solomon.app.ui.theme.SolSpacing
import ro.solomon.app.ui.theme.SolomonColors

data class IngestionSource(
    val key: String,
    val title: String,
    val description: String,
    val coverage: String,
    val coveragePercent: Int,
    val isGranted: () -> Boolean,
    val isConfigured: () -> Boolean = { true },
    val onEnable: ((Context) -> Unit)? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSourcesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }

    fun notificationAccessGranted(): Boolean {
        val pkg = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat?.contains(pkg) == true
    }

    val sources = remember(refreshKey) {
        listOf(
            IngestionSource(
                key = "notification_listener",
                title = "Notificări bancare",
                description = "Solomon citește notificările de la BT Pay, George, ING, Revolut etc. Acoperă ~70% din plăți.",
                coverage = "70%",
                coveragePercent = 70,
                isGranted = { notificationAccessGranted() },
                onEnable = { ctx ->
                    ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            ),
            IngestionSource(
                key = "share",
                title = "Partajare (Share Intent)",
                description = "Poți partaja orice receipt din BT Pay, George, Google Pay etc. → Solomon. Acoperă ~5% extra.",
                coverage = "5%",
                coveragePercent = 5,
                isGranted = { true },
                onEnable = null
            ),
            IngestionSource(
                key = "email",
                title = "Email-uri tranzacții",
                description = "Solomon citește email-urile de la Stripe, PayPal, Revolut, Wise, BT etc. prin forwarding. Acoperă ~10%.",
                coverage = "10%",
                coveragePercent = 10,
                isGranted = { true },
                isConfigured = { true },
                onEnable = null
            ),
            IngestionSource(
                key = "manual",
                title = "Intrare manuală",
                description = "Adaugă tranzacții manual din Chat sau din Portofel. Mereu disponibil.",
                coverage = "fallback",
                coveragePercent = 0,
                isGranted = { true },
                onEnable = null
            )
        )
    }

    val enabledCount = sources.count { it.isGranted() }
    val totalCoverage = sources.filter { it.isGranted() }.sumOf { it.coveragePercent }.coerceAtMost(85).toString() + "%"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Surse de date", color = SolomonColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { SolBackButton(onClick = onBack) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = SolomonColors.Background)
            )
        },
        containerColor = SolomonColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SolSpacing.screenHorizontal, vertical = SolSpacing.base)
        ) {
            HealthHeroCard(enabledCount = enabledCount, totalSources = sources.size, totalCoverage = totalCoverage)
            Spacer(Modifier.height(SolSpacing.lg))

            SolSectionHeaderRow("SURSE ACTIVE", meta = "$enabledCount / ${sources.size}")
            Spacer(Modifier.height(SolSpacing.sm))
            SolListCard {
                sources.forEachIndexed { idx, source ->
                    SourceRow(
                        source = source,
                        onEnable = { source.onEnable?.invoke(context); refreshKey++ }
                    )
                    if (idx < sources.lastIndex) SolHairlineDivider()
                }
            }

            Spacer(Modifier.height(SolSpacing.lg))
            PrivacyNote()
            Spacer(Modifier.height(SolSpacing.xl))
        }
    }
}

@Composable
private fun HealthHeroCard(enabledCount: Int, totalSources: Int, totalCoverage: String) {
    SolHeroCard(accent = if (enabledCount >= 3) SolAccent.Mint else if (enabledCount >= 1) SolAccent.Amber else SolAccent.Rose) {
        Column(verticalArrangement = Arrangement.spacedBy(SolSpacing.xs)) {
            Text("ACOPERIRE TOTALĂ", color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp)
            Text(totalCoverage, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "$enabledCount din $totalSources surse active. Mai multe surse = mai multe tranzacții detectate automat.",
                color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun SourceRow(source: IngestionSource, onEnable: () -> Unit) {
    val granted = source.isGranted()
    val accent = if (granted) SolAccent.Mint else SolAccent.Amber
    val statusColor = if (granted) SolomonColors.Primary else SolomonColors.Amber
    val statusText = if (granted) "ACTIV" else "INACTIV"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !granted && source.onEnable != null) { onEnable() }
            .padding(SolSpacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SolSpacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.color.copy(alpha = 0.15f))
                .border(1.dp, accent.color.copy(alpha = 0.30f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (granted) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                tint = accent.color,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SolSpacing.xs)) {
                Text(source.title, color = SolomonColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(source.coverage, color = SolomonColors.TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(2.dp))
            Text(source.description, color = SolomonColors.TextTertiary, fontSize = 12.sp, lineHeight = 16.sp)
        }
        if (!granted) {
            Text(
                statusText,
                color = statusColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun PrivacyNote() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SolRadius.lg))
            .background(SolomonColors.Surface)
            .border(1.dp, SolomonColors.Hairline, RoundedCornerShape(SolRadius.lg))
            .padding(SolSpacing.base),
        verticalArrangement = Arrangement.spacedBy(SolSpacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SolSpacing.xs)) {
            Text("🛡️", fontSize = 16.sp)
            Text("Confidențialitate", color = SolomonColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            "Toate datele sunt procesate local pe telefon. Solomon nu trimite tranzacțiile tale pe niciun server. Importul public folosește notificări bancare, partajare manuală și email forwarding. Importul SMS rămâne doar pentru build-uri interne/debug, nu pentru versiunea Google Play.",
            color = SolomonColors.TextSecondary, fontSize = 12.sp, lineHeight = 18.sp
        )
    }
}
