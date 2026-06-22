package org.taktik.icure.services.external.rest.v2.dto.embed.form.template

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents the layout of a form template, defining the form name, its actions, sections, and metadata.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.embed.form.template.FormTemplateLayout")
data class FormTemplateLayout(
	/** The name or identifier of the form. */
	@param:Schema(required = true)
	@ActiveField val form: String,
	/** The list of actions available in this form template. */
	@ActiveField val actions: List<Action> = emptyList(),
	/** The list of sections composing this form template. */
	@ActiveField val sections: List<Section> = emptyList(),
	/** A description of the form template. */
	@ActiveField val description: String? = null,
	/** Keywords associated with the form template for search purposes. */
	@ActiveField val keywords: List<String>? = null,
)
