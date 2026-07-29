package org.taktik.icure.asynclogic.impl.customentities

import com.icure.cardinal.customentities.config.CustomEntityDefinition
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.entities.CustomEntityBase

@Service
@Profile("app")
class CustomEntityDefinitionLogicContextFactory(
	val basicCustomEntityDefinitionLogicContext: BasicCustomEntityDefinitionLogicContext,
	val encryptableCustomEntityDefinitionLogicContext: EncryptableCustomEntityDefinitionLogicContext,
	val attachmentCustomEntityDefinitionLogicContext: AttachmentCustomEntityDefinitionLogicContext
) {
	fun getContextFor(
		customEntityDefinition: CustomEntityDefinition,
	): CustomEntityDefinitionLogicContext {
		val allContexts = listOfNotNull(
			basicCustomEntityDefinitionLogicContext,
			when (customEntityDefinition.accessControl) {
				CustomEntityDefinition.AccessControl.DelegationBased -> encryptableCustomEntityDefinitionLogicContext
				CustomEntityDefinition.AccessControl.RoleBased -> null
			},
			if (customEntityDefinition.attachmentsConfiguration != null) attachmentCustomEntityDefinitionLogicContext else null,
		)
		return ContextChain.of(allContexts)
	}

	private class ContextChain private constructor(
		val contexts: List<CustomEntityDefinitionLogicContext>
	) : CustomEntityDefinitionLogicContext {
		companion object {
			fun of(contexts: List<CustomEntityDefinitionLogicContext>): CustomEntityDefinitionLogicContext =
				if (contexts.size == 1) contexts.first() else ContextChain(contexts)
		}

		override suspend fun validateAndMapForCreation(entity: CustomEntityBase): CustomEntityBase =
			contexts.fold(entity) { acc, context -> context.validateAndMapForCreation(acc) }

		override suspend fun checkValidModification(
			currentEntityStub: CustomEntityBase,
			updatedEntity: CustomEntityBase,
		) =
			contexts.forEach {
				it.checkValidModification(
					currentEntityStub = currentEntityStub,
					updatedEntity = updatedEntity
				)
			}

		override suspend fun filterValidModifications(
			currentEntitiesStubs: Collection<CustomEntityBase>,
			updatedEntities: Collection<CustomEntityBase>,
		): Collection<CustomEntityBase> =
			contexts.fold(Pair(currentEntitiesStubs, updatedEntities)) { (stubs, updated), context ->
				if (updated.isEmpty()) {
					Pair(emptyList(), emptyList())
				} else {
					val newUpdated = context.filterValidModifications(
						currentEntitiesStubs = stubs,
						updatedEntities = updated
					)
					val newUpdatedIds = newUpdated.mapTo(mutableSetOf()) { it.id }
					Pair(
						stubs.filter { it.id in newUpdatedIds },
						newUpdated
					)
				}
			}.second

		override suspend fun cleanupPurgedEntity(purgedEntity: CustomEntityBase) {
			contexts.forEach { it.cleanupPurgedEntity(purgedEntity) }
		}
	}
}