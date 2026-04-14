/**
 * Objects data stores in memory.
 */
package fr.voxpol.webapp.model

import fr.voxpol.wikiscrapper.Candidate
import java.time.LocalDate

data class PollRecord(
    val pollster: String,
    val sourceUrl: String?,
    val dateFrom: LocalDate,
    val dateTo: LocalDate,
    val sampleSize: Int,
    val scoresByCandidate: Map<Candidate, Double>,
)
