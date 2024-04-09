package com.icure.codegen.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction

abstract class MoveCommonSdkTask : DefaultTask() {
    @get:InputDirectory
    abstract val srcDir: DirectoryProperty
    @get:InputDirectory
    abstract val dstDir: DirectoryProperty

    @TaskAction
    fun postProcess() {
        println("This is a placeholder for MoveCommonSdkTask.")
        println("Wow, this is amazing!")
    }
}