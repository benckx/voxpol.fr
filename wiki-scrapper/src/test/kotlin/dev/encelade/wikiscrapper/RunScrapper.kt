package dev.encelade.wikiscrapper

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

fun main() {
    val logger = KotlinLogging.logger {}
    val scrapper = WikiScrapper()
    val candidatesInOrder = Candidate.entries

    val tables = scrapper.fetchFirstRoundPollTables()
    val studies = tables.flatMap { scrapper.parseStudies(it) }
    val polls = studies.flatMap { it.polls }

    logger.info { "Found ${tables.size} tables" }
    logger.info { "Studies parsed: ${studies.size}" }
    logger.info { "Polls: ${polls.size}" }

    val csvFile = File("app/src/main/resources/poll-data.csv")
    exportPollsToCsv(studies, candidatesInOrder, csvFile)
    println("CSV exported to: ${csvFile.absolutePath}")

    println()

    studies.take(10).forEach { study ->
        println("    $study")
        println("    ${study.sourceUrl ?: "(no source URL)"}")

        study.polls.forEachIndexed { pollIndex, poll ->
            val orderedValues = candidatesInOrder
                .mapNotNull { candidate ->
                    poll.results[candidate]?.let { value -> "${candidate.name}=${value.toPercentString()}" }
                }
                .joinToString(" | ")

            if (orderedValues.isBlank()) {
                println("      poll #${pollIndex + 1}: (no values)")
            } else {
                println("      poll #${pollIndex + 1}: $orderedValues")
            }
        }
    }

    println()
    println()

    val combinations = studies.flatMap { it.polls }.groupBy { it.results.keys }
    println("\nunique candidate combinations across all polls: ${combinations.size}")
    combinations
        .toList()
        .sortedByDescending { it.second.size }
        .forEach { (candidates, polls) ->
            val candidateNames = candidates.joinToString(", ") { it.name }
            println("  $candidateNames: ${polls.size} poll(s)")
        }

    println("\nlowest/highest by candidate:")
    candidatesInOrder.forEach { candidate ->
        val values = polls.mapNotNull { poll -> poll.results[candidate] }
        if (values.isEmpty()) {
            println("  ${candidate.name}: (no data)")
        } else {
            val low = values.minOrNull()
            val high = values.maxOrNull()
            println("  ${candidate.name}: [${low?.toPercentString()} - ${high?.toPercentString()}]")
        }
    }
}


private fun Double.toPercentString(): String {
    val percent = this * 100.0
    return if (percent % 1.0 == 0.0) {
        "${percent.toInt()}%"
    } else {
        "${"%.1f".format(percent)}%"
    }
}
