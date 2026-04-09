plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.logback.classic.lib)
    implementation(libs.klogger)
    implementation(libs.jsoup)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.test {
    failOnNoDiscoveredTests = false
}
