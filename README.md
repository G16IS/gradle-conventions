# gradle-conventions

Convention plugins reutilizables para repos de G16IS. Este directorio es el **proyecto que se publica**. Los consumidores resuelven los plugins desde GitHub Packages; no usen `includeBuild` de esta carpeta.

## Plugins

| Plugin id | Qué hace |
|---|---|
| `com.g16is.conventions.quality` | En subproyectos: Kotlin JVM 21 + ktlint + detekt. En root: `installGitHooks` (copia `hooks/pre-commit` y `hooks/post-commit` al consumidor). |
| `com.g16is.conventions.coverage` | Kover con `minBound(80)`. |
| `com.g16is.conventions.publishing` | `java-library` + publicación Maven a GitHub Packages. |

Versión publicada: **1.0.0**

Repositorio Maven: `https://maven.pkg.github.com/G16IS/gradle-conventions`

## Cómo aplicar (consumidor)

GitHub Packages pide token también para bajar. En CI: `GITHUB_ACTOR` + `GITHUB_TOKEN` con `packages: read`. En local: las mismas env vars, o `gpr.user` / `gpr.key` en `gradle.properties` (no commitear el token).

`settings.gradle.kts` — `pluginManagement` primero:

```kotlin
pluginManagement {
    repositories {
        maven {
            name = "GitHubPackagesConventions"
            url = uri("https://maven.pkg.github.com/G16IS/gradle-conventions")
            credentials {
                username = providers.environmentVariable("GITHUB_ACTOR")
                    .orElse(providers.gradleProperty("gpr.user"))
                    .get()
                password = providers.environmentVariable("GITHUB_TOKEN")
                    .orElse(providers.gradleProperty("gpr.key"))
                    .get()
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}
```

`build.gradle.kts` del root:

```kotlin
plugins {
    id("com.g16is.conventions.quality") version "1.0.0"
    id("com.g16is.conventions.coverage") version "1.0.0"
    id("com.g16is.conventions.publishing") version "1.0.0" apply false
}
```

En un módulo:

```kotlin
plugins {
    id("com.g16is.conventions.publishing")
}
```

El plugin `publishing` lee el destino de Packages desde propiedades (no hardcodea un repo):

- `gpr.repository=OWNER/REPO`, o
- `gpr.owner` + `gpr.repo`

Credenciales de publicación del consumidor: `GITHUB_ACTOR` / `GITHUB_TOKEN`.

Detekt: si el consumidor tiene `${rootDir}/config/detekt/detekt.yml`, se usa como override (`buildUponDefaultConfig`). Si el archivo no existe, corre la config default de Detekt.

Git hooks: este repo es la fuente (`hooks/pre-commit`, `hooks/post-commit`). Van en el jar del plugin. En el consumidor, `./gradlew installGitHooks` los escribe en `hooks/` y configura `core.hooksPath`. El consumidor no versiona esos scripts.

## Cómo publicar este paquete

Hace falta `GITHUB_ACTOR` y `GITHUB_TOKEN` con `write:packages` (y acceso a la org `G16IS`). El repo GitHub `G16IS/gradle-conventions` tiene que existir.

```bash
export GITHUB_ACTOR=tu-usuario
export GITHUB_TOKEN=ghp_...
./gradlew publish -Pversion=X.Y.Z
```

Ejemplo: `./gradlew publish -Pversion=1.0.0`

`kotlin-dsl` publica el jar y los plugin markers (`com.g16is.conventions.quality`, `.coverage`, `.publishing`).
