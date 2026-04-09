package fx.volpol.webapp

import fx.volpol.webapp.rendering.renderFirstRoundPage
import fx.volpol.webapp.rendering.renderSecondRoundPage
import fx.volpol.webapp.rendering.renderTrendEmbed
import fx.volpol.webapp.services.PollService
import fx.volpol.webapp.services.siteMap
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
        siteMap()
        get("/") {
            call.renderFirstRoundPage(pollService, appConfig, homePageCanonicalUrl)
        }
        get("/premier-tour-2027") {
            call.renderFirstRoundPage(pollService, appConfig, homePageCanonicalUrl)
        }
        get("/second-tour-2027") {
            call.renderSecondRoundPage(pollService, appConfig)
        }
        get("/embed/trend") {
            call.renderTrendEmbed(pollService, appConfig)
        }
        get("/health") {
            call.respondText("ok", ContentType.Text.Plain, HttpStatusCode.OK)
        }
    }
}
