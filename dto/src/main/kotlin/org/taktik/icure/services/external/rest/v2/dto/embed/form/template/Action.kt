package org.taktik.icure.services.external.rest.v2.dto.embed.form.template

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Action(
	@Schema(defaultValue = "emptyList()") val launchers: List<Launcher>? = emptyList(),
	val expression: String? = null,
	@Schema(defaultValue = "emptyList()") val states : List<State>? = emptyList()
)
