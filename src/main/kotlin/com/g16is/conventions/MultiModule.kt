package com.g16is.conventions

import org.gradle.api.Project

/**
 * true si el build es multi-módulo (el root solo agrega, el código vive en subproyectos).
 * Se autodetecta por la existencia de subproyectos; se fuerza con
 * `conventions.multiModule=true|false` en gradle.properties.
 */
fun Project.isMultiModule(): Boolean =
    providers
        .gradleProperty("conventions.multiModule")
        .map { it.toBoolean() }
        .getOrElse(rootProject.subprojects.isNotEmpty())
