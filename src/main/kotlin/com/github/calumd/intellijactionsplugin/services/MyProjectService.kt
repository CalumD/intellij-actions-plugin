package com.github.calumd.intellijactionsplugin.services

import com.github.calumd.intellijactionsplugin.MyBundle
import com.intellij.openapi.project.Project

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
