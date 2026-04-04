package dev.encelade.pollaggregator.model

import dev.encelade.wikiscrapper.Candidate
import java.time.LocalDate

data class PollRecord(
    val pollster: String,
    val sourceUrl: String?,
    val dateFrom: LocalDate,
    val dateTo: LocalDate,
    val sampleSize: Int,
    val scoresByCandidate: Map<Candidate, Double>,
)
