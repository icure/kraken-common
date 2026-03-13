package org.taktik.icure.services.external.rest.v2.dto.embed.form.template

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a launcher that triggers an action in a form template based on a specific trigger event.
 */
class Launcher(
	/** The name of the launcher. */
	val name: String = "",
	/** The trigger event that activates this launcher. */
	val triggerer: Trigger = Trigger.INIT,
	/** Whether the current field value should be passed when the launcher is triggered. */
	val shouldPassValue: Boolean = false,
)

/**
 * Enumerates the trigger events that can activate a launcher in a form template.
 */
enum class Trigger {
	INIT,
	CHANGE,
	CLICK,
	VISIBLE,
	ERROR,
	VALID,
	EVENT,
}
