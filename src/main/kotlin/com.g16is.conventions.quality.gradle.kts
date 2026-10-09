import com.g16is.conventions.InstallGitHooks
import dev.detekt.gradle.extensions.DetektExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

val hasCode = project != rootProject || subprojects.isEmpty()

if (hasCode) {
    val kotlinAlreadyApplied =
        pluginManager.hasPlugin("org.jetbrains.kotlin.jvm") ||
            pluginManager.hasPlugin("org.gradle.kotlin.kotlin-dsl")

    if (!kotlinAlreadyApplied) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(21)
        }
    }

    pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    pluginManager.apply("dev.detekt")

    repositories {
        mavenCentral()
    }

    extensions.configure<KtlintExtension> {
        android.set(false)
        outputToConsole.set(true)
        verbose.set(true)
        coloredOutput.set(true)
        relative.set(true)
        // Los generados (kotlin-dsl, ksp, kapt) entran al source set bajo build/
        // y ktlint no puede autocorregirlos.
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
        val detektYml = file("${project.rootDir}/config/detekt/detekt.yml")
        if (detektYml.exists()) {
            config.setFrom(files(detektYml))
        }
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
}

if (project == rootProject) {
    tasks.register<InstallGitHooks>("installGitHooks") {
        group = "build setup"
        description = "Installs the repository git hooks if they are not already installed."
        repoDirectory.convention(layout.projectDirectory)
        hookNames.convention(listOf("pre-commit", "post-commit"))
    }

    subprojects {
        pluginManager.apply("com.g16is.conventions.quality")
    }
}
