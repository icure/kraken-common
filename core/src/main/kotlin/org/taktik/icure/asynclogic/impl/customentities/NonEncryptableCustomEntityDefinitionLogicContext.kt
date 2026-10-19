package org.taktik.icure.asynclogic.impl.customentities

import org.taktik.icure.entities.CustomEntityBase

/**
 * Context for custom entities that do not support delegations/encryption: any encryption metadata in input is
 * rejected.
 */
object NonEncryptableCustomEntityDefinitionLogicContext : AbstractCustomEntityDefinitionLogicContext(emptyMap()) {
	private fun requireNoEncryptionMetadata(entity: CustomEntityBase) {
		require(
			entity.secretForeignKeys.isEmpty() &&
				entity.cryptedForeignKeys.isEmpty() &&
				entity.delegations.isEmpty() &&
				entity.encryptionKeys.isEmpty() &&
				entity.securityMetadata == null
		) {
			"Custom entity type ${entity.entityTypeId} does not support delegations and/or encryption"
		}
	}

	override suspend fun validateAndMapForCreation(entity: CustomEntityBase): CustomEntityBase =
		entity.also { requireNoEncryptionMetadata(it) }

	override suspend fun checkAndMapValidModification(
		currentEntityStub: CustomEntityBase,
		updatedEntity: CustomEntityBase,
	): CustomEntityBase =
		updatedEntity.also { requireNoEncryptionMetadata(it) }

	override suspend fun filterAndMapValidModifications(
		currentEntitiesStubs: Collection<CustomEntityBase>,
		updatedEntities: Collection<CustomEntityBase>,
	): Collection<CustomEntityBase> =
		updatedEntities.onEach { requireNoEncryptionMetadata(it) } // No filter: invalid content in input is a user error

	override suspend fun cleanupPurgedEntity(purgedEntity: CustomEntityBase) {
		// Nothing to do
	}
}
