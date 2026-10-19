package org.taktik.icure.asynclogic.impl.customentities

import org.taktik.icure.asynclogic.objectstorage.CustomEntityDataAttachmentModificationLogic
import org.taktik.icure.entities.CustomEntityBase

/**
 * Base context for custom entities that support attachments (currently enabled or not).
 *
 * The attachment information can never be changed through the creation or modification of the entity: no attachment
 * information can be changed without changing the actual attachment, through the attachment-specific methods.
 * Note: this is stricter than for documents, where for retro-compatibility some attachment information (e.g. utis)
 * can also be changed through modify.
 *
 * Existing attachments are preserved on modify, and cleaned up on purge.
 */
abstract class AbstractAttachmentCustomEntityDefinitionLogicContext(
	helpers: Map<CustomEntityLogicContextHelper.Key<*>, Any>,
	private val attachmentModificationLogic: CustomEntityDataAttachmentModificationLogic,
) : AbstractCustomEntityDefinitionLogicContext(helpers) {
	/**
	 * Error message for attempts to provide attachment information on creation
	 */
	protected abstract fun invalidAttachmentChangeMessage(entityType: String): String

	override suspend fun validateAndMapForCreation(entity: CustomEntityBase): CustomEntityBase {
		require(entity.dataAttachments.isEmpty()) {
			invalidAttachmentChangeMessage(entity.entityTypeId)
		}
		return entity
	}

	override suspend fun checkAndMapValidModification(
		currentEntityStub: CustomEntityBase,
		updatedEntity: CustomEntityBase,
	): CustomEntityBase =
		// The custom entity attachment modification logic uses the strict validation: no attachment information can be
		// changed (allowed changes to UTI on document were there for legacy compatibility; no need to keep on new type)
		attachmentModificationLogic.ensureValidAttachmentChanges(
			currEntity = currentEntityStub,
			// The couchdb attachments are never provided by the user (not part of the dto), keep the current ones, else
			// saving would also delete them (same as for documents)
			newEntity = updatedEntity.copy(attachments = currentEntityStub.attachments),
			lenientKeys = emptySet()
		)

	override suspend fun filterAndMapValidModifications(
		currentEntitiesStubs: Collection<CustomEntityBase>,
		updatedEntities: Collection<CustomEntityBase>,
	): Collection<CustomEntityBase> {
		val currentEntitiesStubsById = currentEntitiesStubs.associateBy { it.id }
		// No filter: invalid attachment changes in input are a user error
		return updatedEntities.mapNotNull {
			currentEntitiesStubsById[it.id]?.let { matchingCurrent ->
				checkAndMapValidModification(currentEntityStub = matchingCurrent, updatedEntity = it)
			}
		}
	}

	override suspend fun cleanupPurgedEntity(purgedEntity: CustomEntityBase) {
		attachmentModificationLogic.cleanupPurgedEntityAttachments(purgedEntity)
	}
}
