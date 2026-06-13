package ro.solomon.app.ui.shortcuts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
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
import androidx.lifecycle.viewmodel.compose.viewModel
import ro.solomon.app.ui.components.EmptyStateView
import ro.solomon.app.ui.components.SolAccent
import ro.solomon.app.ui.components.SolBackButton
import ro.solomon.app.ui.components.SolHairlineDivider
import ro.solomon.app.ui.components.SolHeroCard
import ro.solomon.app.ui.components.SolInsightCard
import ro.solomon.app.ui.components.SolListCard
import ro.solomon.app.ui.components.SolSectionHeaderRow
import ro.solomon.app.ui.theme.SolRadius
import ro.solomon.app.ui.theme.SolSpacing
import ro.solomon.app.ui.theme.SolomonColors

data class ShortcutStep(val number: Int, val title: String, val detail: String, val icon: String)

class ShortcutSetupViewModel : androidx.lifecycle.ViewModel() {
    val steps: List<ShortcutStep> = listOf(
        ShortcutStep(1, "Deschide Automatizare", "Alege Tasker, Macrodroid sau aplicația ta preferată de automatizare Android.", "settings"),
        ShortcutStep(2, "Creează profil nou", "Adaugă un trigger pe notificare bancară (ex: 'Tranzacție nouă') sau pe o locație specifică (ex: magazin).", "add"),
        ShortcutStep(3, "Acțiune: Trimite la Solomon", "Copiază textul notificării și trimite-l către Solomon prin acțiunea Send Intent / Share.", "share"),
        ShortcutStep(4, "Mapează câmpurile", "Solomon extrage automat suma, comerciantul și data din text. Dacă ceva lipsește, întreabă în chat.", "map"),
        ShortcutStep(5, "Activează", "Salvează și testează. Tranzacțiile vor apărea automat în Portofel.", "bolt")
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutSetupScreen(onBack: () -> Unit, vm: ShortcutSetupViewModel = viewModel()) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SOLOMON · AUTOMATIZARE", color = SolomonColors.Primary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp)
                        Text("Conectează plăți automate", color = SolomonColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
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
            HeroCard()
            Spacer(Modifier.height(SolSpacing.lg))

            WhyInsight()
            Spacer(Modifier.height(SolSpacing.lg))

            SolSectionHeaderRow("PAȘII", meta = "${vm.steps.size} pași · 3 minute")
            Spacer(Modifier.height(SolSpacing.sm))
            SolListCard {
                vm.steps.forEachIndexed { idx, step ->
                    StepRow(step)
                    if (idx < vm.steps.lastIndex) SolHairlineDivider()
                }
            }

            Spacer(Modifier.height(SolSpacing.lg))
            TroubleshootingInsight()
            Spacer(Modifier.height(SolSpacing.lg))

            SupportedAppsCard()

            Spacer(Modifier.height(SolSpacing.lg))
            Button(
                onClick = { /* Open system notification listener settings */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SolomonColors.Primary, contentColor = SolomonColors.OnPrimary),
                shape = RoundedCornerShape(SolRadius.md)
            ) {
                Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(SolSpacing.xs))
                Text("Deschide Setări notificări Android", fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Mai târziu", color = SolomonColors.TextSecondary)
            }
            Spacer(Modifier.height(SolSpacing.xl))
        }
    }
}

@Composable
private fun HeroCard() {
    SolHeroCard(accent = SolAccent.Blue) {
        Column(verticalArrangement = Arrangement.spacedBy(SolSpacing.xs)) {
            Text("AUTOMATIZARE NATIVĂ · ANDROID", color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp)
            Text("Prind plățile singure", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Folosește puterea Android — Notification Listener + Tasker/Macrodroid — ca Solomon să primească automat fiecare plată, fără introducere manuală.",
                color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun WhyInsight() {
    SolInsightCard(icon = "🔒", label = "DE CE?", timestamp = "100% local", accent = SolAccent.Blue) {
        Text(
            "Datele tale nu părăsesc telefonul. Solomon citește notificările prin API-ul oficial Android (NotificationListenerService) — nu interceptăm SMS sau alte aplicații. Tu controlezi exact ce partajezi.",
            color = SolomonColors.TextSecondary, fontSize = 13.sp, lineHeight = 18.sp
        )
    }
}

@Composable
private fun StepRow(step: ShortcutStep) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(SolSpacing.base),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(SolSpacing.md)
    ) {
        Text(
            text = step.number.toString(),
            color = SolomonColors.Primary,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(28.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(step.title, color = SolomonColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(step.detail, color = SolomonColors.TextTertiary, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SolomonColors.Primary.copy(alpha = 0.15f))
                .border(1.dp, SolomonColors.Primary.copy(alpha = 0.30f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            val icon = when (step.icon) {
                "settings" -> Icons.Filled.Settings
                "add" -> Icons.Filled.AutoAwesome
                "share" -> Icons.Filled.Notifications
                "map" -> Icons.Filled.CreditCard
                "bolt" -> Icons.Filled.Bolt
                else -> Icons.Filled.ChevronRight
            }
            Icon(icon, contentDescription = null, tint = SolomonColors.Primary, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun TroubleshootingInsight() {
    SolInsightCard(icon = "⚠", label = "DACĂ NU PRINDE NOTIFICĂRI", timestamp = "Verifică permisiunile", accent = SolAccent.Amber) {
        Text(
            "Multe aplicații de banking (BT Pay, George, Revolut) folosesc notificări protejate. Intră în Setări → Aplicații → [Banca ta] → Notificări și asigură-te că toate categoriile sunt activate. Apoi revino în Solomon.",
            color = SolomonColors.TextSecondary, fontSize = 13.sp, lineHeight = 18.sp
        )
    }
}

@Composable
private fun SupportedAppsCard() {
    SolSectionHeaderRow("COMPATIBIL", meta = "verificat 2025")
    Spacer(Modifier.height(SolSpacing.sm))
    SolListCard {
        listOf(
            "Banca Transilvania (BT Pay)" to "✓",
            "BCR (George)" to "✓",
            "ING Home'Bank" to "✓",
            "BRD" to "✓",
            "Revolut" to "✓",
            "Wise" to "✓",
            "Stripe (email)" to "✓",
            "PayPal (email)" to "✓"
        ).forEachIndexed { idx, (name, mark) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = SolSpacing.base, vertical = SolSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, color = SolomonColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(mark, color = SolomonColors.Primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            if (idx < 7) SolHairlineDivider()
        }
    }
}
