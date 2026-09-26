package com.g16is.conventions

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import org.gradle.api.GradleException

@UntrackedTask(because = "Reads and writes local git config")
abstract class InstallGitHooks @Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask() {

    private val bundledHookNames = listOf("pre-commit", "post-commit")

    @get:Internal
    abstract val repoDirectory: DirectoryProperty

    @get:Input
    abstract val hooksDirectoryName: Property<String>

    init {
        group = "build setup"
        description = "Installs the repository git hooks if they are not already installed."
    }

    @TaskAction
    fun install() {
        val root = repoDirectory.get().asFile
        if (!root.resolve(".git").exists()) {
            logger.warn("No .git directory in {}; skipping git hook install", root)
            return
        }

        val hooksName = hooksDirectoryName.get()
        val hooksDir = root.resolve(hooksName)
        copyBundledHooks(hooksDir)

        val current = gitConfigGet(root, "core.hooksPath")
        if (pointsToOurHooks(current, root, hooksDir, hooksName)) {
            logger.lifecycle("Git hooks already installed (core.hooksPath={}).", current)
            return
        }

        gitConfigSet(root, "core.hooksPath", hooksName)
        logger.lifecycle("Installed git hooks: git config core.hooksPath {}", hooksName)
    }

    private fun copyBundledHooks(hooksDir: File) {
        hooksDir.mkdirs()
        bundledHookNames.forEach { name ->
            val resource = javaClass.classLoader.getResourceAsStream("hooks/$name")
                ?: throw GradleException("Missing bundled git hook: hooks/$name")
            val target = File(hooksDir, name)
            resource.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            target.setExecutable(true, false)
        }
        logger.lifecycle("Installed bundled git hooks into {}", hooksDir)
    }

    private fun pointsToOurHooks(
        current: String?,
        root: File,
        hooksDir: File,
        relativeName: String,
    ): Boolean {
        if (current.isNullOrBlank()) return false
        val value = current.trim().trimEnd('/', '\\')
        if (value == relativeName || value == "./$relativeName") return true
        val asFile = File(value)
        val resolved = if (asFile.isAbsolute) asFile else File(root, value)
        return resolved.canonicalFile == hooksDir.canonicalFile
    }

    private fun gitConfigGet(root: File, key: String): String? {
        val stdout = ByteArrayOutputStream()
        val result = execOperations.exec {
            commandLine("git", "config", "--local", "--get", key)
            workingDir = root
            standardOutput = stdout
            errorOutput = ByteArrayOutputStream()
            isIgnoreExitValue = true
        }
        if (result.exitValue != 0) return null
        return stdout.toString(Charsets.UTF_8).trim().ifEmpty { null }
    }

    private fun gitConfigSet(root: File, key: String, value: String) {
        execOperations.exec {
            commandLine("git", "config", "--local", key, value)
            workingDir = root
        }
    }
}
