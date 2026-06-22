package ro.solomon.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ro.solomon.app.ui.components.*
import ro.solomon.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDownloadScreen(onClose: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modelul AI", color = SolomonColors.Primary) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Înapoi", tint = SolomonColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SolomonColors.Background)
            )
        },
        containerColor = SolomonColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(SolSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(SolSpacing.base)
        ) {
            SolHeroCard(accent = SolAccent.Mint, badge = "MISTRAL") {
                SolHeroLabel("MODEL ACTIV · CLOUD EU")
                Spacer(Modifier.height(SolSpacing.sm))
                Text("mistral-small-latest", color = SolomonColors.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(SolSpacing.xs))
                Text("EU (Paris) · GDPR-native · ISO 27001", color = SolomonColors.TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(SolSpacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
                    SolChip("~0.10 EUR/1M tok", accent = SolAccent.Mint)
                    SolChip("Tool calling", accent = SolAccent.Mint)
                    SolChip("128k ctx", accent = SolAccent.Mint)
                }
            }

            SolInsightCard(label = "Solomon · Info", timestamp = "privacy", accent = SolAccent.Mint) {
                Text(
                    "Folosim Mistral prin API (UE, GDPR). Când e activ, conversațiile și comenzile de tip „adaugă chirie 1500 RON\" sunt procesate în cloud-ul Mistral din Paris, iar datele personale (nume, IBAN, card, telefon, email) sunt anonimizate înainte de trimitere. Fără cheie sau cu Mistral dezactivat, Solomon folosește un răspuns-șablon local, fără niciun apel în rețea.",
                    color = SolomonColors.TextSecondary,
                    fontSize = 14.sp
                )
            }

            SolSectionHeaderRow("Modele disponibile", "1 acum activ")
            SolListCard {
                ModelRow("Mistral Small (latest)", "UE · $0.10/$0.30 per 1M · recomandat", true, SolAccent.Mint)
                SolHairlineDivider()
                ModelRow("Mistral Large (latest)", "UE · premium · mai scump, mai bun", false, SolAccent.Blue)
                SolHairlineDivider()
                ModelRow("Ministral 3B (latest)", "UE · cel mai ieftin · edge (cloud)", false, SolAccent.Amber)
                SolHairlineDivider()
                ModelRow("Șablon local", "Fără rețea · fallback când Mistral e oprit", false, SolAccent.Violet)
            }

            Spacer(Modifier.height(SolSpacing.lg))
            Text(
                "Datele tale rămân în UE. Setările Mistral se configurează din Setări → Model lingvistic → Mistral AI.",
                color = SolomonColors.TextTertiary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ModelRow(name: String, subtitle: String, active: Boolean, accent: SolAccent) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(SolSpacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SolSpacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accent.color.copy(alpha = 0.18f))
                .border(1.dp, accent.color.copy(alpha = 0.30f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("✨", color = accent.color, fontSize = 16.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = SolomonColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = SolomonColors.TextTertiary, fontSize = 11.sp)
        }
        if (active) SolChip("ACTIV", accent = SolAccent.Mint)
        else SolChip("disponibil", accent = accent)
    }
}
