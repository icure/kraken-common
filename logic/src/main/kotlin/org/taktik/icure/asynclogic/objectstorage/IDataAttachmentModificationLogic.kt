package org.taktik.icure.asynclogic.objectstorage

import org.taktik.icure.entities.Document
import org.taktik.icure.entities.base.HasDataAttachments

/**
 * Shared logic for the modification of entities which have [DataAttachment]s.
 */
interface DataAttachmentModificationLogic<T : HasDataAttachments<T>> {
    /**
     * Verifies that all updates to an entity with data attachments are valid in regard to
     * the data attachment information: some changes to attachment information can only be
     * executed through [updateAttachments]. This prevents accidental loss of information.
     *
     * The following changes are considered invalid:
     * 1. The new version of an entity specifies some attachments which do not exist in the
     * current version.
     * 2. The new version changes the value of a [IDataAttachment.couchDbAttachmentId] or
     * [DataAttachment.objectStoreAttachmentId].
     * 3. Any change in the [HasDataAttachments.deletedAttachments]
     *
     * In most cases if there is an invalid change this method will throw an [IllegalArgumentException],
     * however it is possible to specify to have a lenient behaviour for some attachments, in order to
     * preserve retro-compatibility. All invalid changes to attachment data that is mapped to a key
     * in [lenientKeys] will be simply ignored, without triggering an [IllegalArgumentException].
     *
     * @param currEntity the current value of the entity being updated
     * @param newEntity the new desired value for the entity
     * @param lenientKeys keys of attachments to exclude from the check.
     * @return an updated version of newEntity which does not have any invalid change.
     * @throws IllegalArgumentException if there are invalid changes not related to lenient keys.
     */
    fun ensureValidAttachmentChanges(currEntity: T, newEntity: T, lenientKeys: Set<String>): T

    /**
     * Updates an entity attachments, also performing any side-tasks necessary for the appropriate
     * storage of the attachments content.
     * @param currEntity the current value of the entity which needs to be updated.
     * @param changes the changes to apply to the entity attachments.
     * @return the updated entity
     * @throws ObjectStorageException if one or more attachments must be stored using the object
     * storage service but this is not possible at the moment.
     */
    suspend fun updateAttachments(currEntity: T, changes: Map<String, DataAttachmentChange>): T?

}

interface DocumentDataAttachmentModificationLogic : DataAttachmentModificationLogic<Document>

interface TmpDocumentDataAttachmentModificationLogic : DataAttachmentModificationLogic<Document>
