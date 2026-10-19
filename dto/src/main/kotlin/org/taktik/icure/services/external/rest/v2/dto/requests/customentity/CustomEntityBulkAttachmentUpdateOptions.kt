package org.taktik.icure.services.external.rest.v2.dto.requests.customentity

import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.dto.annotations.filtering.ActiveField
import org.taktik.icure.services.external.rest.v2.dto.requests.document.BulkAttachmentUpdateOptions
import java.io.Serializable

/**
 * Options for performing a bulk update of custom entity attachments in a single request.
 * Allows specifying metadata for new or updated attachments and keys of attachments to delete.
 */
data class CustomEntityBulkAttachmentUpdateOptions(
	@param:Schema(
		description = "Metadata for new attachments or attachments which will be updated, by key.",
	)
	/** Metadata for attachments to add or update, keyed by attachment key. */
	@ActiveField val updateAttachmentsMetadata: Map<String, BulkAttachmentUpdateOptions.AttachmentMetadata> = emptyMap(),
	/** Keys of attachments to delete. */
	@param:Schema(description = "Keys of attachments to delete.")
	@ActiveField val deleteAttachments: Set<String> = emptySet(),
) : Serializable
