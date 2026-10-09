
pluginManagement {
    repositories {
        maven {
            name = "GitHubPackagesConventions"
            url = uri("https://maven.pkg.github.com/G16IS/gradle-conventions")
            credentials {
                username =
                    providers
                        .gradleProperty("gpr.user")
                        .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                        .get()
                password =
                    providers
                        .gradleProperty("gpr.key")
                        .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                        .get()
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "gradle-conventions"
