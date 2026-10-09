package com.g16is.conventions

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

@UntrackedTask(because = "Writes into the repository's git hooks directory")
abstract class InstallGitHooks
    @Inject
    constructor(
        private val execOperations: ExecOperations,
    ) : DefaultTask() {
        /** Raíz del repo consumidor (donde está el .git). */
        @get:Internal
        abstract val repoDirectory: DirectoryProperty

        /** Nombres de los hooks bundleados en resources/hooks/ del plugin. */
        @get:Input
        abstract val hookNames: ListProperty<String>

        init {
            group = "build setup"
            description = "Installs the bundled git hooks into the repository's .git/hooks."
        }

        @TaskAction
        fun install() {
            val root = repoDirectory.get().asFile
            if (!root.resolve(".git").exists()) {
                logger.warn("No .git in {}; skipping git hook install", root)
                return
            }

            val hooksDir = resolveHooksDir(root).also { it.mkdirs() }

            hookNames.get().forEach { name ->
                val bytes =
                    javaClass.classLoader
                        .getResourceAsStream("hooks/$name")
                        ?.use { it.readBytes() }
                        ?: throw GradleException("Missing bundled git hook: hooks/$name")

                val target = File(hooksDir, name)
                if (target.exists() && target.readBytes().contentEquals(bytes)) {
                    logger.lifecycle("Hook '{}' already up to date.", name)
                } else {
                    target.writeBytes(bytes)
                    logger.lifecycle("Installed hook '{}' -> {}", name, target)
                }
                target.setExecutable(true, false)
            }
        }

        // Respeta worktrees, submodules y core.hooksPath.
        private fun resolveHooksDir(root: File): File {
            val stdout = ByteArrayOutputStream()
            val result =
                execOperations.exec {
                    commandLine("git", "rev-parse", "--git-path", "hooks")
                    workingDir = root
                    standardOutput = stdout
                    errorOutput = ByteArrayOutputStream()
                    isIgnoreExitValue = true
                }
            val path = stdout.toString(Charsets.UTF_8).trim()
            if (result.exitValue != 0 || path.isEmpty()) return root.resolve(".git/hooks")
            val f = File(path)
            return if (f.isAbsolute) f else File(root, path)
        }
    }
