package ro.solomon.core.domain

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.roundToInt

@JvmInline
@Serializable
value class Money(val amount: Int) : Comparable<Money> {

    val isZero: Boolean get() = amount == 0
    val isPositive: Boolean get() = amount > 0
    val isNegative: Boolean get() = amount < 0

    companion object {
        fun fromMinor(bani: Int): Money {
            val sign = if (bani < 0) -1 else 1
            val absRounded = (abs(bani) + 50) / 100
            return Money(sign * absRounded)
        }

        fun fromRON(value: Double): Money {
            return Money(value.roundToInt())
        }
    }

    operator fun plus(other: Money) = Money(amount + other.amount)
    operator fun minus(other: Money) = Money(amount - other.amount)
    operator fun times(scalar: Int) = Money(amount * scalar)
    operator fun unaryMinus() = Money(-amount)

    override fun compareTo(other: Money) = amount.compareTo(other.amount)
}
