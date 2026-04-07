plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":wiki-scrapper"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.html.builder)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.defaultheaders)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.klogger)
    implementation(libs.logback.classic.lib)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "dev.encelade.pollaggregator.AppKt"
}

tasks.shadowJar {
    archiveClassifier.set("")
    manifest {
        attributes("Main-Class" to application.mainClass.get())
    }
}

tasks.startScripts {
    dependsOn(tasks.shadowJar)
}

tasks.named("startShadowScripts") {
    dependsOn(tasks.jar)
}
