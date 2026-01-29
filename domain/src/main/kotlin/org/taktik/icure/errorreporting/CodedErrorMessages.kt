package org.taktik.icure.errorreporting

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

interface CodedErrorMessages {
	fun getDefaultMessage(errorCode: String, params: Map<String, Any>): String

	companion object {
		fun basic(): CodedErrorMessages = NoTemplates

		fun fromTemplates(templates: Map<String, String>): CodedErrorMessages =
			WithTemplates(templates)

		fun fromResourceTemplates(
			resourcePath: String
		): CodedErrorMessages {
			val resource = this::class.java.classLoader.getResourceAsStream(resourcePath)
				?: throw IllegalArgumentException("Error template resource not found: $resourcePath")

			val templates = resource.use { inputStream ->
				val objectMapper = ObjectMapper()
				objectMapper.readValue<Map<String, String>>(inputStream)
			}
			return WithTemplates(templates)
		}
	}
}

/**
 * Lightweight implementation, fast, no memory impact, but bad for debugging
 */
private object NoTemplates : CodedErrorMessages {
	override fun getDefaultMessage(errorCode: String, params: Map<String, Any>): String =
		"Error $errorCode (this kraken instance does not provide detailed error messages for this error, for debugging purposes we recommend using nightly or qa builds)"
}

/**
 * Implementation with support for actual human-friendly messages (in a predefined language).
 * Better for debugging, but keeps the messages in RAM and actually does template replacement.
 */
private class WithTemplates(
	private val errorTemplates: Map<String, String>
) : CodedErrorMessages {
	override fun getDefaultMessage(errorCode: String, params: Map<String, Any>): String {
		/*
		 * The implementation is kept intentionally simple.
		 * It could have issues if a parameter value could have the form {{param}} itself:
		 * - Template "Message with param1={{param1}}, param2={{param2}}"
		 * - Params (in order) param1="{{param2}}", param2="x"
		 * - Expected result "Message with param1={{param2}}, param2=x
		 * - Actual result:
		 *   - After replacing param1: "Message with param1={{param2}}, param2={{param2}}"
		 *   - After replacing param2: "Message with param1=x, param2=x"
		 * This is not currently a likely scenario, but if we have such a need in the future we will need to implement a
		 *  more robust templating mechanism or use an existing one.
		 */
		var template = errorTemplates[errorCode] ?: return "Error $errorCode (this code is missing a user-friendly message, please contact support)"
		for ((key, value) in params) {
			template = template.replace("{{$key}}", value.toString())
		}
		return template
	}
}