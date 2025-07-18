package org.taktik.icure.services.external.rest.v2.dto.embed.form.template

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class Launcher(
	val name: String = "",
	val triggerer: Trigger = Trigger.INIT,
	val shouldPassValue: Boolean = false,
)

enum class Trigger {
	INIT,
	CHANGE,
	CLICK,
	VISIBLE,
	ERROR,
	VALID,
	EVENT,
}
