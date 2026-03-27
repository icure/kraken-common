package org.taktik.icure.errorreporting

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.use


fun CodedErrorMessages.Companion.fromResourceTemplates(
	resourcePath: String
): CodedErrorMessages {
	val resource = this::class.java.classLoader.getResourceAsStream(resourcePath)
		?: throw IllegalArgumentException("Error template resource not found: $resourcePath")

	val templates = resource.use { inputStream ->
		val objectMapper = ObjectMapper()
		objectMapper.readValue<Map<String, String>>(inputStream)
	}
	return CodedErrorMessages.fromTemplates(templates)
}