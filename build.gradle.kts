plugins {
    base
    alias(libs.plugins.versions)
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ktlint)
}

repositories {
    mavenCentral()
}

val ktlintVersion = libs.versions.ktlint

ktlint {
    version.set(ktlintVersion)
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set(ktlintVersion)
    }
}
