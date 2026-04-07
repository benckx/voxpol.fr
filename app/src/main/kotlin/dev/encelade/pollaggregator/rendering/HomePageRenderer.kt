package dev.encelade.pollaggregator.rendering

import dev.encelade.pollaggregator.model.PollRecord
import dev.encelade.pollaggregator.services.buildCandidateTrendChartData
import dev.encelade.pollaggregator.services.buildGlobalIntervalsChartData
import dev.encelade.pollaggregator.services.buildLineChartData
import dev.encelade.pollaggregator.services.PollService
import dev.encelade.wikiscrapper.TestingHypothesis
import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.html.respondHtml
import kotlinx.html.*
import kotlinx.serialization.json.Json
import java.time.LocalDate

suspend fun ApplicationCall.renderHomePage(
    pollService: PollService,
    gaEnabled: Boolean,
    trendWindowDays: Int,
) {
    val combinations = pollService.combinationsByRecency()
    val cutoffDate = LocalDate.now().minusDays(365)
    val trendChartData = buildCandidateTrendChartData(
        polls = pollService.allPolls(),
        windowDays = trendWindowDays,
    )
    val globalIntervalsData = buildGlobalIntervalsChartData(pollService.pollsBefore(cutoffDate))
    val distinctDateCountByCombination = combinations.associateWith { combination ->
        pollService.pollsForTestingHypothesis(combination)
            .map { it.dateTo }
            .distinct()
            .size
    }
    val multiPollCombinations = combinations
        .filter { distinctDateCountByCombination.getValue(it) >= 3 }

    val metaDescription = "Agrégateur de sondages pour le premier tour de l'élection présidentielle française de 2027."
    val metaKeywords = "sondages, présidentielle, 2027, premier tour, France, agrégateur, candidats"

    val pageDescription = "voxpol.fr présente les sondages pour le premier tour de l'élection présidentielle de 2027."
    val trendDescription = "Evolution moyenne par candidat entre les $trendWindowDays derniers jours " +
            "et les $trendWindowDays jours precedents. La durée de cette fenêtre sera réduire à l'approche du scrutin."
    val rangeDescription = "Ce graphique présente la plage des intentions de vote " +
            "pour chaque candidat, basée sur les sondages réalisés " +
            "au cours des 365 derniers jours. La barre représente " +
            "l'intervalle entre les intentions de vote les plus basses " +
            "et les plus hautes enregistrées pour chaque candidat."
    val combinationDescription =
        "Les sections sont organisées par hypothèse (c'est-à-dire par combinaison de candidats), en partant du sondage le plus récent."

    respondHtml {
        lang = "fr"
        head {
            meta(charset = "utf-8")
            meta(name = "viewport", content = "width=device-width, initial-scale=1")
            meta(name = "description", content = metaDescription)
            meta(name = "keywords", content = metaKeywords)
            meta(name = "author", content = "benckx")
            title("Sondages Premier Tour 2027 - voxpol.fr")
            link(rel = "stylesheet", href = "/static/style.css", type = ContentType.Text.CSS.toString())
            script(src = "https://cdn.jsdelivr.net/npm/apexcharts") {}
            script(src = "/static/utils.js") { defer = true }
            script(src = "/static/trend-chart.js") { defer = true }
            script(src = "/static/intervals-chart.js") { defer = true }
            script(src = "/static/line-charts.js") { defer = true }
            script(src = "/static/app.js") { defer = true }
            if (gaEnabled) renderGoogleAnalytics()
        }
        body {
            main("container") {
                h1 { +"Sondages pour le Premier Tour de 2027" }
                p { +pageDescription }

                if (trendChartData.stats.isNotEmpty()) {
                    h2 { +"Tendances" }
                    p { +trendDescription }
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
                    // TODO: to un-comment once I tested it
//                    details("embed-info") {
//                        summary { +"Intégrer ce widget" }
//                        p { +"Copiez le code suivant pour intégrer le graphique de tendances sur votre site :" }
//                        val iframeCode = """<iframe src="https://voxpol.fr/embed/trend" width="100%" height="520" frameborder="0" scrolling="no" style="border:none;"></iframe>"""
//                        pre { code { +iframeCode } }
//                    }
                }

                if (globalIntervalsData.stats.isNotEmpty()) {
                    h2 { +"Intervalles" }
                    p { +rangeDescription }
                    section("combination-section") {
                        div("poll-intervals-chart poll-intervals-chart-global") {
                            id = "global-intervals-chart"
                            attributes["data-chart-data-id"] = "global-intervals-data"
                        }
                        script(type = "application/json") {
                            id = "global-intervals-data"
                            attributes["class"] = "poll-chart-data"
                            unsafe {
                                +Json.encodeToString(globalIntervalsData)
                            }
                        }
                    }
                }

                // Render multi-poll sections first
                h2 { +"Hypothèses les plus testées" }
                p { +combinationDescription }
                multiPollCombinations.forEachIndexed { index, combination ->
                    combinationSection(pollService, combination, index)
                }

                renderFooter()
            }
        }
    }
}

// render section for given hypothesis (i.e. combination of candidates),
// with line chart and poll data table
private fun FlowContent.combinationSection(
    pollService: PollService,
    combination: TestingHypothesis,
    sectionIndex: Int,
) {
    val candidatesInOrder = combination.candidatesInOrder
    val pollsForCombo = pollService.pollsForTestingHypothesis(combination)

    fun TR.pollsterCell(poll: PollRecord) {
        val pollsterName = normalizePollsterName(poll.pollster)

        td(classes = "pollster") {
            val sourceUrl = poll.sourceUrl
            if (sourceUrl.isNullOrBlank()) {
                +pollsterName
            } else {
                a(href = sourceUrl) {
                    target = "_blank"
                    +pollsterName
                }
            }
        }
    }

    fun TR.dateCell(poll: PollRecord) {
        val latestDate = maxOf(poll.dateFrom, poll.dateTo)
        val renderedLatestDate = formatDate(latestDate)
        td(classes = "date-cell") { +renderedLatestDate }
    }

    fun TR.candidateHeaders() {
        candidatesInOrder.forEach { candidate ->
            th(classes = "candidate-column") { candidateWikiAnchor(candidate) }
        }
    }

    fun TR.scoreCells(poll: PollRecord) {
        val topScoreLevels = poll.scoresByCandidate.values
            .distinct()
            .sortedDescending()
            .take(2)
            .toSet()

        candidatesInOrder.forEach { candidate ->
            val score = poll.scoresByCandidate[candidate]
            val isTopScore = score != null && score in topScoreLevels
            val scoreCellClasses = if (isTopScore) "score-data top-score" else "score-data"

            td(classes = scoreCellClasses) {
                +(score?.let { "%.1f%%".format(it * 100) } ?: "-")
            }
        }
    }

    fun FlowContent.addLineChart() {
        val chartId = "poll-chart-$sectionIndex"
        val chartDataId = "$chartId-data"

        val hasMultipleDates = pollsForCombo.map { it.dateTo }.distinct().size >= 2
        val shouldAddLineChart = pollsForCombo.size >= 2 && hasMultipleDates
        val pollChartDto = pollsForCombo
            .takeIf { shouldAddLineChart }
            ?.let { polls -> buildLineChartData(candidatesInOrder, polls) }

        pollChartDto?.let { data ->
            div("poll-line-chart") {
                id = chartId
                attributes["data-chart-data-id"] = chartDataId
            }
            script(type = "application/json") {
                id = chartDataId
                attributes["class"] = "poll-chart-data"
                unsafe { +Json.encodeToString(data) }
            }
        }
    }

    section("combination-section") {
        addLineChart()

        div("poll-data-scroll") {
            table("poll-data") {
                thead {
                    tr {
                        th { +"Sondeur" }
                        th { +"Date" }
                        candidateHeaders()
                    }
                }
                tbody {
                    pollsForCombo.forEach { poll ->
                        tr {
                            pollsterCell(poll)
                            dateCell(poll)
                            scoreCells(poll)
                        }
                    }
                }
            }
        }
    }
}
