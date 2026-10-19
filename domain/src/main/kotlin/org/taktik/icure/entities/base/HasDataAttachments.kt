package org.taktik.icure.entities.base

import org.taktik.icure.entities.Document
import org.taktik.icure.entities.embed.DeletedAttachment
import org.taktik.icure.entities.objectstorage.DataAttachment
import org.taktik.icure.mergers.annotations.MergeStrategyUse

/**
 * Interface for entities which store part of their data as attachments.
 */
interface HasDataAttachments<T : HasDataAttachments<T>> : StoredDocument {
	/**
	 * All data attachments associated to this entity. The key in this map represents the key of the attachment.
	 * By convention, in entities where there is a main attachment (e.g. [Document]) the key of the main attachment
	 * should be equal to the entity id, in order to avoid accidental collisions.
	 */
	val dataAttachments: Map<String, DataAttachment>

	/**
	 * History of all deleted attachments, automatically updated by the server when an attachment is replaced or
	 * deleted, and never changed by the user.
	 *
	 * This field is being phased out:
	 * - For [Document] and Receipt it is kept for retro-compatibility, but starting from Cardinal
	 *   `FilteringVersions.GENERIC_ENTITIES_CARDINAL_MIN_VERSION` it is not included anymore in the dto, and an empty
	 *   list in input is replaced with the stored value.
	 * - Entities that got data attachments after them (e.g. custom entities) don't use it at all: it is never updated.
	 *
	 * Reasons:
	 * - Its growth is unbounded, which is potentially problematic even without malicious users: for example a bug in a
	 *   client application could leave permanently a lot of unnecessary metadata on the entity.
	 * - Bounding its growth would instead make it unsafe for auditing.
	 *
	 * TODO with the upcoming project-configurable auditing feature:
	 * - Through the auditing configuration it will be possible to choose if the automatic tracking of deleted
	 *   attachments as it works now should be enabled (only possible for [Document] and Receipt): if not enabled
	 *   the deleted attachments will never be updated.
	 * - As part of the auditing feature we will implement a way of keeping track of how the attachments changed,
	 *   outside of the entity itself.
	 */
	@MergeStrategyUse(
		canMerge = "true",
		merge = "mergeDeletedAttachments({{LEFT}}.{{PROP}}, {{RIGHT}}.{{PROP}})"
	)
	val deletedAttachments: List<DeletedAttachment>

	fun withUpdatedDataAttachment(key: String, newValue: DataAttachment?): T

	fun withDataAttachments(newDataAttachments: Map<String, DataAttachment>): T

	fun withDeletedAttachments(newDeletedAttachments: List<DeletedAttachment>): T

	/**
	 * If [deletedAttachmentsMayBeOmitted] (the user uses a version of Cardinal that doesn't include the deleted
	 * attachments in the dto) and this entity, provided by the user, has no deleted attachments, returns a copy of this
	 * entity with the deleted attachments of [currentEntity]; else returns this entity as is.
	 *
	 * This can't be abused to change the deleted attachments, since the user-provided value is never used: the only
	 * possible outcomes are keeping the stored value, or the user-provided value being validated as usual (e.g.
	 * rejected if different from the stored value).
	 */
	fun withOmittedDeletedAttachmentsRestored(currentEntity: T, deletedAttachmentsMayBeOmitted: Boolean): T =
		@Suppress("UNCHECKED_CAST")
		if (deletedAttachmentsMayBeOmitted && deletedAttachments.isEmpty()) {
			withDeletedAttachments(currentEntity.deletedAttachments)
		} else {
			this as T
		}
}
