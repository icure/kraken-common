package org.taktik.icure.services.external.rest.v2.dto.embed.form.template

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class FormTemplateLayout(
	@get:Schema(required = true)
	val form: String,
	val actions: List<Action> = emptyList(),
	val sections: List<Section> = emptyList(),
	val description: String? = null,
	val keywords: List<String>? = null,
)
