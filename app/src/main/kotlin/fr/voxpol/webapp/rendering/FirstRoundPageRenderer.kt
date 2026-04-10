package fr.voxpol.webapp.rendering

import fr.voxpol.webapp.AppConfig
import fr.voxpol.webapp.services.PollService
import fr.voxpol.webapp.services.buildCandidateTrendChartData
import fr.voxpol.webapp.services.buildGlobalIntervalsChartData
import fr.voxpol.webapp.services.buildQualificationThresholdChartData
import fr.voxpol.webapp.utils.koin
import fr.voxpol.webapp.utils.respondHtmlCached
import io.ktor.server.application.*
import kotlinx.html.*
import kotlinx.serialization.json.Json
import java.time.LocalDate

private val pollService by koin<PollService>()
private val appConfig by koin<AppConfig>()

private const val CANONICAL_URL = "https://voxpol.fr/premier-tour-2027"

suspend fun ApplicationCall.renderFirstRoundPage() = respondHtmlCached {
    // config
    val gaEnabled = appConfig.gaEnabled
    val minified = appConfig.minified

    // second round threshold
    val thresholdData = buildQualificationThresholdChartData(pollService.getFirstRoundPolls())

    lang = "fr"
    head {
        renderCommonHead(gaEnabled, minified)
        link(rel = "canonical", href = CANONICAL_URL) {}
        meta(
            name = "description",
            content = "Agrégateur de sondages pour le premier tour de l'élection présidentielle française de 2027."
        )
        title("Sondages Premier Tour 2027 - voxpol.fr")
        script(src = minPath("/static/app.js", minified)) { defer = true }
    }
    body {
        renderSiteHeader("/premier-tour-2027")
        main("container") {
            renderTrendWidget()
            renderRangeWidget()
            renderLineCharts()
            renderSecondRoundThresholdChart(thresholdData)
            renderFooter()
        }
    }
}

private fun FlowContent.renderTrendWidget() {
    val trendWindowDays = appConfig.trendWindowDays
    val trendChartDto = buildCandidateTrendChartData(pollService.getFirstRoundPolls(), trendWindowDays)

    if (trendChartDto.stats.isNotEmpty()) {
        h2 { +"Tendances" }
        p {
            +("Evolution moyenne par candidat entre les $trendWindowDays derniers jours " +
                    "et les $trendWindowDays jours precedents. La durée de cette fenêtre sera réduire à l'approche du scrutin.")
        }
        renderTrendWidget(trendChartDto)
        details("embed-info") {
            summary { +"Intégrer ce widget à votre site" }
            p { +"Copiez le code suivant pour intégrer le graphique de tendances:" }
            val iframeCode =
                """<iframe src="https://voxpol.fr/embed/trend" width="100%" height="700" frameborder="0" style="border:none;"></iframe>"""
            pre { code { +iframeCode } }
        }
    }
}

private fun FlowContent.renderRangeWidget() {
    val intervalsCutOffDate = LocalDate.now().minusDays(365)
    val dto = buildGlobalIntervalsChartData(pollService.getFirstRoundPollsBefore(intervalsCutOffDate))

    if (dto.stats.isNotEmpty()) {
        val description = "Ce graphique présente la plage des intentions de vote " +
                "pour chaque candidat, basée sur les sondages réalisés " +
                "au cours des 365 derniers jours. La barre représente " +
                "l'intervalle entre les intentions de vote les plus basses " +
                "et les plus hautes enregistrées pour chaque candidat."

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

private fun FlowContent.renderLineCharts() {
    val testingHypotheses = pollService.combinationsByRecency().filter { it.candidates.size > 2 }
    val distinctDateCountByCombination = testingHypotheses.associateWith { testingHypothesis ->
        pollService.pollsForTestingHypothesis(testingHypothesis)
            .map { it.dateTo }
            .distinct()
            .size
    }
    val description =
        "Les sections sont organisées par hypothèse (c'est-à-dire par combinaison de candidats), en partant du sondage le plus récent."
    val testingHypothesesToRender = testingHypotheses.filter { distinctDateCountByCombination.getValue(it) >= 3 }

    h2 { +"Hypothèses les plus testées" }
    p { +description }
    testingHypothesesToRender.forEachIndexed { index, testingHypothesis ->
        renderLineChartsAndTableForHypothesis(
            pollService.pollsForTestingHypothesis(testingHypothesis),
            testingHypothesis,
            index
        )
    }
}
