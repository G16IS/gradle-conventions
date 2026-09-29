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

GitHub Packages pide token también para bajar. En local, igual que el TCK: `gpr.user` / `gpr.key` en `gradle.properties` (gitignored). En CI: `USERNAME`/`TOKEN` o `GITHUB_ACTOR`/`GITHUB_TOKEN` con `packages: read`.

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

## Continuous delivery

Mismo esquema que PrintScript. `GITHUB_TOKEN` del workflow tiene `packages: write` sobre **este** repo, así que Actions puede publicar a `https://maven.pkg.github.com/G16IS/gradle-conventions`.

| Workflow | Cuándo | Qué hace |
|---|---|---|
| `Version Tag` (`.github/workflows/version.yml`) | push a `main` | Tag semver y llama a Publish. Sin tags → `v1.0.0`. Merge de PR → bump **minor**. Push directo → bump **patch**. |
| `Publish` (`.github/workflows/publish.yml`) | `workflow_call` / GitHub Release / `workflow_dispatch` | `./gradlew publish -Pversion=X.Y.Z` (el tag sin el prefijo `v`) |

Major: crear un GitHub Release con tag `vX.0.0`. Republicar: Actions → Publish → `workflow_dispatch` con el tag.

## Cómo publicar a mano

Hace falta `GITHUB_ACTOR` y un PAT classic con `write:packages` (y `repo` si el repo es privado). El repo GitHub `G16IS/gradle-conventions` tiene que existir. El token OAuth de `gh` (`gho_`) no autentica Maven Packages.

```bash
export GITHUB_ACTOR=tu-usuario
export GITHUB_TOKEN=ghp_...
./gradlew publish -Pversion=X.Y.Z
```

Ejemplo: `./gradlew publish -Pversion=1.0.0`

`kotlin-dsl` publica el jar y los plugin markers (`com.g16is.conventions.quality`, `.coverage`, `.publishing`).
