package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonFilter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFilter("dataAttachmentFilter")
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
	/**
	 * Algorithm used on the CLIENT SIDE to compress the data attachment.
	 * Null means that the document was not compressed because the tried algorithms could not actually compress the data
	 * (because for example it was an already compressed format) or no algorithms were tried.
	 */
	val compressionAlgorithm: String? = null,
	/**
	 * A string used by the SDK to mark which compression algorithms were tried.
	 * Null means that no compression algorithms were tried.
	 * If an SDK reads some data that is not compressed, if this value indicates that the data was created with an older
	 * version of the SDK then the SDK may try to use any newly available algorithms to compress the data.
	 */
	val triedCompressionAlgorithmsVersion: String? = null,
	/**
	 * Value computed by the backend, the actual size of the data stored for the attachment, in bytes.
	 */
	val storedDataSize: Long? = null,
	/**
	 * Value provided by the client, the real size of the data after it has been decrypted and decompressed, in bytes.
	 * This value is not used or verified by the backend.
	 */
	val realDataSize: Long? = null,
) : Serializable
