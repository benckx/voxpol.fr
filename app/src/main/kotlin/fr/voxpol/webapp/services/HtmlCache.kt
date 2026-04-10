package fr.voxpol.webapp.services

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondText
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.util.concurrent.TimeUnit

enum class HtmlCacheKey {
    FIRST_ROUND,
    SECOND_ROUND,
    EMBED_TREND,
}

/**
 * In-memory HTML cache backed by Caffeine.
 */
class HtmlCache {

    private val logger = KotlinLogging.logger {}

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(1L, TimeUnit.HOURS)
        .removalListener<HtmlCacheKey, String> { key, _, cause ->
            when (cause) {
                RemovalCause.EXPIRED -> logger.info { "HTML cache entry expired: '$key'" }
                RemovalCause.EXPLICIT -> logger.info { "HTML cache entry explicitly invalidated: '$key'" }
                RemovalCause.REPLACED -> logger.info { "HTML cache entry replaced: '$key'" }
                else -> logger.info { "HTML cache entry removed (cause=$cause): '$key'" }
            }
        }
        .build<HtmlCacheKey, String>()

    fun get(key: HtmlCacheKey): String? = cache.getIfPresent(key).also { value ->
        if (value != null) logger.debug { "HTML cache hit: '$key'" }
        else logger.info { "HTML cache miss: '$key'" }
    }

    fun put(key: HtmlCacheKey, html: String) {
        logger.info { "HTML cache entry stored: '$key' (${html.length} chars)." }
        cache.put(key, html)
    }
}

/**
 * Drop-in replacement for `respondHtml` that caches the rendered HTML string under [key].
 * On a cache hit the pre-built string is served immediately; on a miss the [block] is
 * executed once and the result is stored for subsequent requests.
 */
suspend fun ApplicationCall.respondHtmlCached(
    htmlCache: HtmlCache,
    key: HtmlCacheKey,
    block: HTML.() -> Unit,
) {
    val html = htmlCache.get(key) ?: buildString {
        append("<!DOCTYPE html>\n")
        appendHTML().html(block = block)
    }.also { htmlCache.put(key, it) }
    respondText(html, ContentType.Text.Html)
}
