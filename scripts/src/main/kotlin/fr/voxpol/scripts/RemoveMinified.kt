package fr.voxpol.scripts

import java.io.File

fun main() {
    val minifiedFiles = (listAllJsFiles() + listAllCssFiles())
        .map { minifiedFile(it) }
        .filter { it.exists() }

    minifiedFiles.forEach { file ->
        file.delete()
        println("[deleted] ${file.path}")
    }

    val csv = File(CSV_FILE)
    if (csv.exists()) {
        csv.delete()
        println("[deleted] $CSV_FILE")
    }

    println("Removed ${minifiedFiles.size} minified file(s)")
}

