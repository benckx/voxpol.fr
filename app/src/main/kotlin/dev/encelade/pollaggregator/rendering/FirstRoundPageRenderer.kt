package dev.encelade.pollaggregator.rendering

import dev.encelade.pollaggregator.model.CandidateTrendChartDto
import dev.encelade.pollaggregator.model.GlobalIntervalsChartDto
import dev.encelade.pollaggregator.services.buildCandidateTrendChartData
import dev.encelade.pollaggregator.services.buildGlobalIntervalsChartData
import dev.encelade.pollaggregator.services.PollService
import dev.encelade.pollaggregator.services.buildQualificationThresholdChartData
import io.ktor.server.application.ApplicationCall
import io.ktor.server.html.respondHtml
import kotlinx.html.*
import kotlinx.serialization.json.Json
import java.time.LocalDate

suspend fun ApplicationCall.renderFirstRoundPage(
    pollService: PollService,
    gaEnabled: Boolean,
    trendWindowDays: Int,
    canonicalUrl: String,
) {
    val testingHypotheses = pollService.combinationsByRecency().filter { it.candidates.size > 2 }
    val cutoffDate = LocalDate.now().minusDays(365)
    val distinctDateCountByCombination = testingHypotheses.associateWith { testingHypothesis ->
        pollService.pollsForTestingHypothesis(testingHypothesis)
            .map { it.dateTo }
            .distinct()
            .size
    }

    val trendDescription = "Evolution moyenne par candidat entre les $trendWindowDays derniers jours " +
            "et les $trendWindowDays jours precedents. La durée de cette fenêtre sera réduire à l'approche du scrutin."
    val trendChartData = buildCandidateTrendChartData(
        polls = pollService.getFirstRoundPolls(),
        windowDays = trendWindowDays,
    )

    val rangeDescription = "Ce graphique présente la plage des intentions de vote " +
            "pour chaque candidat, basée sur les sondages réalisés " +
            "au cours des 365 derniers jours. La barre représente " +
            "l'intervalle entre les intentions de vote les plus basses " +
            "et les plus hautes enregistrées pour chaque candidat."
    val globalIntervalsData = buildGlobalIntervalsChartData(pollService.getFirstRoundPollsBefore(cutoffDate))

    val sectionsDescription =
        "Les sections sont organisées par hypothèse (c'est-à-dire par combinaison de candidats), en partant du sondage le plus récent."
    val testingHypothesesToRender = testingHypotheses
        .filter { distinctDateCountByCombination.getValue(it) >= 3 }

    val thresholdData = buildQualificationThresholdChartData(pollService.getFirstRoundPolls())

    respondHtml {
        lang = "fr"
        head {
            renderCommonHead(gaEnabled)
            link(rel = "canonical", href = canonicalUrl) {}
            meta(
                name = "description",
                content = "Agrégateur de sondages pour le premier tour de l'élection présidentielle française de 2027."
            )
            title("Sondages Premier Tour 2027 - voxpol.fr")
            script(src = "/static/app.js") { defer = true }
        }
        body {
            renderSiteHeader("/premier-tour-2027")
            main("container") {
                renderTrendWidget(trendDescription, trendChartData)
                renderRangeWidget(rangeDescription, globalIntervalsData)

                // Render line charts sections
                h2 { +"Hypothèses les plus testées" }
                p { +sectionsDescription }
                testingHypothesesToRender.forEachIndexed { index, testingHypothesis ->
                    renderLineChartsAndTableForHypothesis(
                        pollService.pollsForTestingHypothesis(testingHypothesis),
                        testingHypothesis,
                        index
                    )
                }

                renderSecondRoundThresholdChart(thresholdData)
                renderFooter()
            }
        }
    }
}

private fun FlowContent.renderTrendWidget(description: String, dto: CandidateTrendChartDto) {
    if (dto.stats.isNotEmpty()) {
        h2 { +"Tendances" }
        p { +description }
        renderTrendWidget(dto)
        details("embed-info") {
            summary { +"Intégrer ce widget à votre site" }
            p { +"Copiez le code suivant pour intégrer le graphique de tendances:" }
            val iframeCode =
                """<iframe src="https://voxpol.fr/embed/trend" width="100%" height="700" frameborder="0" style="border:none;"></iframe>"""
            pre { code { +iframeCode } }
        }
    }
}

private fun FlowContent.renderRangeWidget(description: String, dto: GlobalIntervalsChartDto) {
    if (dto.stats.isNotEmpty()) {
        h2 { +"Intervalles" }
        p { +description }
        section("combination-section") {
            div("poll-intervals-chart poll-intervals-chart-global") {
                id = "global-intervals-chart"
                attributes["data-chart-data-id"] = "global-intervals-data"
            }
            script(type = "application/json") {
                id = "global-intervals-data"
                attributes["class"] = "poll-chart-data"
                unsafe {
                    +Json.encodeToString(dto)
                }
            }
        }
    }
}
