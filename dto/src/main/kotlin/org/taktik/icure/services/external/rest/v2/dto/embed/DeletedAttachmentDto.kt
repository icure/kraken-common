package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents metadata about an attachment that has been deleted, preserving its former storage identifiers and deletion time.
 */
data class DeletedAttachmentDto(
	/** The former CouchDB attachment identifier, if the attachment was stored as a CouchDB attachment. */
	@param:Schema(description = "If the attachment was stored as a couchdb attachment this holds the id of the attachment, else null.")
	val couchDbAttachmentId: String? = null,
	/** The former object storage identifier, if the attachment was stored using the object storage service. */
	@param:Schema(description = "If the attachment was stored using the object storage servicxe this holds the id of the attachment, else null.")
	val objectStoreAttachmentId: String? = null,
	@param:Schema(
		description =
		"If the attachment was associated to a key this was its key, else null. In documents a deleted main attachment will " +
			"have a null key, and a deleted secondary attachment will have the key it was originally associated to in the map.",
	)
	/** The key the attachment was associated to, or null for main attachments. */
	val key: String? = null,
	/** The timestamp (unix epoch in ms) when the attachment was deleted. */
	@param:Schema(description = "Instant the attachment was deleted.")
	val deletionTime: Long? = null,
) : Serializable
