import dev.detekt.gradle.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    `kotlin-dsl`
    `maven-publish`
    // El quality publicado (1.0.2) no aplica ktlint/detekt en el root, y este
    // repo no puede aplicarse su propio plugin sin publicarlo. Sin estas tasks
    // el pre-commit (`ktlintFormat`, `ktlintCheck`, `detekt`) falla.
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("dev.detekt") version "2.0.0-alpha.6"
    id("com.g16is.conventions.quality") version "1.0.2"
}

gradle.beforeProject {
    if (this != rootProject) {
        pluginManager.apply("com.g16is.conventions.quality")
        pluginManager.apply("com.g16is.conventions.coverage")
    }
}

group = "com.g16is.conventions"
version = (findProperty("version") as String?).takeUnless { it.isNullOrBlank() || it == "unspecified" }
    ?: "0.0.0-SNAPSHOT"

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    implementation("org.jlleitschuh.gradle.ktlint:org.jlleitschuh.gradle.ktlint.gradle.plugin:14.2.0")
    implementation("dev.detekt:dev.detekt.gradle.plugin:2.0.0-alpha.6")
    implementation("org.jetbrains.kotlinx:kover-gradle-plugin:0.9.9")
}

kotlin {
    jvmToolchain(21)
}

extensions.configure<KtlintExtension> {
    android.set(false)
    outputToConsole.set(true)
    verbose.set(true)
    coloredOutput.set(true)
    relative.set(true)
    // kotlin-dsl mete los accessors generados en el source set main, bajo build/.
    // ktlint no puede autocorregirlos y ktlintCheck falla siempre.
    filter {
        exclude { element ->
            element.file.invariantSeparatorsPath.contains("/build/")
        }
    }
}

extensions.configure<DetektExtension> {
    buildUponDefaultConfig.set(true)
    allRules.set(false)
    parallel.set(true)
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    jvmTarget.set("21")
    reports {
        html.required.set(false)
        checkstyle.required.set(false)
        sarif.required.set(false)
        markdown.required.set(false)
    }
}

tasks.processResources {
    from("hooks") {
        into("hooks")
    }
}
