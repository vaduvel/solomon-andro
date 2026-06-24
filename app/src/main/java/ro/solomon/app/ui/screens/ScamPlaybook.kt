package ro.solomon.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ro.solomon.app.ui.components.*
import ro.solomon.app.ui.theme.*

private data class PlaybookStep(
    val index: Int,
    val title: String,
    val detail: String
)

private val scamSteps = listOf(
    PlaybookStep(
        1, "Blochează cardul acum",
        "Deschide aplicația băncii (Revolut, George, BT Pay…) și blochează / îngheață cardul. Durează câteva secunde și oprește alte tranzacții."
    ),
    PlaybookStep(
        2, "Sună banca la numărul oficial",
        "Folosește numărul de pe spatele cardului sau din aplicația oficială a băncii. Spune că ai o tranzacție pe care nu o recunoști."
    ),
    PlaybookStep(
        3, "Cere contestarea tranzacției (chargeback)",
        "Solicită deschiderea unei dispute / sesizări de fraudă. Întreabă explicit de procedura de chargeback și de termenul de soluționare."
    ),
    PlaybookStep(
        4, "Schimbă parolele și pornește 2FA",
        "Dacă bănuiești că ți-a fost accesat contul, schimbă parola de la aplicația băncii și de la email și activează autentificarea în doi pași."
    ),
    PlaybookStep(
        5, "Nu da niciodată codul (OTP) nimănui",
        "Banca NU îți cere codul prin telefon, SMS sau email. Dacă cineva ți-l cere, e fraudă — închide și sună tu banca."
    ),
    PlaybookStep(
        6, "Păstrează dovezile",
        "Salvează SMS-urile, capturile și detaliile tranzacției. Îți vor fi cerute la reclamație."
    )
)

@Composable
fun ScamPlaybookDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SolomonColors.Surface,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(SolSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(SolSpacing.base)
            ) {
                Text("Ce faci acum", color = SolomonColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Pași rapizi dacă o tranzacție pare suspectă. Solomon nu îți poate bloca cardul în locul tău — îți arată exact ce să faci.",
                    color = SolomonColors.TextSecondary, fontSize = 14.sp
                )
                SolListCard {
                    scamSteps.forEachIndexed { i, step ->
                        if (i > 0) SolHairlineDivider()
                        StepRow(step)
                    }
                }
                SolInsightCard(label = "Solomon · Siguranță", accent = SolAccent.Rose) {
                    Text(
                        "Dacă ai dat din greșeală un cod sau date de card, tratează-l ca urgență: blochează cardul și sună banca imediat.",
                        color = SolomonColors.TextSecondary, fontSize = 14.sp
                    )
                }
                SolPrimaryButton(title = "Am înțeles", accent = SolAccent.Rose, fullWidth = true, onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun StepRow(step: PlaybookStep) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(SolSpacing.base),
        horizontalArrangement = Arrangement.spacedBy(SolSpacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SolomonColors.Rose.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text("${step.index}", color = SolomonColors.Rose, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(step.title, color = SolomonColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(SolSpacing.xs))
            Text(step.detail, color = SolomonColors.TextTertiary, fontSize = 13.sp)
        }
    }
}
