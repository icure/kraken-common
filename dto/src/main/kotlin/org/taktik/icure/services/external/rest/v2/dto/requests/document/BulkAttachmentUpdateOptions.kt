package org.taktik.icure.services.external.rest.v2.dto.requests.document

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * Options for performing a bulk update of document attachments in a single request.
 * Allows specifying metadata for new or updated attachments and keys of attachments to delete.
 */
data class BulkAttachmentUpdateOptions(
	@param:Schema(
		description = "Metadata for new attachments or attachments which will be updated, by key. The key for the main attachment is the document id.",
	)
	/** Metadata for attachments to add or update, keyed by attachment key (the document id for the main attachment). */
	val updateAttachmentsMetadata: Map<String, AttachmentMetadata> = emptyMap(),
	/** Keys of attachments to delete (use the document id for the main attachment). */
	@param:Schema(description = "Keys of attachments to delete. The key for the main attachment is the document id.")
	val deleteAttachments: Set<String> = emptySet(),
) : Serializable {
	/**
	 * Metadata describing a single attachment within a bulk update operation.
	 */
	data class AttachmentMetadata(
		@param:Schema(
			description = "The Uniform Type Identifiers (https://developer.apple.com/library/archive/documentation/FileManagement/Conceptual/understanding_utis/understand_utis_conc/understand_utis_conc.html#//apple_ref/doc/uid/TP40001319-CH202-CHDHIJDE) of the attachment. This is a list to allow representing a priority, but each UTI must be unique.",
		)
		/** The Uniform Type Identifiers for the attachment, ordered by priority; each UTI must be unique. */
		val utis: List<String> = emptyList(),
		/** Whether the attachment data is encrypted. */
		val dataIsEncrypted: Boolean? = null,
		val compressionAlgorithm: String? = null,
		val triedCompressionAlgorithmsVersion: String? = null,
		val realDataSize: Long? = null,
	) : Serializable
}
