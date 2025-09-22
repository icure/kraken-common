package org.taktik.icure.serialization

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule

object IcureDomainObjectMapper {
	/**
	 * Get a new instance of object mapper with appropriate configuration for the serialization and deserialization of
	 * iCure entities.
	 */
	fun new() = ObjectMapper().registerModule(
		KotlinModule.Builder()
			.configure(KotlinFeature.NullIsSameAsDefault, true)
			.configure(KotlinFeature.NullToEmptyMap, true)
			.configure(KotlinFeature.NullToEmptyCollection, true)
			.build(),
	).registerModule(
		JacksonModules.strictFloatsModule
	).apply {
		setSerializationInclusion(JsonInclude.Include.NON_NULL)
		configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true)
	}
}
