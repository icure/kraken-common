package org.taktik.icure.services.external.rest.v1.dto.embed.form.template

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v2.dto.embed.form.template.StateToUpdate

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class State(
	val name: String = "",
	val stateToUpdate: StateToUpdate = StateToUpdate.VISIBLE,
	val canLaunchLauncher: Boolean = false,
)

enum class StateToUpdate {
	VALUE,
	VISIBLE,
	READONLY,
	CLAZZ,
	REQUIRED,
}
