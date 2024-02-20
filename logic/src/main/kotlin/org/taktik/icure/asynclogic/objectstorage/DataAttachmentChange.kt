package org.taktik.icure.asynclogic.objectstorage

import kotlinx.coroutines.flow.Flow
import org.springframework.core.io.buffer.DataBuffer

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
     * @param size the size of the attachment content. This value can help to decide the most appropriate storage location for the attachment.
     * @param utis used differently depending on whether this [DataAttachmentChange] triggers
     * the creation of a new [DataAttachment] or updates an existing one:
     * - `Update`: if not null specifies a new value for [DataAttachment.utis].
     * - `Create`: specifies the initial value for [DataAttachment.utis], in this case `null`
     *    will be converted to an empty list.
     */
    data class CreateOrUpdate(
        val data: Flow<DataBuffer>,
        val size: Long,
        val utis: List<String>?,
        val dataIsEncrypted: Boolean
    ) : DataAttachmentChange
}
