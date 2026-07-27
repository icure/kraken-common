package org.taktik.icure.asynclogic.impl

import com.icure.cardinal.customentities.util.CachedCustomEntitiesConfigurationProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.CustomEntityDAO
import org.taktik.icure.asynclogic.impl.customentities.CustomEntityDefinitionLogicContext
import org.taktik.icure.asyncdao.results.filterSuccessfulUpdates
import org.taktik.icure.asynclogic.CustomEntityLogic
import org.taktik.icure.asynclogic.impl.customentities.CustomEntityDefinitionLogicContextFactory
import org.taktik.icure.asynclogic.objectstorage.ObjectStorageEntityGroupName
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.CustomEntityBase
import org.taktik.icure.entities.Document
import org.taktik.icure.exceptions.NotFoundRequestException


@Service
@Profile("app")
class CustomEntityLogicImpl(
	val dao: CustomEntityDAO,
	val datastoreInstanceProvider: DatastoreInstanceProvider,
	val customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
	val customEntityContextFactory: CustomEntityDefinitionLogicContextFactory,
) : CustomEntityLogic {
	private suspend fun getInstanceAndGroup(): IDatastoreInformation = datastoreInstanceProvider.getInstanceAndGroup()

	private suspend inline fun <T> withDefinitionContext(
		entityType: String,
		entities: List<CustomEntityBase>,
		block: suspend CustomEntityDefinitionLogicContext.() -> T,
	): T {
		entities.forEach { check(it.entityTypeId == entityType) } // Should have been already checked by caller
		return withDefinitionContext(entityType, block)
	}

	private suspend inline fun <T> withDefinitionContext(
		entityType: String,
		entity: CustomEntityBase,
		block: suspend CustomEntityDefinitionLogicContext.() -> T,
	): T {
		check(entity.entityTypeId == entityType) // Should have been already checked by caller
		return withDefinitionContext(entityType, block)
	}

	private suspend inline fun <T> withDefinitionContext(
		entityType: String,
		block: suspend CustomEntityDefinitionLogicContext.() -> T,
	): T {
		val definition = checkNotNull(customEntitiesConfigurationProvider.getConfigForCurrentUser()?.customEntities[entityType]) // Should have been already checked by caller
		return customEntityContextFactory.getContextFor(definition).block()
	}

	private inline fun <T> flowWithDefinitionContext(
		entityType: String,
		crossinline block: suspend CustomEntityDefinitionLogicContext.() -> Flow<T>,
	): Flow<T> = flow {
		withDefinitionContext(entityType) {
			emitAll(block())
		}
	}

	override suspend fun createCustomEntity(
		entityType: String,
		entity: CustomEntityBase,
	): CustomEntityBase = withDefinitionContext(entityType) {
		dao.create(getInstanceAndGroup(), validateAndMapForCreation(entity))
	}


	override suspend fun getCustomEntity(
		entityType: String,
		id: String,
	): CustomEntityBase? =
		dao.get(getInstanceAndGroup(), id)?.also {
			require (it.entityTypeId == entityType) {
				"Entity ${it.id} is not of expected type."
			}
		}

	override fun getCustomEntities(
		entityType: String,
		ids: List<String>,
	): Flow<CustomEntityBase> = flow {
		dao.getEntities(getInstanceAndGroup(), ids).collect {
			require(it.entityTypeId == entityType) {
				"Entity ${it.id} is not of expected type."
			}
			emit(it)
		}
	}

	override suspend fun modifyCustomEntity(
		entityType: String,
		entity: CustomEntityBase,
	): CustomEntityBase = withDefinitionContext(
		entityType
	) {
		val datastoreInfo = getInstanceAndGroup()
		checkValidModification(
			currentEntityStub = dao.getCustomEntityMetadataStub(datastoreInfo, entity.id)
				?: throw NotFoundRequestException("Entity ${entity.id} does not exist"),
			updatedEntity = entity
		)
		dao.save(getInstanceAndGroup(), entity)
	}

	override fun modifyCustomEntities(
		entityType: String,
		entities: List<CustomEntityBase>,
	): Flow<CustomEntityBase> = flowWithDefinitionContext(entityType) {
		val datastoreInfo = getInstanceAndGroup()
		dao.saveBulk(
			datastoreInfo,
			filterValidModifications(
				currentEntitiesStubs = dao.getCustomEntitiesMetadataStubs(
					datastoreInfo,
					entities.map { it.id }
				).toList(),
				updatedEntities = entities
			).toList()
		).filterSuccessfulUpdates()
	}
}