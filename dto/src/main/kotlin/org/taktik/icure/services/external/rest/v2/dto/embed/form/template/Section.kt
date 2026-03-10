package org.taktik.icure.services.external.rest.v2.dto.embed.form.template

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a section within a form template layout, containing a set of structure elements (fields and groups).
 */
data class Section(
	/** The name or identifier of this section. */
	@param:Schema(required = true)
	val section: String,
	/** The list of structure elements in this section. */
	@param:Schema(required = true)
	val fields: List<StructureElement>,
	/** A description of this section. */
	val description: String? = null,
	/** Keywords associated with this section for search purposes. */
	val keywords: List<String>? = null,
)
