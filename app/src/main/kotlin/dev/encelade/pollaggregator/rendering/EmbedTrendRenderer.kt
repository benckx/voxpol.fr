package dev.encelade.pollaggregator.rendering

import dev.encelade.pollaggregator.services.PollService
import dev.encelade.pollaggregator.services.buildCandidateTrendChartData
import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.html.respondHtml
import kotlinx.html.*
import kotlinx.serialization.json.Json

suspend fun ApplicationCall.renderTrendEmbed(
    pollService: PollService,
    gaEnabled: Boolean,
    trendWindowDays: Int,
) {
    val trendChartData = buildCandidateTrendChartData(
        polls = pollService.getFirstRoundPolls(),
        windowDays = trendWindowDays,
    )

    respondHtml {
        lang = "fr"
        head {
            renderCommonHead(gaEnabled)
            meta(name = "robots", content = "noindex, nofollow")
            title("Tendances Premier Tour - voxpol.fr")
            link(rel = "stylesheet", href = "/static/embed/embed.css", type = ContentType.Text.CSS.toString())
            script(src = "/static/embed/embed-trend.js") { defer = true }
        }
        body {
            main("container") {
                section("combination-section") {
                    div("candidate-trend-chart") {
                        id = "candidate-trend-chart"
                        attributes["data-chart-data-id"] = "candidate-trend-data"
                    }
                    script(type = "application/json") {
                        id = "candidate-trend-data"
                        attributes["class"] = "poll-chart-data"
                        unsafe { +Json.encodeToString(trendChartData) }
                    }
                }
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
