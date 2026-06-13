package ro.solomon.web

import java.net.URL

sealed class SolomonWebError : Exception() {
    data class DomainNotWhitelisted(val url: String) : SolomonWebError()
    object EmptyQuery : SolomonWebError()
    data class NetworkError(val underlying: Throwable) : SolomonWebError()
    data class DecodingError(val underlying: Throwable) : SolomonWebError()
    data class HttpError(val statusCode: Int) : SolomonWebError()
}

interface WebSearchClientProtocol {
    suspend fun search(query: WebSearchQuery): WebSearchResult
    fun scamCheck(text: String): ScamMatchResult?
    fun isAllowed(url: URL): Boolean
}

class SolomonWebClient(
    private val http: HTTPClient = URLSessionClient(),
    private val cache: WebSearchCache = WebSearchCache(),
    private val whitelist: WebWhitelistFilter = WebWhitelistFilter(),
    private val scamMatcher: ScamPatternMatcher = ScamPatternMatcher()
) : WebSearchClientProtocol {

    private val ddg: DDGInstantAnswerClient = DDGInstantAnswerClient(http)

    private val statsLock = Any()
    private var cacheHits: Int = 0
    private var cacheMisses: Int = 0
    private var totalSearches: Int = 0

    override suspend fun search(query: WebSearchQuery): WebSearchResult {
        val trimmed = query.text.trim()
        if (trimmed.isEmpty()) throw SolomonWebError.EmptyQuery

        synchronized(statsLock) { totalSearches += 1 }

        cache.get(query.cacheKey)?.let { hit ->
            synchronized(statsLock) { cacheHits += 1 }
            return hit
        }
        synchronized(statsLock) { cacheMisses += 1 }

        val result = try {
            ddg.fetch(query)
        } catch (e: DDGClientError) {
            throw when (e) {
                is DDGClientError.NetworkError -> SolomonWebError.NetworkError(e.underlying)
                is DDGClientError.DecodingError -> SolomonWebError.DecodingError(e.underlying)
                is DDGClientError.HttpError -> SolomonWebError.HttpError(e.statusCode)
                DDGClientError.InvalidURL -> SolomonWebError.EmptyQuery
            }
        }

        cache.set(query.cacheKey, result, query.queryType.cacheTTLSeconds)
        return result
    }

    override fun scamCheck(text: String): ScamMatchResult? = scamMatcher.match(text)
    override fun isAllowed(url: URL): Boolean = whitelist.isAllowed(url)

    fun invalidate(query: WebSearchQuery) {
        cache.invalidate(query.cacheKey)
    }

    fun purgeCache() = cache.purgeAll()
    fun purgeExpiredCache() = cache.purgeExpired()

    val cacheCount: Int get() = cache.count
    val validCacheCount: Int get() = cache.validCount
    val hitRate: Double
        get() = synchronized(statsLock) {
            if (totalSearches == 0) 0.0
            else cacheHits.toDouble() / totalSearches.toDouble()
        }
}
