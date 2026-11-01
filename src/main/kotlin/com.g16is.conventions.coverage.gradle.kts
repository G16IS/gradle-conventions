import com.g16is.conventions.CoverageReportTask
import com.g16is.conventions.isMultiModule
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension

plugins {
    id("org.jetbrains.kotlinx.kover")
}

val coverageMinBound =
    providers
        .gradleProperty("coverage.min")
        .map { it.toInt() }
        .orElse(80)

extensions.configure<KoverProjectExtension> {
    reports {
        verify {
            rule { minBound(coverageMinBound.get()) }
        }
    }
}

if (project == rootProject) {
    val multiModule = isMultiModule()

    if (multiModule) {
        subprojects {
            pluginManager.apply("org.jetbrains.kotlinx.kover")

            extensions.configure<KoverProjectExtension> {
                reports {
                    verify {
                        rule { minBound(coverageMinBound.get()) }
                    }
                }
            }

            rootProject.dependencies
                .add("kover", this)
        }
    }

    // Single-module: el root tiene el código y se mide a sí mismo.
    val measured = if (multiModule) subprojects.toList() else listOf(project)

    tasks.register<CoverageReportTask>("coverageReport") {
        group = "verification"
        description = "Resumen de cobertura por módulo"

        dependsOn(measured.map { "${it.path.removeSuffix(":")}:koverXmlReport" })

        moduleReports.set(
            measured.associate { module ->
                module.name to
                    module.layout.buildDirectory
                        .file("reports/kover/report.xml")
                        .get()
                        .asFile.absolutePath
            },
        )
    }

    tasks.register("verifyAllCoverage") {
        group = "verification"
        description = "koverVerify en todos los subproyectos"

        dependsOn(measured.map { "${it.path.removeSuffix(":")}:koverVerify" })
    }
}
