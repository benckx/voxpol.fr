package dev.encelade.pollaggregator

import dev.encelade.pollaggregator.rendering.renderFirstRoundPage
import dev.encelade.pollaggregator.rendering.renderSecondRoundPage
import dev.encelade.pollaggregator.rendering.renderTrendEmbed
import dev.encelade.pollaggregator.services.PollService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main(args: Array<String>) {
    val configArg = parseConfigArg(args)
    embeddedServer(Netty, 8080) {
        module(configArg)
    }.start(wait = true)
}

private fun Application.module(configArg: String? = null) {
    val appConfig = parseAppConfig(configArg)
    val pollService = PollService()
    val homePageCanonicalUrl = "https://voxpol.fr/premier-tour-2027"

    configureHeaders()
    routing {
        configureStaticResources(appConfig)

        get("/health") {
            call.respondText("ok", ContentType.Text.Plain, HttpStatusCode.OK)
        }
        get("/") {
            call.renderFirstRoundPage(
                pollService = pollService,
                gaEnabled = appConfig.gaEnabled,
                trendWindowDays = appConfig.trendWindowDays,
                canonicalUrl = homePageCanonicalUrl,
                minified = appConfig.minified,
            )
        }
        get("/premier-tour-2027") {
            call.renderFirstRoundPage(
                pollService = pollService,
                gaEnabled = appConfig.gaEnabled,
                trendWindowDays = appConfig.trendWindowDays,
                canonicalUrl = homePageCanonicalUrl,
                minified = appConfig.minified,
            )
        }
        get("/second-tour-2027") {
            call.renderSecondRoundPage(
                pollService = pollService,
                gaEnabled = appConfig.gaEnabled,
                minified = appConfig.minified,
            )
        }
        get("/embed/trend") {
            call.renderTrendEmbed(
                pollService = pollService,
                gaEnabled = appConfig.gaEnabled,
                trendWindowDays = appConfig.trendWindowDays,
                minified = appConfig.minified,
            )
        }
        get("/sitemap.xml") {
            val baseUrl = "https://voxpol.fr"
            val urls = listOf(
                "/premier-tour-2027",
                "/second-tour-2027",
            )
            val sitemap = buildString {
                appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
                appendLine("""<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">""")
                for (path in urls) {
                    appendLine("  <url>")
                    appendLine("    <loc>$baseUrl$path</loc>")
                    appendLine("    <changefreq>weekly</changefreq>")
                    appendLine("  </url>")
                }
                append("</urlset>")
            }
            call.respondText(sitemap, ContentType.Text.Xml, HttpStatusCode.OK)
        }
    }
}
