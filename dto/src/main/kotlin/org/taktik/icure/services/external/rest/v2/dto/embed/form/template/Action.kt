package org.taktik.icure.services.external.rest.v2.dto.embed.form.template

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents an action in a form template, combining launchers, a conditional expression, and target states.
 */
data class Action(
	/** The list of launchers that can trigger this action. */
	@param:Schema(defaultValue = "emptyList()") val launchers: List<Launcher>? = emptyList(),
	/** A conditional expression that determines when this action is executed. */
	val expression: String? = null,
	/** The list of states to apply when this action is triggered. */
	@param:Schema(defaultValue = "emptyList()") val states: List<State>? = emptyList(),
)
