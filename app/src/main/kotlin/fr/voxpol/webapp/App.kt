package fr.voxpol.webapp

import fr.voxpol.webapp.rendering.renderFirstRoundPage
import fr.voxpol.webapp.rendering.renderSecondRoundPage
import fr.voxpol.webapp.rendering.renderTrendEmbed
import fr.voxpol.webapp.services.HtmlCache
import fr.voxpol.webapp.services.PollService
import fr.voxpol.webapp.services.siteMap
import fr.voxpol.webapp.utils.configureHeaders
import fr.voxpol.webapp.utils.configureStaticResources
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) {
    val configArg = parseConfigArg(args)
    embeddedServer(Netty, 8080) {
        module(configArg)
    }.start(wait = true)
}

private fun Application.module(configArg: String? = null) {
    install(Koin) {
        slf4jLogger()
        modules(module {
            single { parseAppConfig(configArg) }
            single(createdAtStart = true) { HtmlCache() }
            single(createdAtStart = true) { PollService() }
        })
    }

    configureHeaders()
    routing {
        configureStaticResources()
        siteMap()
        get("/") {
            call.renderFirstRoundPage()
        }
        get("/premier-tour-2027") {
            call.renderFirstRoundPage()
        }
        get("/second-tour-2027") {
            call.renderSecondRoundPage()
        }
        get("/embed/trend") {
            call.renderTrendEmbed()
        }
        get("/health") {
            call.respondText("ok", ContentType.Text.Plain, HttpStatusCode.OK)
        }
    }
}
