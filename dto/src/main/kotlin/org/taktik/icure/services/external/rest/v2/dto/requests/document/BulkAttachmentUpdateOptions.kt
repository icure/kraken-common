package org.taktik.icure.services.external.rest.v2.dto.requests.document

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * Options for performing a bulk update of document attachments in a single request.
 * Allows specifying metadata for new or updated attachments and keys of attachments to delete.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.requests.document.BulkAttachmentUpdateOptions")
data class BulkAttachmentUpdateOptions(
	@param:Schema(
		description = "Metadata for new attachments or attachments which will be updated, by key. The key for the main attachment is the document id.",
	)
	/** Metadata for attachments to add or update, keyed by attachment key (the document id for the main attachment). */
	@ActiveField val updateAttachmentsMetadata: Map<String, AttachmentMetadata> = emptyMap(),
	/** Keys of attachments to delete (use the document id for the main attachment). */
	@param:Schema(description = "Keys of attachments to delete. The key for the main attachment is the document id.")
	@ActiveField val deleteAttachments: Set<String> = emptySet(),
) : Serializable {
	/**
	 * Metadata describing a single attachment within a bulk update operation.
	 */
	@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.requests.document.BulkAttachmentUpdateOptions.AttachmentMetadata")
	data class AttachmentMetadata(
		@param:Schema(
			description = "The Uniform Type Identifiers (https://developer.apple.com/library/archive/documentation/FileManagement/Conceptual/understanding_utis/understand_utis_conc/understand_utis_conc.html#//apple_ref/doc/uid/TP40001319-CH202-CHDHIJDE) of the attachment. This is a list to allow representing a priority, but each UTI must be unique.",
		)
		/** The Uniform Type Identifiers for the attachment, ordered by priority; each UTI must be unique. */
		@ActiveField val utis: List<String> = emptyList(),
		/** Whether the attachment data is encrypted. */
		@ActiveField val dataIsEncrypted: Boolean? = null,
		@ActiveField val compressionAlgorithm: String? = null,
		@ActiveField val triedCompressionAlgorithmsVersion: String? = null,
		@ActiveField val realDataSize: Long? = null,
	) : Serializable
}
