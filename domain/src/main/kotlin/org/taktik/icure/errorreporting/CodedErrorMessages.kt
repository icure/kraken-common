package org.taktik.icure.errorreporting

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

interface CodedErrorMessages {
	fun getDefaultMessage(errorCode: String, params: Map<String, String>): String

	/**
	 * Lightweight implementation, fast, no memory impact, but bad for debugging
	 */
	object CodeOnly : CodedErrorMessages {
		override fun getDefaultMessage(errorCode: String, params: Map<String, String>): String =
			"Error $errorCode (this kraken instance does not provide detailed error messages for this error, for debugging purposes we recommend using nightly or qa builds)"
	}

	/**
	 * Implementation with support for actual human-friendly messages (in a predefined language).
	 * Better for debugging, but keeps the messages in RAM and actually does template replacement.
	 */
	class FromResources(
		resourcePath: String
	) : CodedErrorMessages {
		private val errorTemplates: Map<String, String> = loadErrorTemplates(resourcePath)

		private fun loadErrorTemplates(resourcePath: String): Map<String, String> {
			val resource = this::class.java.classLoader.getResourceAsStream(resourcePath)
				?: throw IllegalArgumentException("Error template resource not found: $resourcePath")

			return resource.use { inputStream ->
				val objectMapper = ObjectMapper()
				objectMapper.readValue<Map<String, String>>(inputStream)
			}
		}

		override fun getDefaultMessage(errorCode: String, params: Map<String, String>): String {
			var template = errorTemplates[errorCode] ?: return "Error $errorCode (this code is missing a user-friendly message, please contact support)"
			for ((key, value) in params) {
				template = template.replace("{{$key}}", value)
				// TODO if value has {{nextKey}} that will be replaced...
			}
			return template
		}

		// TODO substitues bad
	}
}