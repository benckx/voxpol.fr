package dev.encelade.pollaggregator.services

import dev.encelade.pollaggregator.model.PollRecord
import dev.encelade.wikiscrapper.Candidate
import dev.encelade.wikiscrapper.TestingHypothesis
import dev.encelade.wikiscrapper.WikiScrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PollService {

    private val logger = KotlinLogging.logger {}

    @Volatile
    private var polls: List<PollRecord> = emptyList()

    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "poll-refresh").also { it.isDaemon = true }
    }

    init {
        scheduler.scheduleAtFixedRate(::refreshData, 0, 4, TimeUnit.HOURS)
    }

    private fun refreshData() {
        try {
            logger.info { "Fetching poll data from Wikipedia..." }
            val scrapper = WikiScrapper()
            val records = scrapper.fetchAllStudies()
                .flatMap { study ->
                    study.polls.map { poll ->
                        PollRecord(
                            pollster = study.pollster,
                            sourceUrl = study.sourceUrl,
                            dateFrom = study.minDate,
                            dateTo = study.maxDate,
                            sampleSize = study.sampleSize,
                            scoresByCandidate = poll.results,
                        )
                    }
                }
            if (records.isNotEmpty()) {
                polls = records
                logger.info { "Loaded ${records.size} poll records from Wikipedia." }
                return
            }
            logger.warn { "Wikipedia scraping returned no records${if (polls.isEmpty()) ", falling back to CSV" else ", keeping current data"}." }
        } catch (e: Exception) {
            logger.error(e) { "Failed to scrape Wikipedia${if (polls.isEmpty()) ", falling back to CSV" else ", keeping current data"}." }
        }
        if (polls.isEmpty()) loadFromCsv()
    }

    private fun loadFromCsv() {
        try {
            val resourceName = "poll-data.csv"
            val classLoader = PollService::class.java.classLoader
            val csvData = CsvLoader.load(resourceName, classLoader)
            polls = csvData.polls
            logger.info { "Loaded ${polls.size} poll records from CSV fallback." }
        } catch (e: Exception) {
            logger.error(e) { "Failed to load CSV fallback." }
        }
    }

    fun pollsBefore(cutoffDate: LocalDate) =
        polls.filter { !it.dateTo.isBefore(cutoffDate) }

    fun allPolls(): List<PollRecord> = polls

    fun combinationsByRecency(): List<TestingHypothesis> {
        // most recent poll, then has the most polls
        val comparator =
            compareByDescending<Map.Entry<Set<Candidate>, List<PollRecord>>> { (_, polls) -> polls.maxOf { it.dateTo } }
                .thenByDescending { (_, polls) -> polls.size }

        return polls
            .groupBy { it.scoresByCandidate.keys }
            .entries
            .sortedWith(comparator)
            .map { (candidates, _) -> TestingHypothesis(candidates) }
    }

    fun pollsForTestingHypothesis(hypothesis: TestingHypothesis): List<PollRecord> {
        return polls
            .filter { it.scoresByCandidate.keys == hypothesis.candidates }
            .sortedByDescending { it.dateTo }
    }

}
