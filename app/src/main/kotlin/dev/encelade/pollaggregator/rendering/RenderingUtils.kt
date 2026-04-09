package dev.encelade.pollaggregator.rendering

import dev.encelade.pollaggregator.model.CandidateTrendChartDto
import dev.encelade.pollaggregator.model.PollRecord
import dev.encelade.pollaggregator.model.ThresholdChartDto
import dev.encelade.pollaggregator.services.buildLineChartData
import dev.encelade.wikiscrapper.Candidate
import dev.encelade.wikiscrapper.TestingHypothesis
import io.ktor.http.ContentType
import kotlinx.html.*
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val GA_MEASUREMENT_ID = "G-KF6KNYL244"
private const val WIKI_BASE_URL = "https://fr.wikipedia.org/wiki/"

private val DATE_WITH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("d MMM yy", Locale.FRENCH)
private val DATE_WITHOUT_YEAR_FORMATTER = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)

internal fun formatDate(date: LocalDate): String {
    val currentYear = LocalDate.now().year
    val formatter = if (date.year == currentYear) DATE_WITHOUT_YEAR_FORMATTER else DATE_WITH_YEAR_FORMATTER
    return date.format(formatter)
}

internal fun normalizePollsterName(pollster: String): String {
    return when (pollster) {
        "Harris Interactive", "Harris-Interactive" -> "Harris"
        else -> pollster
    }
}

internal fun FlowOrPhrasingContent.candidateWikiAnchor(candidate: Candidate) {
    a(href = "$WIKI_BASE_URL${candidate.wikiSlug}") {
        +candidate.lastName
    }
}

internal fun HEAD.renderCommonHead(gaEnabled: Boolean) {
    val metaKeywords = "sondages, présidentielle, 2027, premier tour, second tour, France, agrégateur, candidats"

    meta(charset = "utf-8")
    meta(name = "viewport", content = "width=device-width, initial-scale=1")
    meta(name = "author", content = "benckx")
    meta(name = "keywords", content = metaKeywords)
    link(rel = "stylesheet", href = "/static/style.css", type = ContentType.Text.CSS.toString())
    script(src = "https://cdn.jsdelivr.net/npm/apexcharts") {}
    script(src = "/static/utils.js") { defer = true }
    script(src = "/static/trend-chart.js") { defer = true }
    script(src = "/static/intervals-chart.js") { defer = true }
    script(src = "/static/line-charts.js") { defer = true }
    script(src = "/static/threshold-chart.js") { defer = true }
    if (gaEnabled) renderGoogleAnalytics()
}

private fun HEAD.renderGoogleAnalytics() {
    val script = """
      window.dataLayer = window.dataLayer || [];
      function gtag(){dataLayer.push(arguments);}
      gtag('js', new Date());
      gtag('config', '$GA_MEASUREMENT_ID');
    """.trimIndent()

    script(src = "https://www.googletagmanager.com/gtag/js?id=$GA_MEASUREMENT_ID") {
        attributes["async"] = "true"
    }
    script { unsafe { +script } }
}

internal fun FlowContent.renderSiteHeader(activePath: String = "") {
    header("site-header") {
        div("site-header-inner") {
            a(href = "/", classes = "site-logo-link") {
                div("vox-logo") {
                    span("v letter") { +"V" }
                    span("o letter") { +"O" }
                    span("x letter") { +"X" }
                    span("pol") { +"pol.fr" }
                }
            }
            nav("site-nav") {
                val firstClass = if (activePath.contains("premier")) "active" else ""
                val secondClass = if (activePath.contains("second")) "active" else ""
                a(href = "/premier-tour-2027", classes = firstClass) { +"Premier Tour 2027" }
                a(href = "/second-tour-2027", classes = secondClass) { +"Second Tour 2027" }
            }
        }
    }
}

internal fun FlowContent.renderFooter() {
    footer("site-footer") {
        p {
            +"Site sans pub, car je n'aime pas la pub."
            br {}
            +"Mais j'aime le "
            a(href = "https://www.paypal.com/paypalme/benckx/4") {
                target = "_blank"
                rel = "noopener noreferrer"
                +"café"
            }
            +" si jamais."
        }
        p {
            a(href = "https://github.com/benckx/voxpol.fr") {
                target = "_blank"
                rel = "noopener noreferrer"
                +"GitHub"
            }
            span { +" | " }
            a(href = "https://benckx.me") {
                target = "_blank"
                rel = "noopener noreferrer"
                +"Contact"
            }
        }
    }
}

internal fun FlowContent.renderLineChartsAndTableForHypothesis(
    pollsForTestingHypothesis: List<PollRecord>,
    testingHypothesis: TestingHypothesis,
    sectionIndex: Int,
    chartIdPrefix: String = "poll-chart",
    title: String? = null,
    highlightTopScores: Int = 2,
) {
    val candidatesInOrder = testingHypothesis.candidatesInOrder

    fun TR.pollsterCell(poll: PollRecord) {
        td(classes = "pollster") {
            val pollsterName = normalizePollsterName(poll.pollster)
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
        td(classes = "date-cell") { +formatDate(maxOf(poll.dateFrom, poll.dateTo)) }
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
            .take(highlightTopScores)
            .toSet()

        candidatesInOrder.forEach { candidate ->
            val score = poll.scoresByCandidate[candidate]
            val cssClass = if (score != null && score in topScoreLevels) "score-data top-score" else "score-data"
            td(classes = cssClass) {
                +(score?.let { "%.1f%%".format(it * 100) } ?: "-")
            }
        }
    }

    fun FlowContent.lineChart() {
        val chartId = "$chartIdPrefix-$sectionIndex"
        val chartDataId = "$chartId-data"
        val hasMultipleDates = pollsForTestingHypothesis.map { it.dateTo }.distinct().size >= 2
        if (pollsForTestingHypothesis.size < 2 || !hasMultipleDates) return

        val data = buildLineChartData(candidatesInOrder, pollsForTestingHypothesis)
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

    section("combination-section") {
        if (title != null) h3 { +title }
        lineChart()
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
                    pollsForTestingHypothesis.forEach { poll ->
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

internal fun FlowContent.renderSecondRoundThresholdChart(dto: ThresholdChartDto) {
    fun FlowContent.renderQualificationThresholdSection() {
        val chartId = "threshold-chart"
        val chartDataId = "$chartId-data"
        section("combination-section") {
            div("threshold-line-chart") {
                id = chartId
                attributes["data-chart-data-id"] = chartDataId
            }
            script(type = "application/json") {
                id = chartDataId
                unsafe { +Json.encodeToString(dto) }
            }
        }
    }


    if (dto.data.size >= 2) {
        h2 { +"Seuil d'accès au second tour" }
        p { +"Score moyen du 2ème candidat dans les sondages du premier tour, agrégé par mois. " }
        renderQualificationThresholdSection()
    }
}

internal fun FlowContent.renderTrendWidget(dto: CandidateTrendChartDto) {
    section("combination-section") {
        div("candidate-trend-chart") {
            id = "candidate-trend-chart"
            attributes["data-chart-data-id"] = "candidate-trend-data"
        }
        script(type = "application/json") {
            id = "candidate-trend-data"
            attributes["class"] = "poll-chart-data"
            unsafe { +Json.encodeToString(dto) }
        }
    }
}
