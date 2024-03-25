package org.taktik.icure.services.external.rest.v2.dto.requests.document

import java.io.Serializable
import io.swagger.v3.oas.annotations.media.Schema

data class BulkAttachmentUpdateOptions(
    @Schema(description = "Metadata for new attachments or attachments which will be updated, by key. The key for the main attachment is the document id.")
		val updateAttachmentsMetadata: Map<String, AttachmentMetadata> = emptyMap(),
    @Schema(description = "Keys of attachments to delete. The key for the main attachment is the document id.")
		val deleteAttachments: Set<String> = emptySet()
	) : Serializable {

	data class AttachmentMetadata(
		@Schema(description = "The Uniform Type Identifiers (https://developer.apple.com/library/archive/documentation/FileManagement/Conceptual/understanding_utis/understand_utis_conc/understand_utis_conc.html#//apple_ref/doc/uid/TP40001319-CH202-CHDHIJDE) of the attachment. This is a list to allow representing a priority, but each UTI must be unique.")
		val utis: List<String> = emptyList(),
		val dataIsEncrypted: Boolean? = null
	) : Serializable

	}