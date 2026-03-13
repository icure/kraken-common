package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a data attachment that can be stored either as a CouchDB attachment or via object storage.
 */
data class DataAttachmentDto(
	/** The identifier of the attachment if stored as a CouchDB attachment. */
	@param:Schema(description = "Id of the attachment, if stored as a couchdb attachment") val couchDbAttachmentId: String? = null,
	/** The identifier of the attachment if stored using the object storage service. */
	@param:Schema(description = "Id of the attachment, if stored using the object storage service") val objectStoreAttachmentId: String? = null,
	@param:Schema(
		description = "The Uniform Type Identifiers (https://developer.apple.com/library/archive/documentation/FileManagement/Conceptual/understanding_utis/understand_utis_conc/understand_utis_conc.html#//apple_ref/doc/uid/TP40001319-CH202-CHDHIJDE) of the attachment. This is a list to allow representing a priority, but each UTI must be unique.",
	/** The Uniform Type Identifiers of the attachment, ordered by priority. */
	) val utis: List<String> = emptyList(),
) : Serializable
