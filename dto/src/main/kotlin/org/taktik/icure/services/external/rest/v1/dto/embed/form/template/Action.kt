package org.taktik.icure.services.external.rest.v1.dto.embed.form.template

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v2.dto.embed.form.template.Launcher
import org.taktik.icure.services.external.rest.v2.dto.embed.form.template.State

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Action(
    val launchers: List<Launcher>? = emptyList(),
    val expression: String? = null,
    val states : List<State>? = emptyList()
)
