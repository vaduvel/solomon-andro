package ro.solomon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
fun MeshBackground(
    topLeftAccent: SolAccent = SolAccent.Mint,
    midRightAccent: SolAccent = SolAccent.Blue,
    bottomLeftAccent: SolAccent = SolAccent.Violet,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(SolomonColors.Background)) {
        Box(
            modifier = Modifier
                .size(380.dp)
                .blur(60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            topLeftAccent.color.copy(alpha = 0.18f),
                            topLeftAccent.color.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    )
                )
                .offset(x = (-100).dp, y = (-120).dp)
        )
        Box(
            modifier = Modifier
                .size(320.dp)
                .blur(50.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            midRightAccent.color.copy(alpha = 0.12f),
                            midRightAccent.color.copy(alpha = 0.02f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.CenterEnd)
                .offset(x = (-100).dp, y = 100.dp)
        )
        Box(
            modifier = Modifier
                .size(280.dp)
                .blur(50.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            bottomLeftAccent.color.copy(alpha = 0.10f),
                            bottomLeftAccent.color.copy(alpha = 0.02f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.BottomStart)
                .offset(x = (-80).dp, y = (-100).dp)
        )
    }
}

@Composable
fun SolChip(
    label: String,
    accent: SolAccent = SolAccent.Mint,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(SolRadius.pill))
            .background(accent.color.copy(alpha = 0.12f))
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
            .padding(horizontal = 4.dp, vertical = SolSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            color = SolomonColors.TextTertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp
        )
        meta?.let {
            Text(
                text = it,
                color = SolomonColors.TextTertiary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun SolHairlineDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .padding(horizontal = SolSpacing.base)
            .background(Color.White.copy(alpha = 0.04f))
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
            .clip(RoundedCornerShape(SolRadius.xl))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.04f),
                        Color.White.copy(alpha = 0.015f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(SolRadius.xl))
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
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.04f),
                        Color.White.copy(alpha = 0.02f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(SolRadius.lg))
            .padding(SolSpacing.base),
        verticalArrangement = Arrangement.spacedBy(SolSpacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SolSpacing.sm)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                accent.color.copy(alpha = 0.18f),
                                accent.color.copy(alpha = 0.06f)
                            )
                        )
                    )
                    .border(1.dp, accent.color.copy(alpha = 0.25f), RoundedCornerShape(11.dp)),
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
                Text(label.uppercase(), color = accent.color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                timestamp?.let {
                    Text(it, color = Color.White.copy(alpha = 0.35f), fontSize = 11.sp)
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
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .blur(30.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.color.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopEnd)
                .offset(x = 20.dp, y = (-10).dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accent.color.copy(alpha = 0.08f),
                            accent.color.copy(alpha = 0.02f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    )
                )
                .border(1.dp, accent.color.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
                .shadow(20.dp, ambientColor = Color.Black.copy(alpha = 0.5f), spotColor = Color.Black.copy(alpha = 0.5f))
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
}

@Composable
fun SolHeroLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = Color.White.copy(alpha = 0.45f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.7.sp,
        modifier = modifier
    )
}

@Composable
fun SolHeroAmount(
    amount: String,
    decimals: String? = null,
    currency: String = "RON",
    accent: SolAccent = SolAccent.Mint,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Text(
            text = amount,
            color = SolomonColors.TextPrimary,
            fontSize = 42.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-1.5).sp
        )
        decimals?.let {
            Text(
                text = it,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = currency,
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
        )
    }
}

@Composable
fun SolPrimaryButton(
    title: String,
    accent: SolAccent = SolAccent.Mint,
    fullWidth: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = when (accent) {
        SolAccent.Mint -> SolomonColors.Background
        SolAccent.Blue -> Color.White
        SolAccent.Amber -> Color(0xFF451A03)
        SolAccent.Rose -> Color.White
        SolAccent.Violet -> Color.White
        else -> Color.White
    }
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = accent.color,
            contentColor = textColor
        ),
        shape = RoundedCornerShape(if (fullWidth) 14.dp else 9.dp),
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .shadow(
                elevation = 12.dp,
                ambientColor = accent.color.copy(alpha = 0.4f),
                spotColor = accent.color.copy(alpha = 0.4f)
            )
    ) {
        Text(
            text = title,
            fontSize = if (fullWidth) 14.sp else 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SolSecondaryButton(
    title: String,
    fullWidth: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(if (fullWidth) 14.dp else 9.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White.copy(alpha = 0.7f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(if (fullWidth) 14.dp else 9.dp))
    ) {
        Text(
            text = title,
            fontSize = if (fullWidth) 14.sp else 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SolStatCard(
    label: String,
    name: String,
    value: String,
    meta: String? = null,
    metaAccent: SolAccent? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconAccent: SolAccent,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.04f),
                        Color.White.copy(alpha = 0.015f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                iconAccent.color.copy(alpha = 0.18f),
                                iconAccent.color.copy(alpha = 0.06f)
                            )
                        )
                    )
                    .border(1.dp, iconAccent.color.copy(alpha = 0.20f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconAccent.color, modifier = Modifier.size(14.dp))
            }
            Text(
                text = label.uppercase(),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(name, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        Text(
            text = value,
            color = SolomonColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        meta?.let {
            Text(
                text = it,
                color = metaAccent?.color ?: Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun SolPill(
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(SolRadius.pill))
            .background(
                if (isActive) SolomonColors.Primary.copy(alpha = 0.12f)
                else Color.White.copy(alpha = 0.04f)
            )
            .border(
                1.dp,
                if (isActive) SolomonColors.Primary.copy(alpha = 0.30f)
                else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(SolRadius.pill)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (isActive) SolomonColors.PrimaryLight else Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SolAllocationBar(
    segments: List<AllocationSegment>,
    height: Int = 7,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(SolRadius.pill))
            .background(Color.White.copy(alpha = 0.04f)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        segments.forEach { seg ->
            Box(
                modifier = Modifier
                    .weight(seg.fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(SolRadius.pill))
                    .background(seg.accent.color)
            )
        }
    }
}

data class AllocationSegment(
    val fraction: Float,
    val accent: SolAccent
)

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
            .background(Color.White.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(clamped)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(accent.color, accent.color)
                    )
                )
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
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("\u2039", color = SolomonColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
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
            Text("\u2713", color = SolomonColors.Primary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
    icon: String = "\u2713",
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
