package fr.voxpol.webapp.services

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.TimeUnit

/**
 * In-memory HTML cache backed by Caffeine.
 */
class HtmlCache {

    private val logger = KotlinLogging.logger {}

    private val cache = Caffeine
        .newBuilder()
        .expireAfterWrite(1L, TimeUnit.HOURS)
        .removalListener<String, String> { key, _, cause ->
            when (cause) {
                RemovalCause.EXPIRED -> logger.info { "HTML cache entry expired: '$key'" }
                RemovalCause.EXPLICIT -> logger.info { "HTML cache entry explicitly invalidated: '$key'" }
                RemovalCause.REPLACED -> logger.info { "HTML cache entry replaced: '$key'" }
                else -> logger.info { "HTML cache entry removed (cause=$cause): '$key'" }
            }
        }
        .build<String, String>()

    fun get(key: String): String? = cache.getIfPresent(key).also { value ->
        if (value != null) logger.debug { "HTML cache hit: '$key'" }
        else logger.info { "HTML cache miss: '$key'" }
    }

    fun put(key: String, html: String) {
        logger.info { "HTML cache entry stored: '$key' (${html.length} chars)." }
        cache.put(key, html)
    }

}
