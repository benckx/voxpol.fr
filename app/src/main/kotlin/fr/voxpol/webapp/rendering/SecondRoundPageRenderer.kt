package fr.voxpol.webapp.rendering

import fr.voxpol.webapp.AppConfig
import fr.voxpol.webapp.utils.koin
import fr.voxpol.webapp.services.*
import io.ktor.server.application.*
import kotlinx.html.*

private val pollService: PollService by koin()
private val htmlCache: HtmlCache by koin()
private val appConfig: AppConfig by koin()

suspend fun ApplicationCall.renderSecondRoundPage() {
    val gaEnabled = appConfig.gaEnabled
    val minified = appConfig.minified

    val thresholdData = buildQualificationThresholdChartData(pollService.getFirstRoundPolls())

    respondHtmlCached(htmlCache) {
        lang = "fr"
        head {
            renderCommonHead(gaEnabled, minified)
            meta(
                name = "description",
                content = "Agrégateur de sondages pour le second tour de l'élection présidentielle française de 2027."
            )
            title("Sondages Second Tour 2027 - voxpol.fr")
            script(src = minPath("/static/app-second-round.js", minified)) { defer = true }
        }
        body {
            renderSiteHeader("/second-tour-2027")
            main("container") {
                renderLineChartsAndTableForHypotheses()
                renderSecondRoundThresholdChart(thresholdData)
                renderFooter()
            }
        }
    }
}

private fun FlowContent.renderLineChartsAndTableForHypotheses() {
    val testingHypotheses = pollService.combinationsByRecency().filter { it.candidates.size == 2 }

    if (testingHypotheses.isEmpty()) {
        p { +"Aucune donnée disponible pour le moment." }
    } else {
        h2 { +"Hypothèses de second tour" }
        testingHypotheses.forEachIndexed { index, testingHypothesis ->
            val title = testingHypothesis.candidatesInOrder.joinToString(" vs ") { it.lastName }
            val pollsForTestingHypothesis = pollService.pollsForTestingHypothesis(testingHypothesis)

            renderLineChartsAndTableForHypothesis(
                pollsForTestingHypothesis = pollsForTestingHypothesis,
                testingHypothesis = testingHypothesis,
                sectionIndex = index,
                chartIdPrefix = "poll-chart-sr",
                title = title,
                highlightTopScores = 1,
            )
        }
    }
}
