package org.taktik.icure.asynclogic.impl.customentities

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asynclogic.objectstorage.CustomEntityDataAttachmentModificationLogic
import org.taktik.icure.entities.CustomEntityBase

@Service
@Profile("app")
class AttachmentCustomEntityDefinitionLogicContext(
	private val attachmentModificationLogic: CustomEntityDataAttachmentModificationLogic,
) : CustomEntityDefinitionLogicContext {
	override suspend fun checkValidModification(
		currentEntityStub: CustomEntityBase,
		updatedEntity: CustomEntityBase,
	) {
		attachmentModificationLogic.ensureValidAttachmentChanges(
			currEntity = currentEntityStub,
			newEntity = updatedEntity,
			lenientKeys = emptySet()
		)
	}

	override suspend fun filterValidModifications(
		currentEntitiesStubs: Collection<CustomEntityBase>,
		updatedEntities: Collection<CustomEntityBase>,
	): Collection<CustomEntityBase> {
		val currentEntitiesStubsById = currentEntitiesStubs.associateBy { it.id }
		return currentEntitiesStubs.filter {
			val matchingCurrent = currentEntitiesStubsById[it.id]
			matchingCurrent != null && kotlin.runCatching {
				checkValidModification(currentEntityStub = matchingCurrent, updatedEntity = it)
			}.isSuccess
		}
	}
}