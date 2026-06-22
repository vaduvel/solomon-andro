package ro.solomon.core.format

import ro.solomon.core.domain.Money
import kotlin.math.abs
import kotlin.math.roundToInt

object RomanianMoneyFormatter {

    enum class Style { short, bareNumber, compact, lei }

    fun format(money: Money, style: Style = Style.short): String = format(money.amount, style)

    /** [amountBani] is expressed in bani (minor units), matching Money.amount. */
    fun format(amountBani: Int, style: Style = Style.short): String = when (style) {
        Style.short -> "${leiWithDecimals(amountBani)} RON"
        Style.bareNumber -> leiWithDecimals(amountBani)
        Style.compact -> compact(amountBani, "RON")
        Style.lei -> "${leiWithDecimals(amountBani)} lei"
    }

    /** Formats a bani amount as lei with 2 decimals, e.g. 123456 -> "1.234,56". */
    fun leiWithDecimals(amountBani: Int): String {
        val sign = if (amountBani < 0) "-" else ""
        val absBani = abs(amountBani)
        val leiPart = absBani / 100
        val baniPart = absBani % 100
        val decimals = baniPart.toString().padStart(2, '0')
        return "$sign${thousands(leiPart)},$decimals"
    }

    /** Groups a non-negative-or-signed integer with '.' as thousands separator. */
    fun thousands(amount: Int): String {
        val sign = if (amount < 0) "-" else ""
        var digits = abs(amount).toString()
        val groups = mutableListOf<String>()
        while (digits.length > 3) {
            groups.add(0, digits.takeLast(3))
            digits = digits.dropLast(3)
        }
        groups.add(0, digits)
        return sign + groups.joinToString(".")
    }

    private fun compact(amountBani: Int, suffix: String): String {
        val lei = amountBani.toDouble() / 100.0
        val magnitude = abs(lei)
        return when {
            magnitude >= 1_000_000 -> "${decimal(lei / 1_000_000)} mil $suffix"
            magnitude >= 1_000 -> "${decimal(lei / 1_000)}k $suffix"
            else -> "${leiWithDecimals(amountBani)} $suffix"
        }
    }

    private fun decimal(value: Double): String {
        val rounded = (value * 10).roundToInt() / 10.0
        return if (rounded == rounded.toInt().toDouble()) {
            rounded.toInt().toString()
        } else {
            String.format("%.1f", rounded).replace(".", ",")
        }
    }
}
