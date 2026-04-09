package fx.volpol.webapp

import com.typesafe.config.ConfigFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.config.HoconApplicationConfig

data class AssetCacheConfig(
    val enabled: Boolean,
    val maxAgeSeconds: Int,
)

data class AppConfig(
    val gaEnabled: Boolean,
    val trendWindowDays: Int,
    val jsCache: AssetCacheConfig,
    val cssCache: AssetCacheConfig,
    val minified: Boolean,
)

private val logger = KotlinLogging.logger {}

fun parseConfigArg(args: Array<String>): String? {
    args.firstOrNull { it.startsWith("--config=") }
        ?.removePrefix("--config=")
        ?.takeIf { it.isNotBlank() }
        ?.let { return it }

    return args.firstOrNull { !it.startsWith("--") }
}

fun parseAppConfig(configArg: String? = null): AppConfig {
    val config = HoconApplicationConfig(loadConfig(configArg))

    val gaEnabled = config.propertyOrNull("googleAnalytics.enabled")?.getString()?.toBoolean() ?: false
    val trendWindowDays = config.propertyOrNull("trends.windowDays")?.getString()?.toIntOrNull()?.coerceAtLeast(1) ?: 21

    val jsCacheEnabled = config.propertyOrNull("staticAssets.jsCache.enabled")?.getString()?.toBoolean() ?: false
    val jsCacheMaxAgeSeconds = config.propertyOrNull("staticAssets.jsCache.maxAgeSeconds")
        ?.getString()
        ?.toIntOrNull()
        ?.coerceAtLeast(0)
        ?: 3600

    val cssCacheEnabled = config.propertyOrNull("staticAssets.cssCache.enabled")?.getString()?.toBoolean() ?: false
    val cssCacheMaxAgeSeconds = config.propertyOrNull("staticAssets.cssCache.maxAgeSeconds")
        ?.getString()
        ?.toIntOrNull()
        ?.coerceAtLeast(0)
        ?: 3600

    val minified = config.propertyOrNull("staticAssets.minified")?.getString()?.toBoolean() ?: false

    val appConfig = AppConfig(
        gaEnabled = gaEnabled,
        trendWindowDays = trendWindowDays,
        jsCache = AssetCacheConfig(
            enabled = jsCacheEnabled,
            maxAgeSeconds = jsCacheMaxAgeSeconds,
        ),
        cssCache = AssetCacheConfig(
            enabled = cssCacheEnabled,
            maxAgeSeconds = cssCacheMaxAgeSeconds,
        ),
        minified = minified,
    )

    logger.info {
        val selected = configArg?.takeIf { it.isNotBlank() } ?: "default"
        "App configuration loaded (source=$selected): $appConfig"
    }

    return appConfig
}

private fun loadConfig(configArg: String?) = when (val normalized = normalizeConfigArg(configArg)) {
    null -> ConfigFactory.load()
    else -> ConfigFactory
        .parseResourcesAnySyntax(normalized)
        .withFallback(ConfigFactory.load())
        .resolve()
}

private fun normalizeConfigArg(configArg: String?): String? {
    val raw = configArg?.trim()?.takeIf { it.isNotEmpty() } ?: return null

    return when {
        raw == "application" -> raw
        raw.startsWith("application-") -> raw.removeSuffix(".conf")
        raw.endsWith(".conf") -> raw.removeSuffix(".conf")
        else -> "application-$raw"
    }
}
