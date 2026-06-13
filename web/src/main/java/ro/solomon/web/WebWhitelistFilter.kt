package ro.solomon.web

import ro.solomon.core.registries.WhitelistedDomain
import ro.solomon.core.registries.WebSearchWhitelist
import ro.solomon.core.registries.WebTrustLevel
import java.net.URL

class WebWhitelistFilter {

    fun isAllowed(url: URL): Boolean = domainFor(url) != null

    fun domainFor(url: URL): WhitelistedDomain? {
        val host = url.host?.lowercase() ?: return null
        return WebSearchWhitelist.all.firstOrNull { whitelisted ->
            host == whitelisted.host || host.endsWith(".${whitelisted.host}")
        }
    }

    fun trustLevelFor(url: URL): WebTrustLevel? = domainFor(url)?.trustLevel

    fun domainsMatchingTag(tag: String): List<WhitelistedDomain> {
        val lowTag = tag.lowercase()
        return WebSearchWhitelist.all.filter { entry ->
            entry.topicTags.any { it.lowercase() == lowTag }
        }
    }

    val officialDomains: List<WhitelistedDomain>
        get() = WebSearchWhitelist.all.filter { it.trustLevel == WebTrustLevel.high }

    fun cacheTTLSecondsFor(url: URL): Long? =
        domainFor(url)?.defaultCachePolicy?.seconds?.toLong()
}
