package org.taktik.icure.asynclogic.impl.customentities

import com.icure.cardinal.customentities.config.CustomEntityDefinition
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.CustomEntityDAO
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogicHelper
import org.taktik.icure.entities.CustomEntityBase

@Service
@Profile("app")
class CustomEntityDefinitionLogicContextFactory(
	val basicCustomEntityDefinitionLogicContext: BasicCustomEntityDefinitionLogicContext,
	val encryptableCustomEntityDefinitionLogicContext: EncryptableCustomEntityDefinitionLogicContext,
	val attachmentCustomEntityDefinitionLogicContext: AttachmentCustomEntityDefinitionLogicContext,
	val disabledAttachmentsCustomEntityDefinitionLogicContext: DisabledAttachmentsCustomEntityDefinitionLogicContext,
) {
	fun getContextFor(
		customEntityDefinition: CustomEntityDefinition,
	): CustomEntityDefinitionLogicContext {
		val attachmentsConfiguration = customEntityDefinition.attachmentsConfiguration
		val allContexts = listOf(
			basicCustomEntityDefinitionLogicContext,
			when (customEntityDefinition.securityModel) {
				is CustomEntityDefinition.SecurityModel.DelegationBased -> encryptableCustomEntityDefinitionLogicContext
				CustomEntityDefinition.SecurityModel.RoleBased -> NonEncryptableCustomEntityDefinitionLogicContext
			},
			when {
				attachmentsConfiguration == null -> NoAttachmentsCustomEntityDefinitionLogicContext
				attachmentsConfiguration.disable -> disabledAttachmentsCustomEntityDefinitionLogicContext
				else -> attachmentCustomEntityDefinitionLogicContext
			},
		)
		return ContextChain.of(allContexts)
	}

	private class ContextChain(
		val contexts: List<AbstractCustomEntityDefinitionLogicContext>
	) : AbstractCustomEntityDefinitionLogicContext(
		kotlin.run {
			val mergedHelpers = mutableMapOf<CustomEntityLogicContextHelper.Key<*>, Any>()
			contexts.forEach { ctx ->
				val duplicate = ctx.helpers.keys.firstOrNull {
					it in mergedHelpers.keys
				}
				check (duplicate == null) {
					"Duplicate helper key in merged context: $duplicate"
				}
				mergedHelpers.putAll(ctx.helpers)
			}
			mergedHelpers
		}
	) {
		companion object {
			fun of(contexts: List<AbstractCustomEntityDefinitionLogicContext>): CustomEntityDefinitionLogicContext =
				if (contexts.size == 1) contexts.first() else ContextChain(contexts)
		}

		override suspend fun validateAndMapForCreation(entity: CustomEntityBase): CustomEntityBase =
			contexts.fold(entity) { acc, context -> context.validateAndMapForCreation(acc) }

		override suspend fun checkAndMapValidModification(
			currentEntityStub: CustomEntityBase,
			updatedEntity: CustomEntityBase,
		) =
			contexts.fold(updatedEntity) { updated, ctx ->
				ctx.checkAndMapValidModification(
					currentEntityStub = currentEntityStub,
					updatedEntity = updated
				)
			}

		override suspend fun filterAndMapValidModifications(
			currentEntitiesStubs: Collection<CustomEntityBase>,
			updatedEntities: Collection<CustomEntityBase>,
		): Collection<CustomEntityBase> =
			contexts.fold(Pair(currentEntitiesStubs, updatedEntities)) { (stubs, updated), context ->
				if (updated.isEmpty()) {
					Pair(emptyList(), emptyList())
				} else {
					val newUpdated = context.filterAndMapValidModifications(
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