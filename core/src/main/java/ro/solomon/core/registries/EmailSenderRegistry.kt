package ro.solomon.core.registries

import kotlinx.serialization.Serializable
import ro.solomon.core.domain.Bank
import ro.solomon.core.domain.TransactionCategory

@Serializable
enum class EmailSenderCategory {
    bank, food_delivery, streaming, utility, shopping_online, bnpl, ifn, travel, entertainment, transport, insurance
}

@Serializable
enum class SenderMatchConfidence { exact, domain, keyword }

@Serializable
data class EmailSender(
    val sender: String,
    val displayName: String,
    val category: EmailSenderCategory,
    val defaultTransactionCategory: TransactionCategory
) {
    val id: String get() = sender.lowercase()
    val domain: String get() = sender.split("@").lastOrNull()?.lowercase() ?: ""
}

object EmailSenderRegistry {

    val banks: List<EmailSender> = listOf(
        EmailSender("notificare@bt.ro", "Banca Transilvania", EmailSenderCategory.bank, TransactionCategory.unknown),
        EmailSender("no-reply@bcr.ro", "BCR (George)", EmailSenderCategory.bank, TransactionCategory.unknown),
        EmailSender("notificari@ing.ro", "ING Bank", EmailSenderCategory.bank, TransactionCategory.unknown),
        EmailSender("e-banking@raiffeisen.ro", "Raiffeisen Bank", EmailSenderCategory.bank, TransactionCategory.unknown),
        EmailSender("no-reply@revolut.com", "Revolut", EmailSenderCategory.bank, TransactionCategory.unknown),
        EmailSender("no-reply@cec.ro", "CEC Bank", EmailSenderCategory.bank, TransactionCategory.unknown),
        EmailSender("e-banking@unicredit.ro", "UniCredit Bank", EmailSenderCategory.bank, TransactionCategory.unknown),
        EmailSender("notify@patriabank.ro", "Patria Bank", EmailSenderCategory.bank, TransactionCategory.unknown),
        EmailSender("e-banking@procreditbank.ro", "ProCredit", EmailSenderCategory.bank, TransactionCategory.unknown),
        EmailSender("notify@libra.ro", "Libra Internet Bank", EmailSenderCategory.bank, TransactionCategory.unknown),
        EmailSender("no-reply@garantibbva.ro", "Garanti BBVA", EmailSenderCategory.bank, TransactionCategory.unknown),
        EmailSender("e-banking@firstbank.ro", "First Bank", EmailSenderCategory.bank, TransactionCategory.unknown),
        EmailSender("no-reply@alphabank.ro", "Alpha Bank", EmailSenderCategory.bank, TransactionCategory.unknown),
        EmailSender("e-banking@otpbank.ro", "OTP Bank", EmailSenderCategory.bank, TransactionCategory.unknown),
        EmailSender("no-reply@idea-bank.ro", "Idea Bank", EmailSenderCategory.bank, TransactionCategory.unknown),
    )

    val foodDelivery: List<EmailSender> = listOf(
        EmailSender("no-reply@glovoapp.com", "Glovo", EmailSenderCategory.food_delivery, TransactionCategory.food_delivery),
        EmailSender("help@wolt.com", "Wolt", EmailSenderCategory.food_delivery, TransactionCategory.food_delivery),
        EmailSender("no-reply@tazz.ro", "Tazz", EmailSenderCategory.food_delivery, TransactionCategory.food_delivery),
        EmailSender("no-reply@boltfood.com", "Bolt Food", EmailSenderCategory.food_delivery, TransactionCategory.food_delivery),
        EmailSender("no-reply@foodpanda.com", "Foodpanda", EmailSenderCategory.food_delivery, TransactionCategory.food_delivery),
    )

    val streaming: List<EmailSender> = listOf(
        EmailSender("info@account.netflix.com", "Netflix", EmailSenderCategory.streaming, TransactionCategory.subscriptions),
        EmailSender("no-reply@email.hbomax.com", "HBO Max", EmailSenderCategory.streaming, TransactionCategory.subscriptions),
        EmailSender("no-reply@spotify.com", "Spotify", EmailSenderCategory.streaming, TransactionCategory.subscriptions),
        EmailSender("no-reply@apple.com", "Apple Services", EmailSenderCategory.streaming, TransactionCategory.subscriptions),
        EmailSender("no-reply@youtube.com", "YouTube Premium", EmailSenderCategory.streaming, TransactionCategory.subscriptions),
        EmailSender("billing@disneyplus.com", "Disney+", EmailSenderCategory.streaming, TransactionCategory.subscriptions),
        EmailSender("no-reply@github.com", "GitHub", EmailSenderCategory.streaming, TransactionCategory.subscriptions),
        EmailSender("mail@adobe.com", "Adobe Creative Cloud", EmailSenderCategory.streaming, TransactionCategory.subscriptions),
        EmailSender("no-reply@dropbox.com", "Dropbox", EmailSenderCategory.streaming, TransactionCategory.subscriptions),
        EmailSender("billing@1password.com", "1Password", EmailSenderCategory.streaming, TransactionCategory.subscriptions),
        EmailSender("no-reply@figma.com", "Figma", EmailSenderCategory.streaming, TransactionCategory.subscriptions),
        EmailSender("no-reply@notion.so", "Notion", EmailSenderCategory.streaming, TransactionCategory.subscriptions),
        EmailSender("no-reply@calm.com", "Calm", EmailSenderCategory.streaming, TransactionCategory.subscriptions),
        EmailSender("no-reply@headspace.com", "Headspace", EmailSenderCategory.streaming, TransactionCategory.subscriptions),
        EmailSender("no-reply@duolingo.com", "Duolingo", EmailSenderCategory.streaming, TransactionCategory.subscriptions),
    )

    val utilities: List<EmailSender> = listOf(
        EmailSender("contact@enel.ro", "Enel", EmailSenderCategory.utility, TransactionCategory.utilities),
        EmailSender("office@digi.ro", "Digi", EmailSenderCategory.utility, TransactionCategory.utilities),
        EmailSender("clientservice@rcs-rds.ro", "RCS-RDS", EmailSenderCategory.utility, TransactionCategory.utilities),
        EmailSender("help@orange.ro", "Orange", EmailSenderCategory.utility, TransactionCategory.utilities),
        EmailSender("contact@vodafone.ro", "Vodafone", EmailSenderCategory.utility, TransactionCategory.utilities),
        EmailSender("servicii@telekom.ro", "Telekom", EmailSenderCategory.utility, TransactionCategory.utilities),
        EmailSender("clienti@engie.ro", "Engie", EmailSenderCategory.utility, TransactionCategory.utilities),
        EmailSender("contact@e-on.ro", "E.ON", EmailSenderCategory.utility, TransactionCategory.utilities),
        EmailSender("clienti@apanovabucuresti.ro", "Apa Nova București", EmailSenderCategory.utility, TransactionCategory.utilities),
        EmailSender("clienti@distributiegazenaturale.ro", "Distrigaz", EmailSenderCategory.utility, TransactionCategory.utilities),
    )

    val shopping: List<EmailSender> = listOf(
        EmailSender("no-reply@emag.ro", "eMAG", EmailSenderCategory.shopping_online, TransactionCategory.shopping_online),
        EmailSender("no-reply@altex.ro", "Altex", EmailSenderCategory.shopping_online, TransactionCategory.shopping_online),
        EmailSender("no-reply@flanco.ro", "Flanco", EmailSenderCategory.shopping_online, TransactionCategory.shopping_online),
        EmailSender("no-reply@elefant.ro", "Elefant", EmailSenderCategory.shopping_online, TransactionCategory.shopping_online),
        EmailSender("no-reply@bookuriste.ro", "Bookurile", EmailSenderCategory.shopping_online, TransactionCategory.shopping_online),
        EmailSender("no-reply@sephora.ro", "Sephora", EmailSenderCategory.shopping_online, TransactionCategory.shopping_online),
        EmailSender("no-reply@douglas.ro", "Douglas", EmailSenderCategory.shopping_online, TransactionCategory.shopping_online),
        EmailSender("no-reply@h-and-m.com", "H&M", EmailSenderCategory.shopping_online, TransactionCategory.shopping_online),
        EmailSender("no-reply@zalando.com", "Zalando", EmailSenderCategory.shopping_online, TransactionCategory.shopping_online),
        EmailSender("no-reply@aboutyou.ro", "About You", EmailSenderCategory.shopping_online, TransactionCategory.shopping_online),
        EmailSender("no-reply@fashiondays.ro", "Fashion Days", EmailSenderCategory.shopping_online, TransactionCategory.shopping_online),
        EmailSender("no-reply@decathlon.ro", "Decathlon", EmailSenderCategory.shopping_online, TransactionCategory.shopping_online),
        EmailSender("no-reply@dedeman.ro", "Dedeman", EmailSenderCategory.shopping_online, TransactionCategory.shopping_online),
        EmailSender("no-reply@ikea.ro", "IKEA", EmailSenderCategory.shopping_online, TransactionCategory.shopping_online),
        EmailSender("no-reply@auchan.ro", "Auchan online", EmailSenderCategory.shopping_online, TransactionCategory.food_grocery),
        EmailSender("no-reply@kaufland.ro", "Kaufland online", EmailSenderCategory.shopping_online, TransactionCategory.food_grocery),
        EmailSender("no-reply@carrefour.ro", "Carrefour online", EmailSenderCategory.shopping_online, TransactionCategory.food_grocery),
    )

    val bnplAndIfn: List<EmailSender> = listOf(
        EmailSender("hello@mokka.ro", "Mokka", EmailSenderCategory.bnpl, TransactionCategory.bnpl),
        EmailSender("no-reply@tbi.ro", "TBI Bank", EmailSenderCategory.bnpl, TransactionCategory.bnpl),
        EmailSender("support@paypo.ro", "PayPo", EmailSenderCategory.bnpl, TransactionCategory.bnpl),
        EmailSender("support@klarna.com", "Klarna", EmailSenderCategory.bnpl, TransactionCategory.bnpl),
        EmailSender("hello@felice.ro", "Felice", EmailSenderCategory.bnpl, TransactionCategory.bnpl),
        EmailSender("no-reply@credius.ro", "Credius", EmailSenderCategory.ifn, TransactionCategory.loans_ifn),
        EmailSender("office@providentromania.ro", "Provident", EmailSenderCategory.ifn, TransactionCategory.loans_ifn),
        EmailSender("no-reply@iutecredit.ro", "IUTE Credit", EmailSenderCategory.ifn, TransactionCategory.loans_ifn),
        EmailSender("contact@vivacredit.ro", "Viva Credit", EmailSenderCategory.ifn, TransactionCategory.loans_ifn),
        EmailSender("contact@horacredit.ro", "Hora Credit", EmailSenderCategory.ifn, TransactionCategory.loans_ifn),
        EmailSender("suport@maimaicredit.ro", "MaiMai Credit", EmailSenderCategory.ifn, TransactionCategory.loans_ifn),
        EmailSender("contact@acredit.ro", "Acredit", EmailSenderCategory.ifn, TransactionCategory.loans_ifn),
        EmailSender("support@ferratum.ro", "Ferratum", EmailSenderCategory.ifn, TransactionCategory.loans_ifn),
        EmailSender("support@cetelem.ro", "Cetelem", EmailSenderCategory.ifn, TransactionCategory.loans_ifn),
    )

    val travel: List<EmailSender> = listOf(
        EmailSender("no-reply@booking.com", "Booking.com", EmailSenderCategory.travel, TransactionCategory.travel),
        EmailSender("automated@airbnb.com", "Airbnb", EmailSenderCategory.travel, TransactionCategory.travel),
        EmailSender("no-reply@esky.ro", "eSky", EmailSenderCategory.travel, TransactionCategory.travel),
        EmailSender("no-reply@vola.ro", "Vola.ro", EmailSenderCategory.travel, TransactionCategory.travel),
        EmailSender("flightcenter@kiwi.com", "Kiwi.com", EmailSenderCategory.travel, TransactionCategory.travel),
        EmailSender("no-reply@tarom.ro", "TAROM", EmailSenderCategory.travel, TransactionCategory.travel),
        EmailSender("booking@blueair.aero", "Blue Air", EmailSenderCategory.travel, TransactionCategory.travel),
        EmailSender("no-reply@wizzair.com", "Wizz Air", EmailSenderCategory.travel, TransactionCategory.travel),
        EmailSender("no-reply@ryanair.com", "Ryanair", EmailSenderCategory.travel, TransactionCategory.travel),
        EmailSender("contact@christiantour.ro", "Christian Tour", EmailSenderCategory.travel, TransactionCategory.travel),
        EmailSender("contact@paraleladu.ro", "Paralela 45", EmailSenderCategory.travel, TransactionCategory.travel),
    )

    val entertainment: List<EmailSender> = listOf(
        EmailSender("no-reply@eventim.ro", "Eventim", EmailSenderCategory.entertainment, TransactionCategory.entertainment),
        EmailSender("contact@iabilet.ro", "iaBilet", EmailSenderCategory.entertainment, TransactionCategory.entertainment),
        EmailSender("hello@bilet.ro", "Bilet.ro", EmailSenderCategory.entertainment, TransactionCategory.entertainment),
        EmailSender("support@untold.com", "UNTOLD", EmailSenderCategory.entertainment, TransactionCategory.entertainment),
        EmailSender("hello@electriccastle.ro", "Electric Castle", EmailSenderCategory.entertainment, TransactionCategory.entertainment),
    )

    val transport: List<EmailSender> = listOf(
        EmailSender("no-reply@bolt.eu", "Bolt", EmailSenderCategory.transport, TransactionCategory.transport),
        EmailSender("no-reply@uber.com", "Uber", EmailSenderCategory.transport, TransactionCategory.transport),
        EmailSender("no-reply@yango.com", "Yango", EmailSenderCategory.transport, TransactionCategory.transport),
        EmailSender("contact@stb.ro", "STB", EmailSenderCategory.transport, TransactionCategory.transport),
        EmailSender("no-reply@cfr-calatori.ro", "CFR Călători", EmailSenderCategory.transport, TransactionCategory.transport),
        EmailSender("contact@blablacar.com", "BlaBlaCar", EmailSenderCategory.transport, TransactionCategory.transport),
        EmailSender("hello@taxify.eu", "Taxify", EmailSenderCategory.transport, TransactionCategory.transport),
    )

    val insurance: List<EmailSender> = listOf(
        EmailSender("contact@allianz.ro", "Allianz", EmailSenderCategory.insurance, TransactionCategory.health),
        EmailSender("contact@asirom.ro", "Asirom", EmailSenderCategory.insurance, TransactionCategory.health),
        EmailSender("clienti@groupama.ro", "Groupama", EmailSenderCategory.insurance, TransactionCategory.health),
        EmailSender("contact@nn.ro", "NN Asigurări", EmailSenderCategory.insurance, TransactionCategory.health),
        EmailSender("clienti@omniasig.ro", "Omniasig", EmailSenderCategory.insurance, TransactionCategory.health),
        EmailSender("contact@uniqa.ro", "Uniqa", EmailSenderCategory.insurance, TransactionCategory.health),
    )

    val all: List<EmailSender> = banks + foodDelivery + streaming + utilities + shopping +
            bnplAndIfn + travel + entertainment + transport + insurance

    fun senderMatching(sender: String): EmailSender? {
        val normalized = sender.lowercase()
        return all.firstOrNull { it.sender.lowercase() == normalized }
    }
}
