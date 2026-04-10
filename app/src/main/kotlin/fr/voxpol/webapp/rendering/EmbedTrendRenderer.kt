package fr.voxpol.webapp.rendering

import fr.voxpol.webapp.AppConfig
import fr.voxpol.webapp.services.PollService
import fr.voxpol.webapp.services.buildCandidateTrendChartData
import fr.voxpol.webapp.utils.koin
import fr.voxpol.webapp.utils.respondHtmlCached
import io.ktor.http.*
import io.ktor.server.application.*
import kotlinx.html.*

private val appConfig by koin<AppConfig>()
private val pollService: PollService by koin()

suspend fun ApplicationCall.renderTrendEmbed() = respondHtmlCached {
    val gaEnabled = appConfig.gaEnabled
    val trendWindowDays = appConfig.trendWindowDays
    val minified = appConfig.minified

    val trendChartData = buildCandidateTrendChartData(
        polls = pollService.getFirstRoundPolls(),
        windowDays = trendWindowDays,
    )

    lang = "fr"
    head {
        renderCommonHead(gaEnabled, minified)
        meta(name = "robots", content = "noindex, nofollow")
        title("Tendances Premier Tour - voxpol.fr")
        link(
            rel = "stylesheet",
            href = minPath("/static/embed/embed.css", minified),
            type = ContentType.Text.CSS.toString()
        )
        script(src = minPath("/static/embed/embed-trend.js", minified)) { defer = true }
    }
    body {
        main("container") {
            renderTrendWidget(trendChartData)
            p("embed-footer") {
                a(href = "https://voxpol.fr") {
                    target = "_blank"
                    +"voxpol.fr"
                }
            }
        }
    }
}
