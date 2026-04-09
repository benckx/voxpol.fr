package dev.encelade.pollaggregator.rendering

import dev.encelade.pollaggregator.services.PollService
import dev.encelade.pollaggregator.services.buildCandidateTrendChartData
import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.html.respondHtml
import kotlinx.html.*

suspend fun ApplicationCall.renderTrendEmbed(
    pollService: PollService,
    gaEnabled: Boolean,
    trendWindowDays: Int,
    minified: Boolean = false,
) {
    val trendChartData = buildCandidateTrendChartData(
        polls = pollService.getFirstRoundPolls(),
        windowDays = trendWindowDays,
    )

    respondHtml {
        lang = "fr"
        head {
            renderCommonHead(gaEnabled, minified)
            meta(name = "robots", content = "noindex, nofollow")
            title("Tendances Premier Tour - voxpol.fr")
            link(rel = "stylesheet", href = minPath("/static/embed/embed.css", minified), type = ContentType.Text.CSS.toString())
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
}
