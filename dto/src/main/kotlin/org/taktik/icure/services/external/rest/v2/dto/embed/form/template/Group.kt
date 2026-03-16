package org.taktik.icure.services.external.rest.v2.dto.embed.form.template

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = JsonDeserializer.None::class)
/**
 * Represents a group of structure elements within a form template section, used to organize fields logically.
 */
data class Group(
	/** The name or identifier of this group. */
	@param:Schema(required = true)
	val group: String,
	/** The list of structure elements (fields or nested groups) in this group. */
	val fields: List<StructureElement>? = null,
) : StructureElement
