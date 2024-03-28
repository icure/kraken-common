package com.icure.codegen.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction

abstract class PostProcessDtoTask : DefaultTask() {
	@get:InputDirectory
	abstract val inputDir: DirectoryProperty

	private val quotes = listOf(
		"I'm gonna build my own theme park! With blackjack! And hookers! You know what- forget the park!",
		"Good news, everyone!",
		"I'm back! And with a full tank of gas, and a questionable cache of firearms."
	)

	/**
	 * Post-process the generated DTO files
	 * This is a placeholder on purpose because it is needed on :kraken-common:dto and have an actual implementation in kraken-cloud.
	 * Enjoy the quotes!
	 */
	@TaskAction
	fun postProcess() {
        println(quotes.random())
	}
}