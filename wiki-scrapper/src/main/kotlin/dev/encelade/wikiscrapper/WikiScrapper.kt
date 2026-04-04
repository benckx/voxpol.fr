package dev.encelade.wikiscrapper

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URLDecoder
import java.text.Normalizer
import java.time.LocalDate

private const val URL =
    "https://fr.wikipedia.org/wiki/Liste_de_sondages_sur_l'%C3%A9lection_pr%C3%A9sidentielle_fran%C3%A7aise_de_2027"

class WikiScrapper {

    private val logger = KotlinLogging.logger {}

    fun fetchAllStudies(): List<Study> =
        fetchFirstRoundPollTables().flatMap { parseStudies(it) }

    fun fetchFirstRoundPollTables(): List<Element> {
        val doc = Jsoup.connect(URL)
            .userAgent("Mozilla/5.0 (compatible; poll-aggregator/1.0)")
            .get()

        return doc.select("table.wikitable")
            .filter { table ->
                val firstRow = table.selectFirst("tr")
                val headers = firstRow?.select("th") ?: return@filter false
                headers.size > 5 &&
                        headers.first()?.text() == "Sondeur" &&
                        extractYearForTable(table) != null
            }
    }

    fun parseColumnMapping(table: Element): Map<Int, Candidate> {
        val candidatesBySlug = Candidate.entries.associateBy { it.wikiSlug }
        val rows = table.select("tr")
        if (rows.size < 2) return emptyMap()

        val mapping = mutableMapOf<Int, Candidate>()
        var absoluteColumnIndex = 3

        rows[1].select("th").forEach { th ->
            val colspan = th.attr("colspan").toIntOrNull()?.coerceAtLeast(1) ?: 1

            val slug = th.selectFirst("a[href^=/wiki/]")
                ?.attr("href")
                ?.removePrefix("/wiki/")
                ?.substringBefore("#")
                ?.let { URLDecoder.decode(it, "UTF-8") }

            val candidate = slug?.let { candidatesBySlug[it] }
            if (candidate != null) {
                repeat(colspan) { offset ->
                    mapping[absoluteColumnIndex + offset] = candidate
                }
            }

            absoluteColumnIndex += colspan
        }

        return mapping
    }

    fun parseStudies(table: Element): List<Study> {
        val rows = table.select("tr")
        if (rows.size <= 3) return emptyList()

        val year = extractYearForTable(table)
        val candidatesBySlug = Candidate.entries.associateBy { it.wikiSlug }
        val columnMapping = parseColumnMapping(table)
        val studies = mutableListOf<Study>()
        var currentStudy: MutableStudy? = null
        var currentPollster: String? = null
        var currentSourceUrl: String? = null

        rows.drop(3).forEach { row ->
            val cells = row.select("td")
            if (cells.isEmpty()) return@forEach
            if (cells.size == 1) return@forEach

            val resultCells: List<Element>

            if (isFullStudyStartRow(cells, year)) {
                currentStudy?.let { studies += it.toImmutable() }

                val pollsterCell = cells[0]
                val (minDate, maxDate) = parseDateRange(cells[1].text().normalizeCellText(), year)
                currentPollster = extractPollsterName(pollsterCell)
                currentSourceUrl = extractSourceUrl(pollsterCell)
                currentStudy = MutableStudy(
                    pollster = currentPollster,
                    sourceUrl = currentSourceUrl,
                    minDate = minDate,
                    maxDate = maxDate,
                    sampleSize = parseSampleSize(cells[2].text()),
                )
                resultCells = cells.drop(3)
            } else if (isSplitStudyStartRow(cells, year)) {
                currentStudy?.let { studies += it.toImmutable() }

                val pollster = currentPollster ?: return@forEach
                val (minDate, maxDate) = parseDateRange(cells[0].text().normalizeCellText(), year)
                currentStudy = MutableStudy(
                    pollster = pollster,
                    sourceUrl = currentSourceUrl,
                    minDate = minDate,
                    maxDate = maxDate,
                    sampleSize = parseSampleSize(cells[1].text()),
                )
                resultCells = cells.drop(2)
            } else {
                resultCells = cells
            }

            val hasAmbiguousMergedResultCell = resultCells.any { cell ->
                val colspan = cell.attr("colspan").toIntOrNull() ?: 1
                colspan > 1 && extractCandidateFromCell(cell, candidatesBySlug) == null
            }
            if (hasAmbiguousMergedResultCell) {
                return@forEach
            }

            val results = mutableMapOf<Candidate, Double>()
            var absoluteColumnIndex = 3

            resultCells.forEach { cell ->
                val colspan = cell.attr("colspan").toIntOrNull()?.coerceAtLeast(1) ?: 1
                val candidate = extractCandidateFromCell(cell, candidatesBySlug)
                    ?: columnMapping[absoluteColumnIndex]

                if (candidate != null) {
                    val score = parseScore(cell.text())
                    if (score != null) {
                        results[candidate] = score
                    }
                }

                absoluteColumnIndex += colspan
            }

            if (results.isNotEmpty()) {
                val poll = Poll(results)
                if (!poll.isSummedCloseToOne()) {
                    logger.warn {
                        "Poll sum out of tolerance for ${currentStudy?.pollster ?: "unknown"} " +
                                "(${currentStudy?.minDate ?: "?"} to ${currentStudy?.maxDate ?: "?"}): ${poll.summed()}"
                    }
                }
                currentStudy?.polls?.add(poll)
            }
        }

        currentStudy?.let { studies += it.toImmutable() }
        return studies
    }

    private fun isFullStudyStartRow(cells: List<Element>, fallbackYear: Int?): Boolean {
        if (cells.size < 3) return false
        val firstCellText = cells[0].text().normalizeCellText()
        val secondCellText = cells[1].text().normalizeCellText()
        val thirdCellText = cells[2].text().normalizeCellText()

        return parseDateRangeOrNull(firstCellText, fallbackYear) == null &&
                parseDateRangeOrNull(secondCellText, fallbackYear) != null &&
                isLikelySampleSize(thirdCellText)
    }

    private fun isSplitStudyStartRow(cells: List<Element>, fallbackYear: Int?): Boolean {
        if (cells.size < 2) return false
        val firstCellText = cells[0].text().normalizeCellText()
        val secondCellText = cells[1].text().normalizeCellText()

        return parseDateRangeOrNull(firstCellText, fallbackYear) != null &&
                isLikelySampleSize(secondCellText)
    }

    private fun isLikelySampleSize(text: String): Boolean {
        return SAMPLE_SIZE_REGEX.containsMatchIn(text)
    }

    private fun parseDateRangeOrNull(date: String, fallbackYear: Int?): Pair<LocalDate, LocalDate>? {
        return runCatching { parseDateRange(date, fallbackYear) }.getOrNull()
    }

    private fun extractCandidateFromCell(
        cell: Element,
        candidatesBySlug: Map<String, Candidate>
    ): Candidate? {
        val slug = cell.selectFirst("a[href^=/wiki/]")
            ?.attr("href")
            ?.removePrefix("/wiki/")
            ?.substringBefore("#")
            ?.let { URLDecoder.decode(it, "UTF-8") }
            ?: return null

        return candidatesBySlug[slug]
    }

    private fun extractPollsterName(pollsterCell: Element): String {
        return pollsterCell.selectFirst("a.external.text")?.text()?.normalizeCellText()
            ?: pollsterCell.selectFirst("a")?.text()?.normalizeCellText()
            ?: pollsterCell.text().normalizeCellText()
    }

    private fun extractSourceUrl(pollsterCell: Element): String? {
        return pollsterCell.selectFirst("a.external.text")?.attr("abs:href")
            ?.takeIf { it.isNotBlank() }
    }

    private fun parseScore(text: String): Double? {
        val normalized = text.normalizeCellText()
        if (normalized == "-") return null

        val number = NUMBER_REGEX.find(normalized)?.value ?: return null
        return number.replace(',', '.').toDoubleOrNull()?.div(100.0)
    }

    private fun parseSampleSize(text: String): Int {
        val digits = text.normalizeCellText().filter { it.isDigit() }
        require(digits.isNotEmpty()) { "Unsupported sample size: '$text'" }
        return digits.toInt()
    }

    private fun String.normalizeCellText(): String {
        return replace('\u00a0', ' ')
            .replace('\u202f', ' ')
            .replace('—', '-')
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun parseDateRange(date: String, fallbackYear: Int?): Pair<LocalDate, LocalDate> {
        val normalized = date.normalizeCellText()

        SINGLE_DAY_REGEX.matchEntire(normalized)?.let { match ->
            val day = parseDay(match.groupValues[1])
            val month = parseMonth(match.groupValues[2])
            val year = parseYear(match.groupValues[3], fallbackYear, normalized)
            val parsedDate = LocalDate.of(year, month, day)
            return parsedDate to parsedDate
        }

        SAME_MONTH_RANGE_REGEX.matchEntire(normalized)?.let { match ->
            val startDay = parseDay(match.groupValues[1])
            val endDay = parseDay(match.groupValues[2])
            val month = parseMonth(match.groupValues[3])
            val year = parseYear(match.groupValues[4], fallbackYear, normalized)
            val startDate = LocalDate.of(year, month, startDay)
            val endDate = LocalDate.of(year, month, endDay)
            require(!endDate.isBefore(startDate)) {
                "Invalid French date range: '$date'"
            }
            return startDate to endDate
        }

        CROSS_MONTH_RANGE_REGEX.matchEntire(normalized)?.let { match ->
            val startDay = parseDay(match.groupValues[1])
            val startMonth = parseMonth(match.groupValues[2])
            val endDay = parseDay(match.groupValues[3])
            val endMonth = parseMonth(match.groupValues[4])
            val year = parseYear(match.groupValues[5], fallbackYear, normalized)
            val startDate = LocalDate.of(year, startMonth, startDay)
            val endDate = LocalDate.of(year, endMonth, endDay)
            require(!endDate.isBefore(startDate)) {
                "Invalid French date range: '$date'"
            }
            return startDate to endDate
        }

        throw IllegalArgumentException("Unsupported French date format: '$date'")
    }

    private fun parseDay(dayText: String): Int {
        return dayText.removeSuffix("er").toInt()
    }

    private fun parseMonth(monthText: String): Int {
        val normalizedMonth = monthText.normalizeForLookup()
        return MONTHS[normalizedMonth]
            ?: throw IllegalArgumentException("Unsupported French month: '$monthText'")
    }

    private fun parseYear(yearText: String?, fallbackYear: Int?, originalDate: String): Int {
        return yearText?.toIntOrNull()
            ?: fallbackYear
            ?: throw IllegalArgumentException("Missing year for date: '$originalDate'")
    }

    private fun extractYearForTable(table: Element): Int? {
        val nearestHeadingText = findNearestPreviousHeadingText(table) ?: return null

        return YEAR_HEADING_REGEX.matchEntire(nearestHeadingText)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
    }

    private fun findNearestPreviousHeadingText(element: Element): String? {
        var current: Element? = element
        while (current != null) {
            var sibling = current.previousElementSibling()
            while (sibling != null) {
                findLastHeadingText(sibling)?.let { return it }
                sibling = sibling.previousElementSibling()
            }
            current = current.parent()
        }
        return null
    }

    private fun findLastHeadingText(element: Element): String? {
        return element.select("h2, h3")
            .asReversed()
            .firstOrNull()
            ?.text()
            ?.normalizeCellText()
    }

    private fun String.normalizeForLookup(): String {
        return Normalizer.normalize(lowercase(), Normalizer.Form.NFD)
            .replace(DIACRITICS_REGEX, "")
    }

    private data class MutableStudy(
        val pollster: String,
        val sourceUrl: String?,
        val minDate: LocalDate,
        val maxDate: LocalDate,
        val sampleSize: Int,
        val polls: MutableList<Poll> = mutableListOf()
    ) {
        fun toImmutable(): Study {
            return Study(
                pollster = pollster,
                sourceUrl = sourceUrl,
                minDate = minDate,
                maxDate = maxDate,
                sampleSize = sampleSize,
                polls = polls.toList(),
            )
        }
    }

    companion object {
        private val NUMBER_REGEX = Regex("""\d+(?:[.,]\d+)?""")
        private val YEAR_HEADING_REGEX = Regex("""^Ann[e\u00e9]e\s+(\d{4})$""", RegexOption.IGNORE_CASE)
        private val SINGLE_DAY_REGEX = Regex("""(\d{1,2}(?:er)?)\s+([\p{L}-]+)(?:\s+(\d{4}))?""")
        private val SAME_MONTH_RANGE_REGEX =
            Regex("""(\d{1,2}(?:er)?)\s*-\s*(\d{1,2}(?:er)?)\s+([\p{L}-]+)(?:\s+(\d{4}))?""")
        private val CROSS_MONTH_RANGE_REGEX =
            Regex("""(\d{1,2}(?:er)?)\s+([\p{L}-]+)\s*-\s*(\d{1,2}(?:er)?)\s+([\p{L}-]+)(?:\s+(\d{4}))?""")
        private val DIACRITICS_REGEX = Regex("""\p{M}+""")
        private val SAMPLE_SIZE_REGEX = Regex("""\d""")
        private val MONTHS = mapOf(
            "janvier" to 1,
            "fevrier" to 2,
            "mars" to 3,
            "avril" to 4,
            "mai" to 5,
            "juin" to 6,
            "juillet" to 7,
            "aout" to 8,
            "septembre" to 9,
            "octobre" to 10,
            "novembre" to 11,
            "decembre" to 12,
        )
    }
}
