package ro.solomon.core.moments

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SolomonContextCoder {

    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }

    @PublishedApi
    internal val prettyJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = true
    }

    inline fun <reified T> encodeAsJSONString(value: T): String = json.encodeToString(value)

    inline fun <reified T> encodeAsPrettyJSONString(value: T): String = prettyJson.encodeToString(value)

    inline fun <reified T> decodeFromJSONString(string: String): T = json.decodeFromString(string)
}
