package fr.voxpol.webapp.rendering

import fr.voxpol.webapp.AppConfig
import fr.voxpol.webapp.services.PollService
import fr.voxpol.webapp.services.buildQualificationThresholdChartData
import io.ktor.server.application.*
import io.ktor.server.html.*
import kotlinx.html.*

suspend fun ApplicationCall.renderSecondRoundPage(
    pollService: PollService,
    appConfig: AppConfig,
) {
    val (gaEnabled, _, _, _, minified) = appConfig
    val testingHypotheses = pollService.combinationsByRecency().filter { it.candidates.size == 2 }
    val thresholdData = buildQualificationThresholdChartData(pollService.getFirstRoundPolls())

    fun FlowContent.renderLineChartsAndTableForHypotheses() {
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

    respondHtml {
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
