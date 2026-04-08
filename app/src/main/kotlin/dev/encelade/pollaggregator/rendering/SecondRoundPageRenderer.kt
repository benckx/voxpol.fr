package dev.encelade.pollaggregator.rendering

import dev.encelade.pollaggregator.services.PollService
import io.ktor.server.application.ApplicationCall
import io.ktor.server.html.respondHtml
import kotlinx.html.*

suspend fun ApplicationCall.renderSecondRoundPage(
    pollService: PollService,
    gaEnabled: Boolean,
) {
    val testingHypotheses = pollService.combinationsByRecency().filter { it.candidates.size == 2 }

    respondHtml {
        lang = "fr"
        head {
            renderCommonHead(gaEnabled)
            meta(
                name = "description",
                content = "Agrégateur de sondages pour le second tour de l'élection présidentielle française de 2027."
            )
            title("Sondages Second Tour 2027 - voxpol.fr")
            script(src = "/static/app-second-round.js") { defer = true }
        }
        body {
            renderSiteHeader("/second-tour-2027")
            main("container") {

                if (testingHypotheses.isEmpty()) {
                    p { +"Aucune donnée disponible pour le moment." }
                } else {
                    h2 { +"Hypothèses de second tour" }
                    testingHypotheses.forEachIndexed { index, testingHypothesis ->
                        val title = testingHypothesis.candidatesInOrder.joinToString(" vs ") { it.lastName }
                        renderLineChartsAndTableForHypothesis(
                            pollsForCombo = pollService.pollsForTestingHypothesis(testingHypothesis),
                            testingHypothesis = testingHypothesis,
                            sectionIndex = index,
                            chartIdPrefix = "poll-chart-sr",
                            title = title,
                            highlightTopScores = 1,
                        )
                    }
                }

                renderFooter()
            }
        }
    }
}
