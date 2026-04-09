package fr.voxpol.scripts

import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.security.MessageDigest
import java.time.LocalDateTime
import kotlin.system.exitProcess

private const val JS_API = "https://www.toptal.com/developers/javascript-minifier/api/raw"
private const val CSS_API = "https://www.toptal.com/developers/cssminifier/api/raw"

private data class MinificationCsvEntry(
    val filePath: String,
    val checksum: String,
    val dateTime: LocalDateTime,
) {
    override fun hashCode() = filePath.hashCode()
    override fun equals(other: Any?) = other is MinificationCsvEntry && filePath == other.filePath

    fun toCsv() = "$filePath,$checksum,$dateTime"
}

// https://www.toptal.com/developers/javascript-minifier/documentation
private fun minifyFile(file: File): Int {
    val input = file.readLines().joinToString("\n")
    val output = minifiedFile(file)
    output.delete()

    val content = buildString {
        append(URLEncoder.encode("input", "UTF-8"))
        append("=")
        append(URLEncoder.encode(input, "UTF-8"))
    }

    val api = when (file.extension) {
        "js" -> JS_API
        "css" -> CSS_API
        else -> throw IllegalArgumentException("Unsupported extension: ${file.extension}")
    }

    val connection = (URI(api).toURL().openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        setRequestProperty("charset", "utf-8")
        setRequestProperty("Content-Length", content.length.toString())
        OutputStreamWriter(outputStream).apply { write(content); flush() }
    }

    if (connection.responseCode == 200) {
        output.writeText(InputStreamReader(connection.inputStream).readText())
    }

    return connection.responseCode
}

private fun checksum(file: File): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(file.readBytes())).toString(16).padStart(32, '0')
}

private fun loadCsv(): MutableList<MinificationCsvEntry> {
    val csv = File(CSV_FILE)
    if (!csv.exists()) return mutableListOf()
    return csv.readLines().map { line ->
        val (filePath, checksum, dateTime) = line.split(",")
        MinificationCsvEntry(filePath, checksum, LocalDateTime.parse(dateTime))
    }.toMutableList()
}

private fun writeCsv(entries: List<MinificationCsvEntry>) {
    val csv = File(CSV_FILE)
    csv.writeText(entries.sortedBy { it.filePath }.joinToString("\n") { it.toCsv() })
    println("Wrote ${entries.size} entries to $CSV_FILE")
}

private fun formatBytes(bytes: Long) = "${bytes / 1024} kB"

fun main() {
    val allFiles = listAllJsFiles() + listAllCssFiles()
    allFiles.forEach { println("[found] ${it.path}") }

    val csvEntries = loadCsv()

    // Remove entries whose source file no longer exists
    csvEntries.removeAll { entry ->
        val missing = !File(entry.filePath).exists()
        if (missing) println("[file not found] ${entry.filePath}")
        missing
    }

    val filesToMinify = allFiles.filter { file ->
        val entry = csvEntries.find { it.filePath == file.path }
        when {
            entry == null -> { println("[entry not found] ${file.path}"); true }
            !minifiedFile(file).exists() -> { println("[not minified] ${file.path}"); true }
            entry.checksum != checksum(file) -> { println("[checksum changed] ${file.path}"); true }
            else -> false
        }
    }

    println("All files     → ${allFiles.size}")
    println("To minify     → ${filesToMinify.size}")

    val chunkSize = 25
    var processed = 0

    filesToMinify.chunked(chunkSize).forEach { chunk ->
        chunk.forEach { file ->
            val code = minifyFile(file)
            println("[${file.path}] → $code")
            if (code != 200) {
                println("ERROR: stopping due to response code $code")
                exitProcess(1)
            }
            csvEntries.removeIf { it.filePath == file.path }
            csvEntries += MinificationCsvEntry(file.path, checksum(file), LocalDateTime.now())
            processed++
        }
        if (chunk.size == chunkSize) {
            writeCsv(csvEntries)
            val toWait = 70_000L
            println("Sleeping ${toWait}ms for rate-limit, remaining: ${filesToMinify.size - processed}")
            Thread.sleep(toWait)
        }
    }

    writeCsv(csvEntries)

    // Summary
    val allCss = listAllCssFiles()
    val allJs = listAllJsFiles()
    val cssOrig = allCss.sumOf { it.length() }
    val cssMin = allCss.sumOf { minifiedFile(it).length() }
    val jsOrig = allJs.sumOf { it.length() }
    val jsMin = allJs.sumOf { minifiedFile(it).length() }

    println()
    println("Unminified JS  → ${formatBytes(jsOrig)}")
    println("Minified JS    → ${formatBytes(jsMin)}")
    println("JS reduction   → ${formatBytes(jsOrig - jsMin)}")
    println()
    println("Unminified CSS → ${formatBytes(cssOrig)}")
    println("Minified CSS   → ${formatBytes(cssMin)}")
    println("CSS reduction  → ${formatBytes(cssOrig - cssMin)}")
    println()
    val totalOrig = cssOrig + jsOrig
    val totalMin = cssMin + jsMin
    println("Total reduction      → ${formatBytes(totalOrig - totalMin)}")
    println("Compression ratio    → ${"%.2f".format(totalOrig.toDouble() / totalMin)}x")
}

