package dev.encelade.pollaggregator

import dev.encelade.pollaggregator.rendering.renderHomePage
import dev.encelade.pollaggregator.services.PollService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main(args: Array<String>) {
    val configArg = parseConfigArg(args)
    embeddedServer(Netty, 8080) {
        module(configArg)
    }.start(wait = true)
}

private fun Application.module(configArg: String? = null) {
    val appConfig = parseAppConfig(configArg)
    val pollService = PollService()

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
    }
}
