package ro.solomon.core.format

import ro.solomon.core.domain.Money
import kotlin.math.abs
import kotlin.math.roundToInt

object RomanianMoneyFormatter {

    enum class Style { short, bareNumber, compact, lei }

    fun format(money: Money, style: Style = Style.short): String = format(money.amount, style)

    fun format(amount: Int, style: Style = Style.short): String = when (style) {
        Style.short -> "${thousands(amount)} RON"
        Style.bareNumber -> thousands(amount)
        Style.compact -> compact(amount, "RON")
        Style.lei -> "${thousands(amount)} lei"
    }

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

    private fun compact(amount: Int, suffix: String): String {
        val magnitude = abs(amount)
        return when {
            magnitude >= 1_000_000 -> "${decimal(amount.toDouble() / 1_000_000)} mil $suffix"
            magnitude >= 1_000 -> "${decimal(amount.toDouble() / 1_000)}k $suffix"
            else -> "$amount $suffix"
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
