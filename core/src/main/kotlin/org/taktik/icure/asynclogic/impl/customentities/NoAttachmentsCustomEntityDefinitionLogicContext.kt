package org.taktik.icure.asynclogic.impl.customentities

import org.taktik.icure.entities.CustomEntityBase

/**
 * Context for custom entities that never supported attachments (no attachments configuration): any attachment
 * information in input is rejected.
 */
object NoAttachmentsCustomEntityDefinitionLogicContext : AbstractCustomEntityDefinitionLogicContext(emptyMap()) {
	private fun requireNoAttachments(entity: CustomEntityBase) {
		require(entity.dataAttachments.isEmpty()) {
			"Custom entity type ${entity.entityTypeId} does not support attachments"
		}
	}

	override suspend fun validateAndMapForCreation(entity: CustomEntityBase): CustomEntityBase =
		entity.also { requireNoAttachments(it) }

	override suspend fun checkAndMapValidModification(
		currentEntityStub: CustomEntityBase,
		updatedEntity: CustomEntityBase,
	): CustomEntityBase =
		updatedEntity.also { requireNoAttachments(it) }

	override suspend fun filterAndMapValidModifications(
		currentEntitiesStubs: Collection<CustomEntityBase>,
		updatedEntities: Collection<CustomEntityBase>,
	): Collection<CustomEntityBase> =
		updatedEntities.onEach { requireNoAttachments(it) } // No filter: invalid content in input is a user error

	override suspend fun cleanupPurgedEntity(purgedEntity: CustomEntityBase) {
		// Nothing to do
	}
}
