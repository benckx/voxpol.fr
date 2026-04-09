package fr.voxpol.webapp.services

import fr.voxpol.webapp.model.CandidateTrendChartDto
import fr.voxpol.webapp.model.CandidateTrendDto
import fr.voxpol.webapp.model.GlobalIntervalByCandidateStatsDto
import fr.voxpol.webapp.model.GlobalIntervalsChartDto
import fr.voxpol.webapp.model.PollChartDto
import fr.voxpol.webapp.model.PollChartPointDto
import fr.voxpol.webapp.model.PollChartSeriesDto
import fr.voxpol.webapp.model.PollRecord
import fr.voxpol.webapp.model.ThresholdChartDto
import fr.voxpol.webapp.model.ThresholdChartPointDto
import fr.voxpol.webapp.model.TrendDirectionDto
import fr.voxpol.webapp.rendering.normalizePollsterName
import fr.voxpol.wikiscrapper.Candidate
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToLong

private fun Double.round4(): Double = (this * 10_000).roundToLong() / 10_000.0

/**
 * Aggregate poll data into a format suitable for line charts
 * @param candidates the sorted list of candidates to include in the chart
 */
fun buildLineChartData(
    candidates: List<Candidate>,
    polls: List<PollRecord>
): PollChartDto {
    // Keep one poll per day in charts to avoid stacked points on the same date.
    val dailyPolls = polls
        .groupBy { it.dateTo }
        .values
        .map { sameDayPolls ->
            sameDayPolls
                .sortedWith(
                    compareByDescending<PollRecord> { it.sampleSize }
                        .thenByDescending { it.dateFrom }
                        .thenBy { it.pollster }
                        .thenBy { it.sourceUrl ?: "" }
                )
                .first()
        }
    val chronologicalPolls = dailyPolls.sortedBy { it.dateTo }

    return PollChartDto(
        series = candidates.map { candidate ->
            PollChartSeriesDto(
                name = candidate.lastName,
                color = candidate.cssColor ?: "#64748b",
                data = chronologicalPolls.map { poll ->
                    PollChartPointDto(
                        x = poll.dateTo.toString(),
                        y = requireNotNull(poll.scoresByCandidate[candidate]) {
                            "Missing score for ${candidate.name} in poll $poll"
                        },
                        pollster = normalizePollsterName(poll.pollster),
                    )
                },
            )
        },
    )
}

fun buildGlobalIntervalsChartData(
    polls: List<PollRecord>,
): GlobalIntervalsChartDto {
    val excludedFromGlobalSummary = setOf(Candidate.POUTOU, Candidate.BAYROU)

    val stats = Candidate.entries
        .filterNot { it in excludedFromGlobalSummary }
        .mapNotNull { candidate ->
            val values = polls.mapNotNull { poll -> poll.scoresByCandidate[candidate] }
            if (values.isEmpty()) {
                null
            } else {
                GlobalIntervalByCandidateStatsDto(
                    name = candidate.lastName,
                    color = candidate.cssColor ?: "#64748b",
                    min = values.min(),
                    avg = values.average(),
                    max = values.max(),
                )
            }
        }

    return GlobalIntervalsChartDto(
        stats = stats.sortedByDescending { it.max },
    )
}

fun buildCandidateTrendChartData(
    polls: List<PollRecord>,
    windowDays: Int,
    now: LocalDate = LocalDate.now(),
): CandidateTrendChartDto {
    require(windowDays > 0) { "windowDays must be > 0" }

    val latestStart = now.minusDays(windowDays.toLong() - 1)
    val previousEnd = latestStart.minusDays(1)
    val previousStart = previousEnd.minusDays(windowDays.toLong() - 1)

    val latestWindowPolls = polls.filter { poll ->
        !poll.dateTo.isBefore(latestStart) && !poll.dateTo.isAfter(now)
    }
    val previousWindowPolls = polls.filter { poll ->
        !poll.dateTo.isBefore(previousStart) && !poll.dateTo.isAfter(previousEnd)
    }

    val stats = Candidate.entries
        .mapNotNull { candidate ->
            val latestValues = latestWindowPolls.mapNotNull { it.scoresByCandidate[candidate] }
            val previousValues = previousWindowPolls.mapNotNull { it.scoresByCandidate[candidate] }

            if (latestValues.size < 2 || previousValues.size < 2) {
                return@mapNotNull null
            }

            val latestAvg = latestValues.average().round4()
            val previousAvg = previousValues.average().round4()
            val delta = (latestAvg - previousAvg).round4()

            CandidateTrendDto(
                name = candidate.lastName,
                color = candidate.cssColor ?: "#64748b",
                latestAvg = latestAvg,
                previousAvg = previousAvg,
                delta = delta,
                direction = when {
                    abs(delta) < .005 -> TrendDirectionDto.FLAT // half a percent
                    delta > 0 -> TrendDirectionDto.UP
                    else -> TrendDirectionDto.DOWN
                },
                latestPollCount = latestValues.size,
                previousPollCount = previousValues.size,
            )
        }
        .sortedByDescending { it.latestAvg }

    return CandidateTrendChartDto(
        windowDays = windowDays,
        stats = stats,
    )
}

/**
 * For each first-round poll, computes the 2nd-highest candidate score (= the score needed to
 * qualify for the second round). Results are grouped and averaged by calendar month
 * (using the study's max day / [PollRecord.dateTo]), returning avg, min, and max per month.
 */
fun buildQualificationThresholdChartData(firstRoundPolls: List<PollRecord>): ThresholdChartDto {
    val data = firstRoundPolls
        .groupBy { it.dateTo.withDayOfMonth(1) }
        .entries
        .sortedBy { (date, _) -> date }
        .mapNotNull { (date, polls) ->
            val secondHighestValues = polls.mapNotNull { poll ->
                poll.scoresByCandidate.values
                    .sortedDescending()
                    .getOrNull(1)
            }
            if (secondHighestValues.isEmpty()) null
            else ThresholdChartPointDto(
                x = date.toString(),
                avg = secondHighestValues.average(),
                min = secondHighestValues.min(),
                max = secondHighestValues.max(),
            )
        }
    return ThresholdChartDto(data = data)
}

