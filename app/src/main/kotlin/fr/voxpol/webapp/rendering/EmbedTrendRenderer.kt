package fr.voxpol.webapp.rendering

import fr.voxpol.webapp.AppConfig
import fr.voxpol.webapp.services.HtmlCache
import fr.voxpol.webapp.services.HtmlCacheKey
import fr.voxpol.webapp.services.PollService
import fr.voxpol.webapp.services.buildCandidateTrendChartData
import fr.voxpol.webapp.services.respondHtmlCached
import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import kotlinx.html.*

suspend fun ApplicationCall.renderTrendEmbed(
    pollService: PollService,
    appConfig: AppConfig,
    htmlCache: HtmlCache,
) {
    val (gaEnabled, trendWindowDays, _, _, minified) = appConfig
    val trendChartData = buildCandidateTrendChartData(
        polls = pollService.getFirstRoundPolls(),
        windowDays = trendWindowDays,
    )

    respondHtmlCached(htmlCache, HtmlCacheKey.EMBED_TREND) {
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
