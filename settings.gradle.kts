plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "poll-aggregator"
include("app")
include("wiki-scrapper")
