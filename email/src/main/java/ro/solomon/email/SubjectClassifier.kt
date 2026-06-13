package ro.solomon.email

import ro.solomon.core.domain.FlowDirection
import ro.solomon.core.domain.TransactionCategory

class SubjectClassifier {

    fun isFinanciallyRelevant(subject: String): Boolean {
        val s = subject.lowercase().stripDiacritics()
        return relevanceKeywords.any { s.contains(it) }
    }

    fun inferDirection(subject: String): FlowDirection? {
        val s = subject.lowercase().stripDiacritics()
        val incomingScore = incomingKeywords.count { s.contains(it) }
        val outgoingScore = outgoingKeywords.count { s.contains(it) }
        return when {
            incomingScore > outgoingScore -> FlowDirection.incoming
            outgoingScore > incomingScore -> FlowDirection.outgoing
            else -> null
        }
    }

    fun suggestCategory(subject: String): TransactionCategory? {
        val s = subject.lowercase().stripDiacritics()
        for ((keywords, cat) in categoryHints) {
            if (keywords.any { s.contains(it) }) return cat
        }
        return null
    }

    companion object {
        private val relevanceKeywords = listOf(
            "factura", "plata", "comanda", "tranzactie", "abonament",
            "extras", "rambursare", "transfer", "achitat", "achitare",
            "debit", "credit", "suma", "total", "rata", "imprumut",
            "valoare", "confirmare", "rezervare", "bilet", "chitanta",
            "invoice", "bon", "payment", "order", "transaction",
            "subscription", "statement", "refund", "charge", "receipt",
            "booking", "reservation", "ticket"
        )

        private val incomingKeywords = listOf(
            "primit", "intrat", "incasat", "rambursare", "refund",
            "restituire", "credit aprobat", "credit virat", "salariu",
            "bonus", "transfer primit", "suma virata", "received",
            "credited", "deposited"
        )

        private val outgoingKeywords = listOf(
            "factura", "plata", "platit", "comanda", "achitat",
            "debit", "retras", "scadent", "rata", "abonament",
            "cumparatura", "invoice", "payment due", "charged",
            "debited", "withdrawn", "order confirmed"
        )

        private val categoryHints: List<Pair<List<String>, TransactionCategory>> = listOf(
            listOf("glovo", "wolt", "tazz", "foodpanda", "bolt food") to TransactionCategory.food_delivery,
            listOf("netflix", "hbo", "spotify", "apple music", "youtube premium", "disney") to TransactionCategory.subscriptions,
            listOf("enel", "digi", "rcs", "orange", "vodafone", "telekom", "engie", "eon", "gaz", "curent", "apa") to TransactionCategory.utilities,
            listOf("emag", "altex", "flanco", "zalando", "h&m", "ikea") to TransactionCategory.shopping_online,
            listOf("mokka", "tbi", "paypo", "klarna", "bnpl", "rate") to TransactionCategory.bnpl,
            listOf("credius", "provident", "iute", "viva credit", "ifn") to TransactionCategory.loans_ifn,
            listOf("booking", "airbnb", "tarom", "wizz", "ryanair", "vola", "esky") to TransactionCategory.travel,
            listOf("uber", "bolt", "stb", "cfr", "blablacar") to TransactionCategory.transport,
            listOf("allianz", "groupama", "nn asigurari", "omniasig", "uniqa", "asirom") to TransactionCategory.health,
            listOf("eventim", "bilet") to TransactionCategory.entertainment
        )
    }
}
