package dev.encelade.wikiscrapper

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

fun main() {
    val logger = KotlinLogging.logger {}
    val scrapper = WikiScrapper()
    val candidatesInOrder = Candidate.entries

    // --- First round ---
    val tables = scrapper.fetchFirstRoundPollTables()
    val studies = tables.flatMap { scrapper.parseStudies(it) }
    val polls = studies.flatMap { it.polls }

    logger.info { "Found ${tables.size} tables" }
    logger.info { "Studies parsed: ${studies.size}" }
    logger.info { "Polls: ${polls.size}" }

    val csvFile = File("app/src/main/resources/poll-data-first-round.csv")
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

    // --- Second round ---
    println()
    println()

    val secondRoundStudies = scrapper.fetchSecondRoundPollTables().flatMap { scrapper.parseStudies(it) }
    val secondRoundPolls = secondRoundStudies.flatMap { it.polls }

    logger.info { "Second-round studies parsed: ${secondRoundStudies.size}" }
    logger.info { "Second-round polls: ${secondRoundPolls.size}" }

    val csvFileSecondRound = File("app/src/main/resources/poll-data-second-round.csv")
    exportPollsToCsv(secondRoundStudies, candidatesInOrder, csvFileSecondRound)
    println("CSV exported to: ${csvFileSecondRound.absolutePath}")

    println()

    secondRoundStudies.take(10).forEach { study ->
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

    val secondRoundCombinations = secondRoundPolls.groupBy { it.results.keys }
    println("\nunique second-round hypotheses: ${secondRoundCombinations.size}")
    secondRoundCombinations
        .toList()
        .sortedByDescending { it.second.size }
        .forEach { (candidates, polls) ->
            val candidateNames = candidates.joinToString(" vs ") { it.lastName }
            println("  $candidateNames: ${polls.size} poll(s)")
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
