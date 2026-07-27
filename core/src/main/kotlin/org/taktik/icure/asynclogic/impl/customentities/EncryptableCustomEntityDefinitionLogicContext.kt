package org.taktik.icure.asynclogic.impl.customentities

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.CustomEntityDAO
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogicHelper
import org.taktik.icure.entities.CustomEntityBase

@Service
@Profile("app")
class EncryptableCustomEntityDefinitionLogicContext : CustomEntityDefinitionLogicContext {
	private val helper = EntityWithEncryptionMetadataLogicHelper<CustomEntityBase, CustomEntityDAO> {
		copy(securityMetadata = it)
	}

	override suspend fun validateAndMapForCreation(entity: CustomEntityBase): CustomEntityBase {
		require (!helper.hasLegacyEncryptionMetadata(entity)) {
			"Custom entities do not support legacy encryption metadata."
		}
		return entity
	}

	override suspend fun checkValidModification(currentEntityStub: CustomEntityBase, updatedEntity: CustomEntityBase) {
		require (!helper.hasLegacyEncryptionMetadata(updatedEntity)) {
			"Custom entities do not support legacy encryption metadata."
		}
		helper.doValidateEntityChange(
			updatedEntity = updatedEntity,
			currentEntity = currentEntityStub,
			throwErrorOnInvalidRev = true,
		)
	}

	override suspend fun filterValidModifications(currentEntitiesStubs: Collection<CustomEntityBase>, updatedEntities: Collection<CustomEntityBase>): Collection<CustomEntityBase> =
		helper.filterValidEntityChanges(
			updatedEntities = updatedEntities,
			originalEntities = currentEntitiesStubs.asFlow().filter {
				!helper.hasLegacyEncryptionMetadata(it)
			}
		).toList()
}