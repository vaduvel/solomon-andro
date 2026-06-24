package ro.solomon.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import ro.solomon.app.R

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val SpaceGroteskGF = GoogleFont("Space Grotesk")
private val SpaceMonoGF = GoogleFont("Space Mono")

private val SpaceGrotesk = FontFamily(
    Font(googleFont = SpaceGroteskGF, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = SpaceGroteskGF, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = SpaceGroteskGF, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = SpaceGroteskGF, fontProvider = googleFontProvider, weight = FontWeight.Bold)
)

/** Monospace face for labels, figures and timestamps (per design spec). */
val SpaceMono = FontFamily(
    Font(googleFont = SpaceMonoGF, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = SpaceMonoGF, fontProvider = googleFontProvider, weight = FontWeight.Bold)
)

private val SolomonColorScheme = darkColorScheme(
    primary = SolomonColors.Primary,
    onPrimary = SolomonColors.OnPrimary,
    primaryContainer = SolomonColors.PrimaryDeep,
    onPrimaryContainer = SolomonColors.TextPrimary,
    secondary = SolomonColors.Blue,
    onSecondary = SolomonColors.TextPrimary,
    tertiary = SolomonColors.Amber,
    background = SolomonColors.Background,
    onBackground = SolomonColors.TextPrimary,
    surface = SolomonColors.Surface,
    onSurface = SolomonColors.TextPrimary,
    surfaceVariant = SolomonColors.SurfaceVariant,
    onSurfaceVariant = SolomonColors.TextSecondary,
    surfaceTint = SolomonColors.Primary,
    outline = SolomonColors.Outline,
    outlineVariant = SolomonColors.OutlineVariant,
    error = SolomonColors.Error,
    onError = SolomonColors.TextPrimary,
)

private val SolomonTypography = Typography(
    displayLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 56.sp, letterSpacing = (-1.5).sp),
    displayMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 42.sp, letterSpacing = (-1.2).sp),
    displaySmall = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = (-0.8).sp),
    headlineLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    headlineSmall = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    titleSmall = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    bodyLarge = TextStyle(fontFamily = SpaceGrotesk, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = SpaceGrotesk, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = SpaceGrotesk, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.5.sp)
)

@Composable
fun SolomonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SolomonColorScheme,
        typography = SolomonTypography,
        content = content
    )
}
