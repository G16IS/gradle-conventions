import com.g16is.conventions.InstallGitHooks
import dev.detekt.gradle.extensions.DetektExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

if (project != rootProject) {
    pluginManager.apply("org.jetbrains.kotlin.jvm")
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    pluginManager.apply("dev.detekt")

    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain(21)
    }

    extensions.configure<KtlintExtension> {
        android.set(false)
        outputToConsole.set(true)
        verbose.set(true)
        coloredOutput.set(true)
        relative.set(true)
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
    if (project == rootProject) {
        tasks.register<InstallGitHooks>("installGitHooks") {
            group = "build setup"
            description = "Installs the repository git hooks if they are not already installed."
            repoDirectory.convention(layout.projectDirectory)
            hookNames.convention(listOf("pre-commit", "post-commit"))
        }
    }
}
