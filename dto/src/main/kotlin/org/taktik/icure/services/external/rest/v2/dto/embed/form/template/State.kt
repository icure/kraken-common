package org.taktik.icure.services.external.rest.v2.dto.embed.form.template

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a state change applied to a form field when an action is triggered.
 */
class State(
	/** The name of the target field. */
	@ActiveField val name: String = "",
	/** The aspect of the field state to update (value, visibility, readonly, etc.). */
	@ActiveField val stateToUpdate: StateToUpdate = StateToUpdate.VISIBLE,
	/** Whether this state change can trigger additional launchers. */
	@ActiveField val canLaunchLauncher: Boolean = false,
)

/**
 * Enumerates the aspects of a form field state that can be updated by an action.
 */
enum class StateToUpdate {
	VALUE,
	VISIBLE,
	READONLY,
	CLAZZ,
	REQUIRED,
}
