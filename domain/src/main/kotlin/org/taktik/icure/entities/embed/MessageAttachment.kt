package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class MessageAttachment(
	/** The location type of the attachment (annex or body). */
	val type: DocumentLocation? = null,
	/** The list of document identifiers for this attachment. */
	val ids: List<String> = emptyList(),
) : Serializable
