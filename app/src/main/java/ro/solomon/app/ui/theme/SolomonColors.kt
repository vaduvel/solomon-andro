package ro.solomon.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Solomon Design System — nativ Android.
 *
 * Accent: Volt #C5E84B.
 * Dark theme only (la fel ca iOS).
 */
object SolomonColors {
    // Volt brand
    val Primary = Color(0xFFC5E84B)           // #C5E84B — accent / CTA
    val PrimaryDeep = Color(0xFF9AB83C)        // #9AB83C — gradient end
    val PrimaryLight = Color(0xFFD8F082)       // #D8F082 — Volt light
    val OnPrimary = Color(0xFF0B0C0E)          // dark text pe Volt

    // Background layers
    val Background = Color(0xFF0B0C0E)         // #0B0C0E — canvas
    val Surface = Color(0xFF16181C)            // #16181C — card default
    val SurfaceVariant = Color(0xFF1C1F23)     // raised
    val SurfaceElevated = Color(0xFF22252A)    // modal/sheet

    // Text
    val TextPrimary = Color(0xFFF2EEE6)        // #F2EEE6 — warm white
    val TextSecondary = Color(0xCCFFFFFF)      // 80% white
    val TextTertiary = Color(0x99FFFFFF)       // 60% white
    val TextDisabled = Color(0x66FFFFFF)

    // Semantic
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFFA726)
    val Error = Color(0xFFEF5350)
    val Info = Color(0xFF60A5FA)

    // Accent variants
    val Blue = Color(0xFF60A5FA)
    val Amber = Color(0xFFFBBF24)
    val Rose = Color(0xFFFB7185)
    val Violet = Color(0xFF8B5CF6)

    // Surfaces variants
    val Outline = Color(0x33FFFFFF)            // 20% white border
    val OutlineVariant = Color(0x1AFFFFFF)     // 10% white border
    val Hairline = Color(0x0FFFFFFF)           // 6%

    // Transaction
    val Incoming = Color(0xFF4CAF50)
    val Outgoing = Color(0xFFEF5350)

    // Category accents
    val CategoryColors = mapOf(
        "food_grocery" to Color(0xFF81C784),
        "food_delivery" to Color(0xFFA5D6A7),
        "food_dining" to Color(0xFFC8E6C9),
        "transport" to Color(0xFF64B5F6),
        "utilities" to Color(0xFF42A5F5),
        "rent_mortgage" to Color(0xFF1E88E5),
        "subscriptions" to Color(0xFFCE93D8),
        "shopping_online" to Color(0xFFBA68C8),
        "shopping_offline" to Color(0xFFAB47BC),
        "entertainment" to Color(0xFFFFAB91),
        "health" to Color(0xFFEF9A9A),
        "loans_ifn" to Color(0xFFE57373),
        "loans_bank" to Color(0xFFD32F2F),
        "bnpl" to Color(0xFFC62828),
        "travel" to Color(0xFF81D4FA),
        "savings" to Color(0xFF66BB6A),
        "unknown" to Color(0xFF757575),
    )
}

/** Accent variants pentru InsightCard, HeroCard, chips. */
enum class SolAccent(val color: androidx.compose.ui.graphics.Color) {
    Mint(SolomonColors.Primary),
    Blue(SolomonColors.Blue),
    Amber(SolomonColors.Amber),
    Rose(SolomonColors.Rose),
    Violet(SolomonColors.Violet),
    Success(SolomonColors.Success),
    Warning(SolomonColors.Warning),
    Error(SolomonColors.Error)
}
