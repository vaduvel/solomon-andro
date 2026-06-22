package ro.solomon.core.domain

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * Monetary amount stored in **bani** (minor units, 1 RON = 100 bani).
 *
 * Keeping money in minor units avoids the precision loss that happened when the
 * value was stored in whole lei (e.g. 39.99 RON previously rounded to 40 RON).
 * All arithmetic operates on bani, so sums and comparisons stay exact.
 *
 * IMPORTANT: `amount` is expressed in bani. Do NOT build a Money from a lei
 * literal via the primary constructor (e.g. `Money(1500)` means 15,00 lei).
 * Use [fromLei] for whole-lei values, or [fromBani] / [fromRON] / [fromMinor].
 */
@JvmInline
@Serializable
value class Money(val amount: Int) : Comparable<Money> {

    /** Value in bani (minor units). Alias for [amount]. */
    val bani: Int get() = amount

    /** Value in lei as a Double. Use only for display/formatting, never for storage. */
    val lei: Double get() = amount / 100.0

    val isZero: Boolean get() = amount == 0
    val isPositive: Boolean get() = amount > 0
    val isNegative: Boolean get() = amount < 0

    companion object {
        val zero: Money = Money(0)

        /** Build from bani (minor units). */
        fun fromBani(bani: Int): Money = Money(bani)

        /** Build from whole lei. */
        fun fromLei(lei: Int): Money = Money(lei * 100)

        /** Build from a lei Double, rounding to the nearest ban. */
        fun fromLei(lei: Double): Money = Money((lei * 100).roundToInt())

        /**
         * Previously rounded a minor-unit value to whole lei (losing precision).
         * Now that Money is stored in bani, this preserves the exact minor units.
         */
        fun fromMinor(bani: Int): Money = Money(bani)

        /** Build from a RON value, preserving bani precision (39.99 -> 3999 bani). */
        fun fromRON(value: Double): Money = Money((value * 100).roundToInt())
    }

    operator fun plus(other: Money) = Money(amount + other.amount)
    operator fun minus(other: Money) = Money(amount - other.amount)
    operator fun times(scalar: Int) = Money(amount * scalar)
    operator fun unaryMinus() = Money(-amount)

    override fun compareTo(other: Money) = amount.compareTo(other.amount)
}
