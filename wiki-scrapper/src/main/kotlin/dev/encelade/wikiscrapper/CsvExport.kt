package dev.encelade.wikiscrapper

import java.io.File
import java.util.Locale

fun exportPollsToCsv(
    studies: List<Study>,
    candidates: List<Candidate>,
    outputFile: File
) {
    outputFile.parentFile?.mkdirs()

    val header = listOf("pollster", "source_url", "date_from", "date_to", "sample_size") + candidates.map { it.name }
    val rows = studies.flatMap { study ->
        study.polls.map { poll ->
            listOf(
                study.pollster,
                study.sourceUrl.orEmpty(),
                study.minDate.toString(),
                study.maxDate.toString(),
                study.sampleSize.toString(),
            ) + candidates.map { candidate ->
                poll.results[candidate]?.toPercentCsvValue().orEmpty()
            }
        }
    }

    val csvContent = buildString {
        appendLine(header.toCsvLine())
        rows.forEach { appendLine(it.toCsvLine()) }
    }

    outputFile.writeText(csvContent)
}

private fun List<String>.toCsvLine(): String = joinToString(",") { it.escapeCsv() }

private fun String.escapeCsv(): String {
    val escaped = replace("\"", "\"\"")
    return if (contains(',') || contains('"') || contains('\n') || contains('\r')) {
        "\"$escaped\""
    } else {
        escaped
    }
}

private fun Double.toPercentCsvValue(): String {
    val percent = this * 100.0
    return if (percent % 1.0 == 0.0) {
        percent.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", percent)
    }
}
