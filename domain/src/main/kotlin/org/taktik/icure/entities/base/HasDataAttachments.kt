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
	 * deleted. It can't be changed by the user: entities provided by the user must always contain the stored value.
	 *
	 * This field is being phased out:
	 * - For [Document] and Receipt it is kept for retro-compatibility. Clients (e.g. the Cardinal SDK) may hide it to
	 *   the user, but must always send back the stored value.
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
}
