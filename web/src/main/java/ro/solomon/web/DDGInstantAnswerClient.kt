package ro.solomon.web

import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection

fun interface HTTPClient {
    suspend fun fetch(url: URL): HttpResult
}

data class HttpResult(
    val statusCode: Int,
    val body: String
)

class URLSessionClient(
    private val timeoutMs: Int = 8_000
) : HTTPClient {
    override suspend fun fetch(url: URL): HttpResult {
        val conn: URLConnection = url.openConnection()
        if (conn is HttpURLConnection) {
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Solomon/1.0 (Android)")
        }
        val code = (conn as? HttpURLConnection)?.responseCode ?: 0
        val body = conn.getInputStream().bufferedReader().use { it.readText() }
        return HttpResult(code, body)
    }
}

sealed class DDGClientError : Exception() {
    object InvalidURL : DDGClientError()
    data class NetworkError(val underlying: Throwable) : DDGClientError()
    data class DecodingError(val underlying: Throwable) : DDGClientError()
    data class HttpError(val statusCode: Int) : DDGClientError()
}

class DDGInstantAnswerClient(
    private val http: HTTPClient = URLSessionClient()
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(query: WebSearchQuery): WebSearchResult {
        val url = query.ddgURL ?: throw DDGClientError.InvalidURL

        val result: HttpResult = try {
            http.fetch(url)
        } catch (e: Throwable) {
            throw DDGClientError.NetworkError(e)
        }

        if (result.statusCode !in 200..299) {
            throw DDGClientError.HttpError(result.statusCode)
        }

        val ddg = try {
            json.decodeFromString(DDGResponse.serializer(), result.body)
        } catch (e: Throwable) {
            throw DDGClientError.DecodingError(e)
        }

        return buildResult(ddg, query)
    }

    private fun buildResult(ddg: DDGResponse, query: WebSearchQuery): WebSearchResult {
        val answer = ddg.answer?.trim()?.takeIf { it.isNotEmpty() }
        val abstract = ddg.abstractText.trim().takeIf { it.isNotEmpty() }
        val sourceURL = ddg.abstractURL.takeIf { it.isNotBlank() }
        val topics = ddg.relatedTopics.mapNotNull { it.text?.trim()?.takeIf { t -> t.isNotEmpty() } }
        return WebSearchResult(
            query = query.text,
            queryType = query.queryType,
            answer = answer,
            abstractText = abstract,
            sourceURL = sourceURL,
            relatedTopics = topics.take(5)
        )
    }
}
