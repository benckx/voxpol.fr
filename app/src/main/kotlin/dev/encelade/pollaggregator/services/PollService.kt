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
    private var inMemPollRecords: List<PollRecord> = emptyList()

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
            val pollRecords = scrapper.fetchAllStudies()
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

            if (pollRecords.isNotEmpty()) {
                val csvCount = csvPollCount()
                if (csvCount > 0 && pollRecords.size * 2 <= csvCount) {
                    logger.warn { "Wikipedia returned only ${pollRecords.size} records, which is half or less of CSV count ($csvCount). Falling back to CSV." }
                    loadFromCsv()
                    return
                }
                inMemPollRecords = pollRecords
                logger.info {
                    val latestDate = pollRecords.maxOf { it.dateTo }
                    val daysAgo = LocalDate.now().toEpochDay() - latestDate.toEpochDay()
                    "Loaded ${pollRecords.size} poll records from Wikipedia. Latest data point: $latestDate ($daysAgo day(s) ago)"
                }
                return
            }
            logger.warn { "Wikipedia scraping returned no records${if (inMemPollRecords.isEmpty()) ", falling back to CSV" else ", keeping current data"}." }
        } catch (e: Exception) {
            logger.error(e) { "Failed to scrape Wikipedia${if (inMemPollRecords.isEmpty()) ", falling back to CSV" else ", keeping current data"}." }
        }

        if (inMemPollRecords.isEmpty()) loadFromCsv()
    }

    private fun csvPollCount(): Int {
        return try {
            val classLoader = PollService::class.java.classLoader
            CsvLoader.load("poll-data.csv", classLoader).polls.size
        } catch (e: Exception) {
            logger.warn(e) { "Failed to load CSV for count comparison." }
            0
        }
    }

    private fun loadFromCsv() {
        try {
            val resourceName = "poll-data.csv"
            val classLoader = PollService::class.java.classLoader
            val csvData = CsvLoader.load(resourceName, classLoader)
            inMemPollRecords = csvData.polls
            logger.info { "Loaded ${inMemPollRecords.size} poll records from CSV fallback." }
        } catch (e: Exception) {
            logger.error(e) { "Failed to load CSV fallback." }
        }
    }

    fun pollsBefore(cutoffDate: LocalDate) =
        inMemPollRecords.filter { !it.dateTo.isBefore(cutoffDate) }

    fun allPolls(): List<PollRecord> = inMemPollRecords

    fun combinationsByRecency(): List<TestingHypothesis> {
        // most recent poll, then has the most polls
        val comparator =
            compareByDescending<Map.Entry<Set<Candidate>, List<PollRecord>>> { (_, polls) -> polls.maxOf { it.dateTo } }
                .thenByDescending { (_, polls) -> polls.size }

        return inMemPollRecords
            .groupBy { it.scoresByCandidate.keys }
            .entries
            .sortedWith(comparator)
            .map { (candidates, _) -> TestingHypothesis(candidates) }
    }

    fun pollsForTestingHypothesis(hypothesis: TestingHypothesis): List<PollRecord> {
        return inMemPollRecords
            .filter { it.scoresByCandidate.keys == hypothesis.candidates }
            .sortedByDescending { it.dateTo }
    }

}
