package ro.solomon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ro.solomon.app.ui.theme.SolAccent
import ro.solomon.app.ui.theme.SolRadius
import ro.solomon.app.ui.theme.SolSpacing
import ro.solomon.app.ui.theme.SolomonColors

@Composable
fun SolChip(
    label: String,
    accent: SolAccent = SolAccent.Mint,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(SolRadius.pill))
            .background(accent.color.copy(alpha = 0.10f))
            .border(1.dp, accent.color.copy(alpha = 0.25f), RoundedCornerShape(SolRadius.pill))
            .padding(horizontal = SolSpacing.sm, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = accent.color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SolSectionHeaderRow(
    title: String,
    meta: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SolSpacing.base, vertical = SolSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            color = SolomonColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp
        )
        meta?.let {
            Text(
                text = it,
                color = SolomonColors.TextTertiary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun SolHairlineDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SolomonColors.Hairline)
    )
}

@Composable
fun SolListCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SolRadius.lg))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.04f),
                        Color.White.copy(alpha = 0.02f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(SolRadius.lg))
    ) {
        content()
    }
}

@Composable
fun SolInsightCard(
    label: String,
    timestamp: String? = null,
    accent: SolAccent = SolAccent.Mint,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SolRadius.lg))
            .background(SolomonColors.Surface)
            .border(1.dp, SolomonColors.Hairline, RoundedCornerShape(SolRadius.lg))
            .padding(SolSpacing.base),
        verticalArrangement = Arrangement.spacedBy(SolSpacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.color.copy(alpha = 0.12f))
                    .border(1.dp, accent.color.copy(alpha = 0.30f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = accent.color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(label.uppercase(), color = accent.color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.0.sp)
                timestamp?.let {
                    Text(it, color = SolomonColors.TextTertiary, fontSize = 10.sp)
                }
            }
        }
        content()
    }
}

@Composable
fun SolHeroCard(
    accent: SolAccent = SolAccent.Mint,
    badge: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SolRadius.xl))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        accent.color.copy(alpha = 0.18f),
                        accent.color.copy(alpha = 0.05f)
                    )
                )
            )
            .border(1.dp, accent.color.copy(alpha = 0.30f), RoundedCornerShape(SolRadius.xl))
            .padding(SolSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SolSpacing.sm)
    ) {
        if (badge != null) {
            Box(modifier = Modifier.align(Alignment.End)) {
                SolChip(badge, accent)
            }
        }
        content()
    }
}

@Composable
fun SolHeroLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = SolomonColors.TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        modifier = modifier
    )
}

@Composable
fun SolLoadingIndicator(accent: SolAccent = SolAccent.Mint, label: String? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(SolSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(SolSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = accent.color)
        label?.let {
            Text(it, color = SolomonColors.TextTertiary, fontSize = 13.sp)
        }
    }
}

@Composable
fun SolLinearProgress(
    progress: Float,
    accent: SolAccent = SolAccent.Mint,
    height: Int = 6,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.06f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(clamped)
                .background(accent.color)
        )
    }
}

@Composable
fun SolProgressRing(
    progress: Float,
    label: String? = null,
    size: Int = 120,
    lineWidth: Int = 9,
    accent: SolAccent = SolAccent.Mint,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            color = Color.White.copy(alpha = 0.06f),
            strokeWidth = lineWidth.dp,
            modifier = Modifier.fillMaxSize()
        )
        CircularProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            color = accent.color,
            strokeWidth = lineWidth.dp,
            modifier = Modifier.fillMaxSize()
        )
        if (label != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                    color = SolomonColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = label.uppercase(),
                    color = SolomonColors.TextTertiary,
                    fontSize = 9.sp,
                    letterSpacing = 1.0.sp
                )
            }
        }
    }
}

@Composable
fun SolBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("‹", color = SolomonColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun IngestionToast(
    title: String,
    detail: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SolSpacing.base)
            .clip(RoundedCornerShape(SolRadius.pill))
            .background(SolomonColors.SurfaceVariant)
            .border(1.dp, SolomonColors.Primary.copy(alpha = 0.25f), RoundedCornerShape(SolRadius.pill))
            .padding(horizontal = SolSpacing.base, vertical = SolSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SolSpacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(50))
                .background(SolomonColors.Primary.copy(alpha = 0.15f))
                .border(1.dp, SolomonColors.Primary.copy(alpha = 0.40f), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", color = SolomonColors.Primary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SolomonColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            detail?.let {
                Text(it, color = SolomonColors.TextTertiary, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun EmptyStateView(
    icon: String = "✓",
    title: String,
    subtitle: String? = null,
    accent: SolAccent = SolAccent.Mint,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SolRadius.lg))
            .background(SolomonColors.Surface)
            .border(1.dp, SolomonColors.Hairline, RoundedCornerShape(SolRadius.lg))
            .padding(SolSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(SolSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accent.color.copy(alpha = 0.15f))
                .border(1.dp, accent.color.copy(alpha = 0.30f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, color = accent.color, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(title, color = SolomonColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        subtitle?.let {
            Text(it, color = SolomonColors.TextTertiary, fontSize = 14.sp)
        }
    }
}
