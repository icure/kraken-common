package org.taktik.icure.asynclogic.impl.customentities

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.CustomEntityDAO
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.CustomEntityBase
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.utils.toMap
import org.taktik.icure.validation.EntityIdValidation
import org.taktik.icure.validation.aspect.Fixer

@Service
@Profile("app")
class BasicCustomEntityDefinitionLogicContext(
	private val fixer: Fixer,
) : CustomEntityDefinitionLogicContext {
	override suspend fun validateAndMapForCreation(entity: CustomEntityBase): CustomEntityBase {
		if (entity.rev != null) throw IllegalArgumentException("Cannot create an entity with revision.")
		EntityIdValidation.checkValidForCreation(entity.id)
		return fixer.fix(entity, isCreate = true)
	}

	override suspend fun checkAndMapValidModification(currentEntityStub: CustomEntityBase, updatedEntity: CustomEntityBase): CustomEntityBase {
		if (updatedEntity.rev == null) throw IllegalArgumentException("Cannot update an entity without revision.")
		if (currentEntityStub.rev != updatedEntity.rev) throw ConflictRequestException("Entity ${updatedEntity.id} has been modified since it was retrieved.")
		require(currentEntityStub.entityTypeId == updatedEntity.entityTypeId) {
			"Entity ${updatedEntity.id} is not of expected type."
		}
		return fixer.fix(updatedEntity, isCreate = false)
	}

	override suspend fun filterAndMapValidModifications(currentEntitiesStubs: Collection<CustomEntityBase>, updatedEntities: Collection<CustomEntityBase>): Collection<CustomEntityBase> {
		if (updatedEntities.any { it.rev == null }) throw IllegalArgumentException("Cannot update an entity without revision.") // No filter because hard user error
		val existingById = currentEntitiesStubs.associateBy { it.id }
		return updatedEntities.mapNotNull { updatedEntity ->
			existingById[updatedEntity.id]?.let { existing ->
				require (existing.entityTypeId == updatedEntity.entityTypeId) {
					"Entity ${updatedEntity.id} is not of expected type."
				}
				if (updatedEntity.rev == existing.rev) {
					fixer.fix(updatedEntity, isCreate = false)
				} else null
			}
		}
	}
}