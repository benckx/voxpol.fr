package dev.encelade.pollaggregator

import dev.encelade.pollaggregator.rendering.renderHomePage
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

    configureHeaders()
    routing {
        configureStaticResources(appConfig)

        get("/health") {
            call.respondText("ok", ContentType.Text.Plain, HttpStatusCode.OK)
        }
        get("/") {
            call.renderHomePage(
                pollService = pollService,
                gaEnabled = appConfig.gaEnabled,
                trendWindowDays = appConfig.trendWindowDays,
            )
        }
        get("/embed/trend") {
            call.renderTrendEmbed(
                pollService = pollService,
                gaEnabled = appConfig.gaEnabled,
                trendWindowDays = appConfig.trendWindowDays,
            )
        }
    }
}
