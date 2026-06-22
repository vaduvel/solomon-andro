package ro.solomon.app.ui.bank

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ro.solomon.app.services.EnableBankingConfigStore
import ro.solomon.app.ui.theme.SolSpacing
import ro.solomon.app.ui.theme.SolomonColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnableBankingSetupScreen(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val savedAppId by EnableBankingConfigStore.appIdFlow().collectAsState(initial = "")
    val savedKey by EnableBankingConfigStore.privateKeyFlow().collectAsState(initial = "")
    var appId by remember(savedAppId) { mutableStateOf(savedAppId) }
    var privateKey by remember(savedKey) { mutableStateOf(savedKey) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enable Banking") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Înapoi")
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
            verticalArrangement = Arrangement.spacedBy(SolSpacing.md)
        ) {
            Text(
                "Enable Banking este agregatorul PSD2 folosit de Solomon pentru Open Banking. " +
                "Ai nevoie de cont pe enablebanking.com/cp pentru a genera APPLICATION_ID și o cheie privată RSA 4096.",
                color = SolomonColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )

            Text("Pas 1: Creează cont pe enablebanking.com/cp", fontWeight = FontWeight.SemiBold, color = SolomonColors.Primary, fontSize = 13.sp)
            Text("Pas 2: Generează o aplicație REST (Restricted Production)", color = SolomonColors.TextTertiary, fontSize = 12.sp)
            Text("Pas 3: Copiază APPLICATION_ID (UUID) și cheia privată PEM aici:", color = SolomonColors.TextTertiary, fontSize = 12.sp)

            OutlinedTextField(
                value = appId,
                onValueChange = { appId = it },
                label = { Text("APPLICATION_ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", fontSize = 12.sp) }
            )

            OutlinedTextField(
                value = privateKey,
                onValueChange = { privateKey = it },
                label = { Text("Cheie privată RSA (PEM)") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                maxLines = 12,
                placeholder = { Text("-----BEGIN PRIVATE KEY-----", fontSize = 12.sp) }
            )

            Button(
                onClick = {
                    scope.launch {
                        EnableBankingConfigStore.save(appId.trim(), privateKey.trim())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SolomonColors.Primary)
            ) { Text("Salvează credențiale") }

            HorizontalDivider(color = SolomonColors.Hairline)

            Text(
                "Cheia PEM rămâne doar pe dispozitivul tău, stocată în DataStore criptat. " +
                "Solomon nu trimite niciodată cheia privată pe rețea — doar semnează JWT-uri local.",
                color = SolomonColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
