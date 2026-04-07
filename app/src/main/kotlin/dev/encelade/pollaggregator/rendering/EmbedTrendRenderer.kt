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
    trendWindowDays: Int,
) {
    val trendChartData = buildCandidateTrendChartData(
        polls = pollService.allPolls(),
        windowDays = trendWindowDays,
    )

    respondHtml {
        lang = "fr"
        head {
            meta(charset = "utf-8")
            meta(name = "viewport", content = "width=device-width, initial-scale=1")
            meta(name = "robots", content = "noindex, nofollow")
            title("Tendances Premier Tour - voxpol.fr")
            link(rel = "stylesheet", href = "/static/style.css", type = ContentType.Text.CSS.toString())
            link(rel = "stylesheet", href = "/static/embed/embed.css", type = ContentType.Text.CSS.toString())
            script(src = "https://cdn.jsdelivr.net/npm/apexcharts") {}
            script(src = "/static/utils.js") { defer = true }
            script(src = "/static/trend-chart.js") { defer = true }
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
