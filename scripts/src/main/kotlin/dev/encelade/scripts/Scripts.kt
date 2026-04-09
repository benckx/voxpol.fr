package dev.encelade.scripts

import java.io.File

/** All non-minified JS source files under the app's static resources. */
fun listAllJsFiles(): List<File> =
    File("app/src/main/resources/static")
        .walkTopDown()
        .filter { it.isFile && it.extension == "js" && !it.name.contains(".min.") }
        .toList()

/** All non-minified CSS source files under the app's static resources. */
fun listAllCssFiles(): List<File> =
    File("app/src/main/resources/static")
        .walkTopDown()
        .filter { it.isFile && it.extension == "css" && !it.name.contains(".min.") }
        .toList()

/** Returns the sibling `.min.*` file for a given source file. */
fun minifiedFile(file: File): File =
    File(file.parentFile, "${file.nameWithoutExtension}.min.${file.extension}")

/** Path to the CSV that tracks per-file checksums to avoid redundant API calls. */
const val CSV_FILE = "minified_files.csv"

