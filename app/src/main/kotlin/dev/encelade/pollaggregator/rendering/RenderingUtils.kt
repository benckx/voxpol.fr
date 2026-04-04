package dev.encelade.pollaggregator.rendering

import dev.encelade.wikiscrapper.Candidate
import kotlinx.html.FlowOrPhrasingContent
import kotlinx.html.HEAD
import kotlinx.html.a
import kotlinx.html.script
import kotlinx.html.unsafe
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val GA_MEASUREMENT_ID = "G-KF6KNYL244"
private const val WIKI_BASE_URL = "https://fr.wikipedia.org/wiki/"

private val DATE_WITH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("d MMM yy", Locale.FRENCH)
private val DATE_WITHOUT_YEAR_FORMATTER = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)

fun formatDate(date: LocalDate): String {
    val currentYear = LocalDate.now().year
    val formatter = if (date.year == currentYear) DATE_WITHOUT_YEAR_FORMATTER else DATE_WITH_YEAR_FORMATTER
    return date.format(formatter)
}

fun FlowOrPhrasingContent.candidateWikiAnchor(candidate: Candidate) {
    a(href = "$WIKI_BASE_URL${candidate.wikiSlug}") {
        +candidate.lastName
    }
}

fun normalizePollsterName(pollster: String): String {
    return when (pollster) {
        "Harris Interactive", "Harris-Interactive" -> "Harris"
        else -> pollster
    }
}

fun HEAD.renderGoogleAnalytics() {
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
