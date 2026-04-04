package dev.encelade.pollaggregator

import io.ktor.http.CacheControl
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.Route

fun Route.configureStaticResources(appConfig: AppConfig) {
    staticResources("/static", "static") {
        if (appConfig.jsCache.enabled || appConfig.cssCache.enabled) {
            cacheControl { resource ->
                when {
                    resource.path.endsWith(".js") && appConfig.jsCache.enabled -> {
                        listOf(CacheControl.MaxAge(appConfig.jsCache.maxAgeSeconds))
                    }

                    resource.path.endsWith(".css") && appConfig.cssCache.enabled -> {
                        listOf(CacheControl.MaxAge(appConfig.cssCache.maxAgeSeconds))
                    }

                    else -> emptyList()
                }
            }
        }
    }
}
