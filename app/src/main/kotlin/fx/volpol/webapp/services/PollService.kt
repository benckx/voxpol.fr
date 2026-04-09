package fx.volpol.webapp.services

import fx.volpol.webapp.model.PollRecord
import fr.voxpol.wikiscrapper.Candidate
import fr.voxpol.wikiscrapper.Study
import fr.voxpol.wikiscrapper.TestingHypothesis
import fr.voxpol.wikiscrapper.WikiScrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PollService {

    private val logger = KotlinLogging.logger {}

    @Volatile
    private var inMemFirstRoundPollRecords: List<PollRecord> = emptyList()

    @Volatile
    private var inMemSecondRoundPollRecords: List<PollRecord> = emptyList()

    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "poll-refresh").also { it.isDaemon = true }
    }

    init {
        scheduler.scheduleAtFixedRate(::refreshData, 0, 12, TimeUnit.HOURS)
    }

    private fun refreshData() {
        refreshRoundData(
            roundLabel = "first-round",
            fetchStudies = WikiScrapper::fetchFirstRoundStudies,
            getCurrentRecords = { inMemFirstRoundPollRecords },
            setRecords = { records -> inMemFirstRoundPollRecords = records },
            csvResourceName = "poll-data-first-round.csv",
        )

        refreshRoundData(
            roundLabel = "second-round",
            fetchStudies = WikiScrapper::fetchAllSecondRoundStudies,
            getCurrentRecords = { inMemSecondRoundPollRecords },
            setRecords = { records -> inMemSecondRoundPollRecords = records },
            csvResourceName = "poll-data-second-round.csv",
        )
    }

    /**
     * Shared refresh logic for both rounds.
     *
     * @param roundLabel      human-readable label used in log messages ("first-round" / "second-round")
     * @param fetchStudies    how to retrieve studies from the [WikiScrapper]
     * @param getCurrentRecords reader for the current in-memory records (used to decide log wording)
     * @param setRecords      writer that stores freshly-fetched records in memory
     * @param csvResourceName CSV resource used as a sanity-check threshold and fallback
     */
    private fun refreshRoundData(
        roundLabel: String,
        fetchStudies: (WikiScrapper) -> List<Study>,
        getCurrentRecords: () -> List<PollRecord>,
        setRecords: (List<PollRecord>) -> Unit,
        csvResourceName: String,
    ) {
        fun loadFromCsv() {
            val pollsFromCsv = CsvLoader.safeLoad(csvResourceName)
            if (pollsFromCsv != null) {
                setRecords(pollsFromCsv)
                logger.info { "Loaded ${pollsFromCsv.size} $roundLabel poll records from CSV fallback." }
            } else {
                logger.error { "Failed to load CSV fallback for $roundLabel." }
            }
        }

        try {
            logger.info { "Fetching $roundLabel poll data from Wikipedia..." }
            val pollRecords = fetchStudies(WikiScrapper())
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
                val csvCount = CsvLoader.safeLoad(csvResourceName)?.size ?: 0
                if (csvCount > 0 && pollRecords.size * 2 <= csvCount) {
                    logger.warn { "Wikipedia returned only ${pollRecords.size} records, which is half or less of CSV count ($csvCount). Falling back to CSV." }
                    loadFromCsv()
                    return
                }
                setRecords(pollRecords)
                logger.info {
                    val latestDate = pollRecords.maxOf { it.dateTo }
                    val daysAgo = LocalDate.now().toEpochDay() - latestDate.toEpochDay()
                    "Loaded ${pollRecords.size} $roundLabel poll records from Wikipedia. Latest data point: $latestDate ($daysAgo day(s) ago)"
                }
                return
            }
            logger.warn { "Wikipedia scraping returned no $roundLabel records${if (getCurrentRecords().isEmpty()) ", falling back to CSV" else ", keeping current data"}." }
        } catch (e: Exception) {
            logger.error(e) { "Failed to scrape $roundLabel data from Wikipedia${if (getCurrentRecords().isEmpty()) ", falling back to CSV" else ", keeping current data"}." }
        }

        if (getCurrentRecords().isEmpty()) loadFromCsv()
    }

    fun getFirstRoundPollsBefore(cutoffDate: LocalDate) =
        inMemFirstRoundPollRecords.filter { !it.dateTo.isBefore(cutoffDate) }

    fun getFirstRoundPolls(): List<PollRecord> = inMemFirstRoundPollRecords

    private fun allPollRecords() = inMemFirstRoundPollRecords + inMemSecondRoundPollRecords

    fun combinationsByRecency(): List<TestingHypothesis> {
        // most recent poll, then has the most polls
        val comparator =
            compareByDescending<Map.Entry<Set<Candidate>, List<PollRecord>>> { (_, polls) -> polls.maxOf { it.dateTo } }
                .thenByDescending { (_, polls) -> polls.size }

        return allPollRecords()
            .groupBy { it.scoresByCandidate.keys }
            .entries
            .sortedWith(comparator)
            .map { (candidates, _) -> TestingHypothesis(candidates) }
    }

    fun pollsForTestingHypothesis(hypothesis: TestingHypothesis): List<PollRecord> {
        return allPollRecords()
            .filter { it.scoresByCandidate.keys == hypothesis.candidates }
            .sortedByDescending { it.dateTo }
    }

}
