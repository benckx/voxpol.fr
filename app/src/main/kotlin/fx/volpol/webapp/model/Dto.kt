package fx.volpol.webapp.model

import kotlinx.serialization.Serializable

@Serializable
data class PollChartDto(
    val series: List<PollChartSeriesDto>,
)

@Serializable
data class PollChartSeriesDto(
    val name: String,
    val color: String,
    val data: List<PollChartPointDto>,
)

@Serializable
data class PollChartPointDto(
    val x: String,
    val y: Double,
    val pollster: String,
)

@Serializable
data class GlobalIntervalByCandidateStatsDto(
    val name: String,
    val color: String,
    val min: Double,
    val avg: Double,
    val max: Double,
)

@Serializable
data class GlobalIntervalsChartDto(
    val stats: List<GlobalIntervalByCandidateStatsDto>,
)

@Serializable
enum class TrendDirectionDto {
    UP,
    FLAT,
    DOWN,
}

@Serializable
data class CandidateTrendDto(
    val name: String,
    val color: String,
    val latestAvg: Double,
    val previousAvg: Double,
    val delta: Double,
    val direction: TrendDirectionDto,
    val latestPollCount: Int,
    val previousPollCount: Int,
)

@Serializable
data class CandidateTrendChartDto(
    val windowDays: Int,
    val stats: List<CandidateTrendDto>,
)

@Serializable
data class ThresholdChartPointDto(
    val x: String,
    val avg: Double,
    val min: Double,
    val max: Double,
)

@Serializable
data class ThresholdChartDto(
    val data: List<ThresholdChartPointDto>,
)

