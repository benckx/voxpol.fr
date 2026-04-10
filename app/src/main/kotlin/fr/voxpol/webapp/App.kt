package fr.voxpol.webapp

import fr.voxpol.webapp.rendering.renderFirstRoundPage
import fr.voxpol.webapp.rendering.renderSecondRoundPage
import fr.voxpol.webapp.rendering.renderTrendEmbed
import fr.voxpol.webapp.services.HtmlCache
import fr.voxpol.webapp.services.PollService
import fr.voxpol.webapp.services.siteMap
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
    val htmlCache = HtmlCache()
    val pollService = PollService()
    val homePageCanonicalUrl = "https://voxpol.fr/premier-tour-2027"

    configureHeaders()
    routing {
        configureStaticResources(appConfig)
        siteMap()
        get("/") {
            call.renderFirstRoundPage(pollService, appConfig, homePageCanonicalUrl, htmlCache)
        }
        get("/premier-tour-2027") {
            call.renderFirstRoundPage(pollService, appConfig, homePageCanonicalUrl, htmlCache)
        }
        get("/second-tour-2027") {
            call.renderSecondRoundPage(pollService, appConfig, htmlCache)
        }
        get("/embed/trend") {
            call.renderTrendEmbed(pollService, appConfig, htmlCache)
        }
        get("/health") {
            call.respondText("ok", ContentType.Text.Plain, HttpStatusCode.OK)
        }
    }
}
