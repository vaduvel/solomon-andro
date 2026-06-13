package ro.solomon.email

import ro.solomon.core.domain.TransactionCategory
import ro.solomon.core.registries.EmailSenderCategory
import ro.solomon.core.registries.EmailSenderRegistry

class SenderMapper {

    fun map(from: String): SenderMatchResult? {
        val normalized = from.lowercase().trim()

        EmailSenderRegistry.all.firstOrNull { it.sender.lowercase() == normalized }?.let { exact ->
            return SenderMatchResult(sender = exact, confidence = ro.solomon.core.registries.SenderMatchConfidence.exact)
        }

        val fromDomain = domainOf(normalized)
        EmailSenderRegistry.all.firstOrNull { domainOf(it.sender) == fromDomain }?.let { domainMatch ->
            return SenderMatchResult(sender = domainMatch, confidence = ro.solomon.core.registries.SenderMatchConfidence.domain)
        }

        EmailSenderRegistry.all.firstOrNull { isSubdomain(fromDomain, domainOf(it.sender)) }?.let { parentMatch ->
            return SenderMatchResult(sender = parentMatch, confidence = ro.solomon.core.registries.SenderMatchConfidence.domain)
        }

        return null
    }

    private fun domainOf(address: String): String =
        address.substringAfter("@", "").lowercase()

    private fun isSubdomain(sub: String, parent: String): Boolean {
        if (parent.isEmpty() || parent.length >= sub.length) return false
        return sub.endsWith(".$parent")
    }
}
