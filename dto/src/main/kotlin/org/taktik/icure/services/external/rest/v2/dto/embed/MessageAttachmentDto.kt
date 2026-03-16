package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents an attachment associated with a message, specifying its location type and document identifiers.
 */
data class MessageAttachmentDto(
	/** The location type of the attachment (annex or body). */
	val type: DocumentLocationDto? = null,
	/** The list of document identifiers for this attachment. */
	val ids: List<String> = emptyList(),
) : Serializable
