plugins {
    `java-library`
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            val repositoryPath =
                providers.gradleProperty("gpr.repository").orElse(
                    providers.gradleProperty("gpr.owner").zip(providers.gradleProperty("gpr.repo")) { owner, repo ->
                        "$owner/$repo"
                    },
                )
            url = uri("https://maven.pkg.github.com/${repositoryPath.get()}")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
