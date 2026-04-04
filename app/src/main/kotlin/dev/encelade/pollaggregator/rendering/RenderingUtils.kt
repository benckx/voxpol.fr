package dev.encelade.pollaggregator.rendering

import dev.encelade.wikiscrapper.Candidate
import kotlinx.html.FlowContent
import kotlinx.html.FlowOrPhrasingContent
import kotlinx.html.HEAD
import kotlinx.html.a
import kotlinx.html.br
import kotlinx.html.footer
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.span
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

fun FlowOrPhrasingContent.candidateWikiAnchor(candidate: Candidate) {
    a(href = "$WIKI_BASE_URL${candidate.wikiSlug}") {
        +candidate.lastName
    }
}

fun FlowContent.renderFooter() {
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
