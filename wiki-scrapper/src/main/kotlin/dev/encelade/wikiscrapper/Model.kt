package dev.encelade.wikiscrapper

import java.time.LocalDate
import kotlin.math.abs

data class TestingHypothesis(
    val candidates: Set<Candidate>
) {

    val candidatesInOrder: List<Candidate>
        get() = Candidate.entries.filter { it in candidates }

    override fun toString(): String {
        val names = candidates.toList().map { it.lastName }.sorted()
        return "Hypothesis {${names.joinToString(", ")}}}"
    }
}

enum class Candidate(val wikiSlug: String, val color: String? = null) {

    // ordered from the left to the right political spectrum
    ARTAUD("Nathalie_Arthaud", "bb0000"),
    POUTOU("Philippe_Poutou", "bb0000"),
    MELENCHON("Jean-Luc_Mélenchon", "cc2443"),
    ROUSSEL("Fabien_Roussel", "dd0000"),
    RUFFIN("François_Ruffin", "dd0000"),
    ROUSSEAU("Sandrine_Rousseau", "00c000"),
    TONDELIER("Marine_Tondelier", "00c000"),
    JADOT("Yannick_Jadot", "00c000"),
    GLUCKSMANN("Raphaël_Glucksmann", "FF8080"),
    HOLLANDE("François_Hollande", "FF8080"),
    FAURE("Olivier_Faure_(homme_politique)", "FF8080"),
    ATTAL("Gabriel_Attal", "FED700"),
    BAYROU("François_Bayrou", "FED700"),
    PHILIPPE("Édouard_Philippe", "FED700"),
    DARMANIN("Gérald_Darmanin", "FED700"),
    LECORNU("Sébastien_Lecornu", "FED700"),
    RETAILLEAU("Bruno_Retailleau", "0066CC"),
    VILLEPIN("Dominique_de_Villepin", "adc1fd"),
    DUPONT_AIGNAN("Nicolas_Dupont-Aignan", "0082C4"),
    BARDELLA("Jordan_Bardella", "0D378A"),
    LE_PEN("Marine_Le_Pen", "0D378A"),
    KNAFO("Sarah_Knafo", "404040"),
    ZEMMOUR("Éric_Zemmour", "404040");

    val lastName: String
        get() = when (this) {
            LE_PEN -> "Le Pen"
            DUPONT_AIGNAN -> "Dupont"
            else -> toString().split(' ').last()
        }

    val cssColor: String?
        get() = color?.let { "#$it" }

    override fun toString(): String {
        return wikiSlug.removeSuffix("_(homme_politique)").replace('_', ' ')
    }
}

data class Poll(
    val results: Map<Candidate, Double>,
) {
    fun summed(): Double = results.values.sum()

    fun isSummedCloseToOne(tolerance: Double = .10): Boolean {
        return abs(summed() - 1.0) <= tolerance
    }
}

data class Study(
    val pollster: String,
    val sourceUrl: String?,
    val minDate: LocalDate,
    val maxDate: LocalDate,
    val sampleSize: Int,
    val polls: List<Poll>
) {
    override fun toString(): String {
        val dateRange = if (minDate == maxDate) {
            minDate.toString()
        } else {
            "$minDate to $maxDate"
        }
        return "Study{$pollster, $dateRange, $sampleSize, polls=${polls.size}}"
    }
}
