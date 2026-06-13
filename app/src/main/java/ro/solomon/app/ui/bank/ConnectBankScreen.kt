package ro.solomon.app.ui.bank

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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

data class BankInfo(val name: String, val bic: String, val enabled: Boolean, val accounts: Int, val expiresAt: String?)

class ConnectBankViewModel : androidx.lifecycle.ViewModel() {
    val connected: List<BankInfo> = emptyList()
    val availableBanks: List<BankInfo> = listOf(
        BankInfo("Banca Transilvania", "BTRLRO22", enabled = false, accounts = 0, expiresAt = null),
        BankInfo("BCR", "RNCBROBU", enabled = false, accounts = 0, expiresAt = null),
        BankInfo("ING Bank", "INGBROBU", enabled = false, accounts = 0, expiresAt = null),
        BankInfo("BRD", "BRDEROBU", enabled = false, accounts = 0, expiresAt = null),
        BankInfo("Raiffeisen Bank", "RZBRROBU", enabled = false, accounts = 0, expiresAt = null),
        BankInfo("UniCredit Bank", "BACXROBU", enabled = false, accounts = 0, expiresAt = null),
        BankInfo("CEC Bank", "CECEROBU", enabled = false, accounts = 0, expiresAt = null),
        BankInfo("Alpha Bank", "BUCUROBU", enabled = false, accounts = 0, expiresAt = null)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectBankScreen(onBack: () -> Unit, vm: ConnectBankViewModel = viewModel()) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Conectează banca", color = SolomonColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
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
            ComingSoonHero()
            Spacer(Modifier.height(SolSpacing.lg))

            SolSectionHeaderRow("BĂNCI SUPORTATE (Q1 2025)", meta = "${vm.availableBanks.size} bănci")
            Spacer(Modifier.height(SolSpacing.sm))
            SolListCard {
                vm.availableBanks.forEachIndexed { idx, bank ->
                    AvailableBankRow(bank, onClick = { })
                    if (idx < vm.availableBanks.lastIndex) SolHairlineDivider()
                }
            }

            Spacer(Modifier.height(SolSpacing.lg))
            WhyNote()
            Spacer(Modifier.height(SolSpacing.base))
            CurrentCoverageNote()
            Spacer(Modifier.height(SolSpacing.xl))
        }
    }
}

@Composable
private fun ComingSoonHero() {
    SolHeroCard(accent = SolAccent.Blue) {
        Column(verticalArrangement = Arrangement.spacedBy(SolSpacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Text("OPEN BANKING · Q1 2025", color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp)
            }
            Text("Conectare directă cu banca ta", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "În Q1 2025 lansăm Open Banking prin Enable Banking (GDPR-native, Paris). Te vei putea conecta la orice bancă RO cu un singur tap — autorizare prin OAuth2, fără ca Solomon să vadă vreodată parola ta.",
                color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun WhyNote() {
    SolInsightCard(icon = "🛡", label = "DE CE PSD2?", timestamp = "Lege UE", accent = SolAccent.Mint) {
        Text(
            "PSD2 e lege europeană care obligă băncile să ofere acces la conturile tale către terți autorizați. Asta înseamnă că Solomon va putea citi tranzacțiile tale direct de la BT, BCR, ING — cu acordul tău, prin OAuth2 securizat.",
            color = SolomonColors.TextSecondary, fontSize = 13.sp, lineHeight = 18.sp
        )
    }
}

@Composable
private fun CurrentCoverageNote() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SolRadius.lg))
            .background(SolomonColors.Surface)
            .border(1.dp, SolomonColors.Primary.copy(alpha = 0.30f), RoundedCornerShape(SolRadius.lg))
            .padding(SolSpacing.base),
        verticalArrangement = Arrangement.spacedBy(SolSpacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SolSpacing.xs)) {
            Text("✅", fontSize = 16.sp)
            Text("Acum acoperim deja ~85%", color = SolomonColors.Primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            "Chiar fără Open Banking, Solomon capturează plățile prin:\n• Notificări bancare (BT Pay, George, ING, Revolut) — 70%\n• Partajare receipts (Google Pay, eMAG) — 5%\n• Email parsing / forwarding (Stripe, PayPal, Revolut) — 10%\n• Adăugare manuală — fallback fără permisiuni sensibile",
            color = SolomonColors.TextSecondary, fontSize = 12.sp, lineHeight = 18.sp
        )
        Text(
            "Open Banking va adăuga: sold real, IBAN, nume titular și sincronizare istoric 90+ zile.",
            color = SolomonColors.TextTertiary, fontSize = 12.sp
        )
    }
}

@Composable
private fun AvailableBankRow(bank: BankInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(SolSpacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SolSpacing.md)
    ) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(SolomonColors.SurfaceVariant), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = SolomonColors.TextSecondary, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(bank.name, color = SolomonColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(bank.bic, color = SolomonColors.TextTertiary, fontSize = 11.sp)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = SolomonColors.TextTertiary, modifier = Modifier.size(16.dp))
    }
}
