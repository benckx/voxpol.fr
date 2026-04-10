package fr.voxpol.webapp

import io.ktor.http.CacheControl
import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.request.path
import io.ktor.server.routing.Route

private val AntiFrameHeadersPlugin = createApplicationPlugin("AntiFrameHeaders") {
    onCall { call ->
        if (!call.request.path().startsWith("/embed")) {
            call.response.headers.append("X-Frame-Options", "DENY")
            call.response.headers.append("Content-Security-Policy", "frame-ancestors 'none'")
        }
    }
}

fun Application.configureHeaders() {
    install(DefaultHeaders) {
        header(HttpHeaders.Server, "voxpol.fr")
    }
    install(AntiFrameHeadersPlugin)
}

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
