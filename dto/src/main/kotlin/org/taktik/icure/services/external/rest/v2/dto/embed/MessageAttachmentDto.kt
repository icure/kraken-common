package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents an attachment associated with a message, specifying its location type and document identifiers.
 */
data class MessageAttachmentDto(
	/** The location type of the attachment (annex or body). */
	@ActiveField val type: DocumentLocationDto? = null,
	/** The list of document identifiers for this attachment. */
	@ActiveField val ids: List<String> = emptyList(),
) : Serializable
