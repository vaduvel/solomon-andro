package ro.solomon.app.ui.util

import android.view.HapticFeedbackConstants
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalSolomonHaptics = staticCompositionLocalOf<Haptics> { error("Haptics not provided") }

/**
 * Wrapper Compose peste Android HapticFeedback.
 * Mapare 1:1 cu Haptics Swift (light/medium/heavy/selection/success/warning/error).
 */
class Haptics(private val haptic: HapticFeedback?) {
    fun light() { haptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    fun medium() { haptic?.performHapticFeedback(HapticFeedbackType.LongPress) }
    fun heavy() { haptic?.performHapticFeedback(HapticFeedbackType.LongPress) }
    fun selection() { haptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    fun success() { /* Android nu are echivalent direct; longPress e cel mai apropiat */ haptic?.performHapticFeedback(HapticFeedbackType.LongPress) }
    fun warning() { haptic?.performHapticFeedback(HapticFeedbackType.LongPress) }
    fun error() { haptic?.performHapticFeedback(HapticFeedbackType.LongPress) }
}

@Composable
fun rememberHaptics(): Haptics {
    val h = LocalHapticFeedback.current
    return Haptics(h)
}
