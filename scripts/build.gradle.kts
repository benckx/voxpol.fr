plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.logback.classic.lib)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.register<JavaExec>("minify") {
    group = "deployment"
    description = "Minify JS and CSS assets using the Toptal API"
    mainClass = "dev.encelade.scripts.MinifyKt"
    classpath = sourceSets.main.get().runtimeClasspath
    workingDir = rootProject.projectDir
}

tasks.register<JavaExec>("removeMinified") {
    group = "deployment"
    description = "Remove all minified JS and CSS files"
    mainClass = "dev.encelade.scripts.RemoveMinifiedKt"
    classpath = sourceSets.main.get().runtimeClasspath
    workingDir = rootProject.projectDir
}

