package ro.solomon.core.registries

import kotlinx.serialization.Serializable
import ro.solomon.core.domain.CancellationDifficulty

@Serializable
data class CancellationEntry(
    val nameMatchPatterns: List<String>,
    val cancellationUrl: String? = null,
    val stepsSummary: String = "",
    val warning: String? = null,
    val alternative: String? = null,
    val difficulty: CancellationDifficulty = CancellationDifficulty.medium
) {
    init {
        nameMatchPatterns.map { it.lowercase() }
    }
}

object SubscriptionCancellationDB {

    private val allEntries: List<CancellationEntry> = listOf(
        CancellationEntry(listOf("netflix"), "https://www.netflix.com/cancelplan",
            "Logare → Account → Cancel Membership.", difficulty = CancellationDifficulty.easy),
        CancellationEntry(listOf("hbo", "hbo max", "max"), "https://play.max.com/account/preferences",
            "Settings → Subscription → Cancel.", difficulty = CancellationDifficulty.easy),
        CancellationEntry(listOf("disney"), "https://www.disneyplus.com/account/subscription",
            "Account → Subscription → Cancel.", difficulty = CancellationDifficulty.easy),
        CancellationEntry(listOf("spotify"), "https://www.spotify.com/account/subscription/",
            "Account → Subscription → Cancel Premium.", alternative = "Spotify Free e gratuit cu reclame",
            difficulty = CancellationDifficulty.easy),
        CancellationEntry(listOf("apple music"), "https://music.apple.com/account/subscriptions",
            "Settings → Subscriptions → Apple Music → Cancel.", difficulty = CancellationDifficulty.easy),
        CancellationEntry(listOf("youtube premium", "youtube music"), "https://www.youtube.com/paid_memberships",
            "YouTube → Avatar → Purchases → Cancel.", difficulty = CancellationDifficulty.easy),
        CancellationEntry(listOf("icloud", "icloud+"), "https://support.apple.com/en-us/HT207024",
            "Settings → iCloud → Manage Storage → Downgrade.",
            warning = "Dacă ai mai mult de 5GB stocat, fișierele se pot șterge",
            alternative = "Free 5GB iCloud sau Google Drive 15GB", difficulty = CancellationDifficulty.medium),
        CancellationEntry(listOf("google one"), "https://one.google.com/storage",
            "one.google.com → Settings → Cancel membership.", difficulty = CancellationDifficulty.easy),
        CancellationEntry(listOf("dropbox"), "https://www.dropbox.com/account/plan",
            "Account → Plan → Downgrade to Basic.", difficulty = CancellationDifficulty.easy),
        CancellationEntry(listOf("adobe creative cloud", "adobe"), "https://account.adobe.com/plans",
            "Plans → Manage plan → Cancel.",
            warning = "Penalty 50% din suma rămasă pe contract anual",
            alternative = "GIMP, Affinity Photo", difficulty = CancellationDifficulty.hard),
        CancellationEntry(listOf("microsoft 365", "office 365"), "https://account.microsoft.com/services",
            "account.microsoft.com → Services & subscriptions → Cancel.", difficulty = CancellationDifficulty.easy),
        CancellationEntry(listOf("notion"), "https://www.notion.so/my-integrations",
            "Settings → Plans → Switch to Free.", difficulty = CancellationDifficulty.easy),
        CancellationEntry(listOf("1password"), "https://my.1password.com/profile/subscription",
            "Profile → Subscription → Cancel.",
            warning = "După cancel, datele rămân criptate dar read-only", difficulty = CancellationDifficulty.medium),
        CancellationEntry(listOf("github"), "https://github.com/settings/billing/plans",
            "Settings → Billing → Cancel.", difficulty = CancellationDifficulty.easy),
        CancellationEntry(listOf("chatgpt", "openai"), "https://chatgpt.com/#settings/Subscription",
            "Settings → Subscription → Cancel.", difficulty = CancellationDifficulty.medium),
        CancellationEntry(listOf("claude", "anthropic"), "https://claude.ai/settings/billing",
            "Settings → Billing → Cancel.", difficulty = CancellationDifficulty.medium),
        CancellationEntry(listOf("nordvpn", "expressvpn", "surfshark"), "https://account.nordvpn.com/",
            "Account → Subscription → Cancel.",
            warning = "Refund 30 zile dacă nu ai folosit mult", difficulty = CancellationDifficulty.medium),
        CancellationEntry(listOf("dazn"), "https://www.dazn.com/en-RO/account/subscription",
            "Account → Subscription → Cancel.", difficulty = CancellationDifficulty.medium),
    )

    fun entryForSubscriptionName(name: String): CancellationEntry? {
        val lower = name.lowercase()
        return allEntries.firstOrNull { entry ->
            entry.nameMatchPatterns.any { lower.contains(it) }
        }
    }
}
