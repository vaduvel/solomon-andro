package ro.solomon.web

import java.text.Normalizer

private fun String.stripDiacritics(): String {
    val nfd = Normalizer.normalize(this, Normalizer.Form.NFD)
    return nfd.replace(Regex("""\p{InCombiningDiacriticalMarks}+"""), "")
}

internal fun String.normalizeForScam(): String =
    lowercase().stripDiacritics()

class WebSearchCache {

    private data class Entry(
        val result: WebSearchResult,
        val expiresAtEpochSeconds: Long
    ) {
        fun isExpired(now: Long): Boolean = expiresAtEpochSeconds < now
    }

    private val store: MutableMap<String, Entry> = mutableMapOf()
    private val lock = Any()

    fun get(key: String): WebSearchResult? {
        val now = System.currentTimeMillis() / 1000L
        return synchronized(lock) {
            val entry = store[key] ?: return@synchronized null
            if (entry.isExpired(now)) {
                store.remove(key)
                return@synchronized null
            }
            entry.result.copy(isFromCache = true)
        }
    }

    fun set(key: String, result: WebSearchResult, ttlSeconds: Long) {
        val now = System.currentTimeMillis() / 1000L
        synchronized(lock) {
            store[key] = Entry(result = result, expiresAtEpochSeconds = now + ttlSeconds)
        }
    }

    fun invalidate(key: String) {
        synchronized(lock) { store.remove(key) }
    }

    fun purgeAll() {
        synchronized(lock) { store.clear() }
    }

    fun purgeExpired() {
        val now = System.currentTimeMillis() / 1000L
        synchronized(lock) {
            store.entries.removeAll { it.value.isExpired(now) }
        }
    }

    val count: Int get() = synchronized(lock) { store.size }
    val validCount: Int get() {
        val now = System.currentTimeMillis() / 1000L
        return synchronized(lock) { store.values.count { !it.isExpired(now) } }
    }
}
