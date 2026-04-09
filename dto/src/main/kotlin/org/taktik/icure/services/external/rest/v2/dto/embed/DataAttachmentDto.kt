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
	/** if the attachment is stored as a couchdb attachment this holds the id of the attachment, else null. */
	@param:Schema(description = "Id of the attachment, if stored as a couchdb attachment") val couchDbAttachmentId: String? = null,
	/** if the attachment is stored with the object storage service this holds the id of the attachment, else null. */
	@param:Schema(description = "Id of the attachment, if stored using the object storage service") val objectStoreAttachmentId: String? = null,
	/** [Uniform Type Identifiers](https://developer.apple.com/library/archive/documentation/FileManagement/Conceptual/understanding_utis/understand_utis_conc/understand_utis_conc.html#//apple_ref/doc/uid/TP40001319-CH202-CHDHIJDE) for the data attachment. This is a list in order to allow specifying a priority, but each uti must be unique. */
	@param:Schema(
		description = "The Uniform Type Identifiers (https://developer.apple.com/library/archive/documentation/FileManagement/Conceptual/understanding_utis/understand_utis_conc/understand_utis_conc.html#//apple_ref/doc/uid/TP40001319-CH202-CHDHIJDE) of the attachment. This is a list to allow representing a priority, but each UTI must be unique.",
	) val utis: List<String> = emptyList(),
) : Serializable
