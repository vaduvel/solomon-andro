package ro.solomon.core.registries

import kotlinx.serialization.Serializable

@Serializable
enum class IFNRiskTier { high, very_high, extreme }

@Serializable
data class IFNRecord(
    val name: String,
    val domain: String,
    val emailSenderPattern: String,
    val daeMinPercent: Int,
    val daeMaxPercent: Int,
    val typicalLoanRangeMin: Int,
    val typicalLoanRangeMax: Int,
    val riskTier: IFNRiskTier
) {
    val id: String get() = domain

    fun estimatedRepaymentMultiplier(termMonths: Int = 6): Double {
        val years = termMonths.toDouble() / 12.0
        val dae = daeMinPercent.toDouble() / 100.0
        return 1.0 + dae * years
    }
}

object IFNDatabase {

    val all: List<IFNRecord> = listOf(
        IFNRecord("Credius", "credius.ro", "no-reply@credius.ro", 280, 2334, 300, 3000, IFNRiskTier.extreme),
        IFNRecord("Provident", "providentromania.ro", "office@providentromania.ro", 100, 650, 500, 8000, IFNRiskTier.very_high),
        IFNRecord("IUTE Credit", "iutecredit.ro", "no-reply@iutecredit.ro", 250, 1800, 200, 5000, IFNRiskTier.extreme),
        IFNRecord("Viva Credit", "vivacredit.ro", "contact@vivacredit.ro", 200, 1500, 200, 4000, IFNRiskTier.extreme),
        IFNRecord("Hora Credit", "horacredit.ro", "contact@horacredit.ro", 150, 1200, 200, 3500, IFNRiskTier.extreme),
        IFNRecord("MaiMai Credit", "maimaicredit.ro", "suport@maimaicredit.ro", 200, 2490, 200, 4000, IFNRiskTier.extreme),
        IFNRecord("Acredit", "acredit.ro", "contact@acredit.ro", 280, 2334, 200, 3000, IFNRiskTier.extreme),
        IFNRecord("Ferratum", "ferratum.ro", "support@ferratum.ro", 200, 1500, 200, 4000, IFNRiskTier.extreme),
        IFNRecord("Cetelem", "cetelem.ro", "support@cetelem.ro", 12, 35, 1000, 50000, IFNRiskTier.high)
    )

    fun recordForDomain(domain: String): IFNRecord? {
        val normalized = domain.lowercase()
        return all.firstOrNull { it.domain == normalized }
    }

    fun recordForSender(sender: String): IFNRecord? {
        val normalized = sender.lowercase()
        return all.firstOrNull { it.emailSenderPattern.lowercase() == normalized }
    }

    fun atLeast(tier: IFNRiskTier): List<IFNRecord> {
        val order = mapOf(IFNRiskTier.high to 0, IFNRiskTier.very_high to 1, IFNRiskTier.extreme to 2)
        val threshold = order[tier] ?: 0
        return all.filter { (order[it.riskTier] ?: 0) >= threshold }
    }
}
