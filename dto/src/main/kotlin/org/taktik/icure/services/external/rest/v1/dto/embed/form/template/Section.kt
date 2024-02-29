package org.taktik.icure.services.external.rest.v1.dto.embed.form.template

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Section(
	val section: String? = null,
	val fields: List<StructureElement> = emptyList(),
	val description: String? = null,
	val keywords: List<String>? = null,
)
