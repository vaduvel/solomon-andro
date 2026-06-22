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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ro.solomon.app.ui.theme.SolAccent
import ro.solomon.app.ui.components.SolBackButton
import ro.solomon.app.ui.components.SolHairlineDivider
import ro.solomon.app.ui.components.SolHeroCard
import ro.solomon.app.ui.components.SolInsightCard
import ro.solomon.app.ui.components.SolListCard
import ro.solomon.app.ui.components.SolSectionHeaderRow
import ro.solomon.app.ui.theme.SolRadius
import ro.solomon.app.ui.theme.SolSpacing
import ro.solomon.app.ui.theme.SolomonColors
import ro.solomon.core.enablebanking.ASPSP
import ro.solomon.core.enablebanking.BankConnection
import ro.solomon.core.enablebanking.BankConnectionService
import ro.solomon.core.enablebanking.EnableBankingClient
import ro.solomon.core.enablebanking.EnableBankingConfig

class ConnectBankViewModel : ViewModel() {
    var banks by mutableStateOf<List<ASPSP>>(emptyList())
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
    var configure by mutableStateOf(false)
        private set

    fun load() {
        if (!EnableBankingConfig.isConfigured) {
            loading = false
            configure = true
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            loading = true
            error = null
            try {
                banks = EnableBankingClient.listBanksRO().filter { it.beta != true }
            } catch (e: Exception) {
                error = e.message
            }
            loading = false
        }
    }

    val connected: List<BankConnection> get() = BankConnectionService.allConnections
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectBankScreen(onBack: () -> Unit, vm: ConnectBankViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.load() }
    val scope = rememberCoroutineScope()

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
            val msg = vm.error
            if (vm.configure) {
                NotConfiguredCard()
            } else if (vm.loading) {
                LoadingState()
            } else {
                ConnectedBanksSection(vm.connected, onDisconnect = { conn ->
                    scope.launch { BankConnectionService.disconnect(conn) }
                })
                Spacer(Modifier.height(SolSpacing.lg))

                if (msg != null) {
                    ErrorCard(msg)
                    Spacer(Modifier.height(SolSpacing.md))
                }

                SolSectionHeaderRow("BĂNCI DISPONIBILE", meta = "${vm.banks.size} bănci")
                Spacer(Modifier.height(SolSpacing.sm))

                if (vm.banks.isEmpty()) {
                    AvailableBanksFallback()
                } else {
                    SolListCard {
                        vm.banks.forEachIndexed { idx, bank ->
                            AvailableBankRow(bank, onClick = {
                                try {
                                    val url = BankConnectionService.startConnect(bank)
                                } catch (e: Exception) {
                                    vm.error = e.message
                                }
                            })
                            if (idx < vm.banks.lastIndex) SolHairlineDivider()
                        }
                    }
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
private fun NotConfiguredCard() {
    SolHeroCard(accent = SolAccent.Amber) {
        Column(verticalArrangement = Arrangement.spacedBy(SolSpacing.xs)) {
            Text("OPEN BANKING · SETUP", color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp)
            Text("Configurează Enable Banking", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text("Pentru a conecta băncile, adaugă APPLICATION_ID și cheia privată RSA 4096 în Setări → Configurare Enable Banking.",
                color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(SolSpacing.sm))
            Text("APP_ID + private key lipsă", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxWidth().padding(vertical = SolSpacing.xl), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = SolomonColors.Primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(SolSpacing.sm))
            Text("Încărc bănci suportate...", color = SolomonColors.TextTertiary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ErrorCard(msg: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(SolRadius.md))
        .background(SolomonColors.Error.copy(alpha = 0.15f)).padding(SolSpacing.base)) {
        Text(msg, color = SolomonColors.Error, fontSize = 12.sp)
    }
}

@Composable
private fun ConnectedBanksSection(connections: List<BankConnection>, onDisconnect: (BankConnection) -> Unit) {
    if (connections.isEmpty()) return
    SolSectionHeaderRow("BĂNCI CONECTATE", meta = "${connections.size}")
    Spacer(Modifier.height(SolSpacing.sm))
    SolListCard {
        connections.forEachIndexed { idx, conn ->
            ConnectedBankRow(conn, onDisconnect = { onDisconnect(conn) })
            if (idx < connections.lastIndex) SolHairlineDivider()
        }
    }
    Spacer(Modifier.height(SolSpacing.md))
}

@Composable
private fun ConnectedBankRow(conn: BankConnection, onDisconnect: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(SolSpacing.base), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SolSpacing.md)) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
            .background(SolomonColors.Primary.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = SolomonColors.Primary, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(conn.aspspName, color = SolomonColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("${conn.accounts.size} conturi · ${conn.accounts.joinToString { a -> a.currency }}", color = SolomonColors.TextTertiary, fontSize = 11.sp)
        }
        TextButton(onClick = onDisconnect) { Text("Deconectează", color = SolomonColors.Error, fontSize = 12.sp) }
    }
}

@Composable
private fun AvailableBankRow(bank: ASPSP, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onClick() }.padding(SolSpacing.base),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SolSpacing.md)) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(SolomonColors.SurfaceVariant), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = SolomonColors.TextSecondary, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(bank.name, color = SolomonColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(bank.country, color = SolomonColors.TextTertiary, fontSize = 11.sp)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = SolomonColors.TextTertiary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun AvailableBanksFallback() {
    SolListCard {
        val fallback = listOf("Banca Transilvania", "BCR", "ING Bank", "BRD", "Raiffeisen Bank", "UniCredit Bank", "CEC Bank", "Alpha Bank", "Revolut", "Garanti Bank", "Libra Bank")
        fallback.forEachIndexed { idx, name ->
            Row(Modifier.fillMaxWidth().padding(SolSpacing.base), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SolSpacing.md)) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(SolomonColors.SurfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = SolomonColors.TextTertiary, modifier = Modifier.size(18.dp))
                }
                Text(name, color = SolomonColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            if (idx < fallback.lastIndex) SolHairlineDivider()
        }
    }
}

@Composable
private fun WhyNote() {
    SolInsightCard(label = "DE CE PSD2?", timestamp = "Lege UE", accent = SolAccent.Mint) {
        Text("PSD2 e lege europeană care obligă băncile să ofere acces la conturile tale către terți autorizați. Prin Enable Banking (GDPR-native, Paris), Solomon poate citi tranzacțiile direct de la banca ta — cu acordul tău, prin OAuth2.",
            color = SolomonColors.TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun CurrentCoverageNote() {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(SolRadius.lg))
        .background(SolomonColors.Surface)
        .border(1.dp, SolomonColors.Primary.copy(alpha = 0.30f), RoundedCornerShape(SolRadius.lg))
        .padding(SolSpacing.base), verticalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SolSpacing.xs)) {
            Text("\u2705", fontSize = 16.sp)
            Text("Fără Open Banking: ~85% acoperire", color = SolomonColors.Primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Text("Notificări bancare (BT Pay, George, ING, Revolut, Google Wallet) — 70%\n" +
            "Share intent + Email parsing — 15%\n" +
            "Adăugare manuală — fallback\n\n" +
            "Open Banking adaugă: solduri reale, IBAN, istoric 90+ zile, sincronizare live.",
            color = SolomonColors.TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
    }
}
