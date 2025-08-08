/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.gui

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonDiscriminator
import org.taktik.icure.services.external.rest.v1.dto.gui.type.Data
import org.taktik.icure.services.external.rest.v1.handlers.JacksonEditorV1Deserializer
import java.io.Serializable

@JsonDiscriminator("key")
@JsonDeserialize(using = JacksonEditorV1Deserializer::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
abstract class Editor(
	val left: Double? = null,
	val top: Double? = null,
	val width: Double? = null,
	val height: Double? = null,
	val isMultiline: Boolean = false,
	val labelPosition: LabelPosition? = null,
	val isReadOnly: Boolean = false,
	val defaultValue: Data? = null,
) : Serializable {
	@JsonProperty("key")
	private fun includeDiscriminator(): String = this.javaClass.simpleName
}
