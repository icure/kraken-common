package org.taktik.icure.asynclogic.objectstorage

import kotlinx.coroutines.flow.Flow
import org.springframework.core.io.buffer.DataBuffer
import org.taktik.icure.entities.objectstorage.DataAttachment

/**
 * Represents a request to change [DataAttachment]s.
 * - [DataAttachmentChange.Delete] delete an existing attachment.
 * - [DataAttachmentChange.CreateOrUpdate] update an existing attachment or create a new one if none exist.
 */
sealed interface DataAttachmentChange {
	/**
	 * Represents a request to delete an attachment.
	 */
	object Delete : DataAttachmentChange

	/**
	 * Represents a request to create or update an attachment.
	 * @param data the content of the attachment.
	 * @param size the real size of [data] in bytes, after compression and encryption on the client side, if not
	 * matching the update will fail. Will be saved on [DataAttachment.realDataSize]
	 * @param utis used differently depending on whether this [DataAttachmentChange] triggers
	 * the creation of a new [DataAttachment] or updates an existing one:
	 * - `Update`: if not null specifies a new value for [DataAttachment.utis].
	 * - `Create`: specifies the initial value for [DataAttachment.utis], in this case `null`
	 *    will be converted to an empty list.
	 * @param dataIsEncrypted if true the mime-type of the attachment in the final storage layer will be
	 * application/octet-stream else the mime-type is taken from [utis]
	 * @param compressionAlgorithm value to set on [DataAttachment.compressionAlgorithm]
	 * @param triedCompressionAlgorithmsVersion value to set on [DataAttachment.triedCompressionAlgorithmsVersion]
	 * @param realDataSize value to set on [DataAttachment.realDataSize]
	 */
	data class CreateOrUpdate(
		val data: Flow<DataBuffer>,
		val size: Long,
		val utis: List<String>?,
		val dataIsEncrypted: Boolean,
		val compressionAlgorithm: String?,
		val triedCompressionAlgorithmsVersion: String?,
		val realDataSize: Long?,
	) : DataAttachmentChange
}
