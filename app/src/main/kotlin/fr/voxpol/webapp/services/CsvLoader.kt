package fr.voxpol.webapp.services

import fr.voxpol.webapp.model.PollRecord
import fr.voxpol.wikiscrapper.Candidate
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate

data class CsvData(
    val candidateColumns: List<Candidate>,
    val polls: List<PollRecord>
)

object CsvLoader {

    private val logger = KotlinLogging.logger {}

    private val candidatesByName = Candidate.entries.associateBy { it.name }
    private val requiredColumns = setOf("pollster", "date_from", "date_to", "sample_size")
    private val metadataColumns = requiredColumns + "source_url"

    fun safeLoad(resourceName: String): List<PollRecord>? {
        return try {
            load(resourceName, CsvLoader::class.java.classLoader).polls
        } catch (e: Exception) {
            logger.error(e) { "Failed to load poll data from CSV resource '$resourceName'" }
            null
        }
    }

    private fun load(resourceName: String, classLoader: ClassLoader): CsvData {
        val csv = classLoader.getResourceAsStream(resourceName)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: error("Resource not found: $resourceName")

        val lines = csv
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        require(lines.isNotEmpty()) { "CSV is empty: $resourceName" }

        val header = lines.first().split(',').map { it.trim() }
        require(header.size >= 5) { "CSV header must have at least 5 columns" }
        require(requiredColumns.all { it in header }) {
            "Unexpected CSV header in $resourceName"
        }

        val columnIndexByName = header.withIndex().associate { (index, name) -> name to index }

        val candidateColumns = header.filterNot { it in metadataColumns }.map { name ->
            candidatesByName[name] ?: error("Unknown candidate column: '$name'")
        }

        val polls = lines.drop(1).mapIndexed { index, line ->
            parseDataLine(
                line = line,
                lineNumber = index + 2,
                expectedColumnCount = header.size,
                columnIndexByName = columnIndexByName,
                candidateColumns = candidateColumns,
            )
        }

        return CsvData(candidateColumns, polls)
    }

    private fun parseDataLine(
        line: String,
        lineNumber: Int,
        expectedColumnCount: Int,
        columnIndexByName: Map<String, Int>,
        candidateColumns: List<Candidate>,
    ): PollRecord {
        val fields = line.split(',').map { it.trim() }
        require(fields.size == expectedColumnCount) {
            "Invalid column count at line $lineNumber: expected $expectedColumnCount, got ${fields.size}"
        }

        val pollster = fields[columnIndexByName.getValue("pollster")]
        val sourceUrl = columnIndexByName["source_url"]
            ?.let { index -> fields[index].ifBlank { null } }
        val dateFrom = LocalDate.parse(fields[columnIndexByName.getValue("date_from")])
        val dateTo = LocalDate.parse(fields[columnIndexByName.getValue("date_to")])
        val sampleSize = fields[columnIndexByName.getValue("sample_size")].toInt()

        val scores = candidateColumns.mapNotNull { candidate ->
            val candidateColumnName = candidate.name
            val candidateColumnIndex = columnIndexByName[candidateColumnName]
                ?: error("Missing candidate column '$candidateColumnName' at line $lineNumber")
            val raw = fields[candidateColumnIndex]
            if (raw.isEmpty()) {
                null
            } else {
                val score = raw.toDoubleOrNull()
                    ?: error("Invalid score '$raw' at line $lineNumber")
                candidate to score / 100.0
            }
        }.toMap()

        return PollRecord(
            pollster = pollster,
            sourceUrl = sourceUrl,
            dateFrom = dateFrom,
            dateTo = dateTo,
            sampleSize = sampleSize,
            scoresByCandidate = scores,
        )
    }
}
