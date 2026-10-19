package org.taktik.icure.asynclogic.impl

import com.fasterxml.jackson.databind.JsonMappingException
import com.icure.cardinal.customentities.util.CachedCustomEntitiesConfigurationProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import org.springframework.context.annotation.Profile
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asyncdao.CustomEntityDAO
import org.taktik.icure.asyncdao.getEntitiesWithExpectedRev
import org.taktik.icure.asyncdao.results.filterSuccessfulUpdates
import org.taktik.icure.asynclogic.CustomEntityLogic
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.impl.customentities.CustomEntityDefinitionLogicContext
import org.taktik.icure.asynclogic.impl.customentities.CustomEntityDefinitionLogicContextFactory
import org.taktik.icure.asynclogic.impl.customentities.CustomEntityLogicContextHelper
import org.taktik.icure.asynclogic.objectstorage.DataAttachmentChange
import org.taktik.icure.asynclogic.objectstorage.contentFlowOfNullable
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.CustomEntityBase
import org.taktik.icure.entities.requests.BulkShareOrUpdateMetadataParams
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.utils.toMap


@Service
@Profile("app")
class CustomEntityLogicImpl(
	val dao: CustomEntityDAO,
	val datastoreInstanceProvider: DatastoreInstanceProvider,
	val customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
	val customEntityContextFactory: CustomEntityDefinitionLogicContextFactory,
	val exchangeDataMapLogic: ExchangeDataMapLogic
) : CustomEntityLogic {
	private suspend fun getInstanceAndGroup(): IDatastoreInformation = datastoreInstanceProvider.getInstanceAndGroup()

	/*
	 * This logic may be used directly, without passing through the controller or service: all inputs, including the
	 * entity type, must be validated here.
	 *
	 * Handling of entities of the wrong type:
	 * - Single methods taking only an id fail with an error if the entity is of the wrong type.
	 * - Bulk methods taking only ids ignore entities of the wrong type, as if they did not exist (same behaviour as
	 *   bulk methods of builtin entities).
	 * - Methods taking the full entity always fail if the stored entity is of the wrong type, regardless of single/bulk.
	 * Errors never reveal the actual type of the entity.
	 */

	private fun requireExpectedType(entity: CustomEntityBase, entityType: String) {
		require(entity.entityTypeId == entityType) { wrongTypeMessage(entity.id) }
	}

	private fun wrongTypeMessage(id: String) = "Entity $id is not of expected type."

	/**
	 * Get a full entity by id, failing if it is of the wrong type. This includes builtin entities: the dao would fail
	 * with a deserialization exception which reveals implementation details (and possibly the actual type).
	 */
	private suspend fun doGetCustomEntity(
		datastoreInfo: IDatastoreInformation,
		entityType: String,
		id: String,
	): CustomEntityBase? = try {
		dao.get(datastoreInfo, id)
	} catch (e: JsonMappingException) {
		// Avoid leaking information about the actual type of the entity.
		// Depending on the builtin entity, deserialization may fail for different reasons (missing required properties,
		// mismatching java_type, ...): use the stubs view, which only includes custom entities, to verify if the entity
		// is not a custom entity, or if it is a custom entity that can't be deserialized.
		if (dao.getCustomEntityMetadataStub(datastoreInfo, id) == null) throw IllegalArgumentException(wrongTypeMessage(id)) else throw e
	}?.also { requireExpectedType(it, entityType) }

	private suspend fun doGetCustomEntityWithExpectedRev(
		datastoreInfo: IDatastoreInformation,
		entityType: String,
		id: String,
		rev: String?,
	): CustomEntityBase {
		val retrieved = doGetCustomEntity(datastoreInfo, entityType, id)
			?: throw NotFoundRequestException("Entity with id $id not found")
		if (rev != null && retrieved.rev != rev) {
			throw ConflictRequestException("Revision does not match for entity with id $id")
		}
		return retrieved
	}

	private suspend inline fun <T> withDefinitionContext(
		entityType: String,
		entities: List<CustomEntityBase>,
		block: suspend CustomEntityDefinitionLogicContext.() -> T,
	): T {
		entities.forEach { requireExpectedType(it, entityType) }
		return withDefinitionContext(entityType, block)
	}

	private suspend inline fun <T> withDefinitionContext(
		entityType: String,
		entity: CustomEntityBase,
		block: suspend CustomEntityDefinitionLogicContext.() -> T,
	): T {
		requireExpectedType(entity, entityType)
		return withDefinitionContext(entityType, block)
	}

	private suspend inline fun <T> withDefinitionContext(
		entityType: String,
		block: suspend CustomEntityDefinitionLogicContext.() -> T,
	): T = customEntityContextFactory.getContextFor(getDefinition(entityType)).block()

	/**
	 * Get the definition of the custom entity type, failing if it doesn't exist or is disabled (consistently with
	 * the controller, a disabled custom entity is treated as non-existing).
	 */
	private suspend fun getDefinition(entityType: String) =
		customEntitiesConfigurationProvider.getConfigForCurrentUser()?.customEntities[entityType]?.takeIf { !it.disabled }
			?: throw NotFoundRequestException("No custom entity config found for $entityType")

	private inline fun <T> flowWithDefinitionContext(
		entityType: String,
		crossinline block: suspend CustomEntityDefinitionLogicContext.() -> Flow<T>,
	): Flow<T> = flow {
		withDefinitionContext(entityType) {
			emitAll(block())
		}
	}

	private inline fun <T> flowWithDefinitionContext(
		entityType: String,
		entities: List<CustomEntityBase>,
		crossinline block: suspend CustomEntityDefinitionLogicContext.() -> Flow<T>,
	): Flow<T> = flow {
		withDefinitionContext(entityType, entities) {
			emitAll(block())
		}
	}

	override suspend fun createCustomEntity(
		entityType: String,
		entity: CustomEntityBase,
	): CustomEntityBase = withDefinitionContext(entityType, entity) {
		dao.create(getInstanceAndGroup(), validateAndMapForCreation(entity))
	}

	override fun createCustomEntities(
		entityType: String,
		entities: List<CustomEntityBase>,
	): Flow<CustomEntityBase> = flowWithDefinitionContext(entityType, entities) {
		dao.createBulk(getInstanceAndGroup(), entities.map { validateAndMapForCreation(it) }).filterSuccessfulUpdates()
	}


	override suspend fun getCustomEntity(
		entityType: String,
		id: String,
	): CustomEntityBase? {
		getDefinition(entityType)
		return doGetCustomEntity(getInstanceAndGroup(), entityType, id)
	}

	override fun getCustomEntities(
		entityType: String,
		ids: List<String>,
	): Flow<CustomEntityBase> = flow {
		getDefinition(entityType)
		emitAll(dao.getEntities(getInstanceAndGroup(), ids).filter { it.entityTypeId == entityType })
	}

	override suspend fun getCustomEntityMetadataStub(
		entityType: String,
		id: String,
	): CustomEntityBase? {
		getDefinition(entityType)
		return doGetCustomEntityMetadataStub(getInstanceAndGroup(), entityType, id)
	}

	private suspend fun doGetCustomEntityMetadataStub(
		datastoreInfo: IDatastoreInformation,
		entityType: String,
		id: String,
	): CustomEntityBase? =
		// No need to handle builtin entities: the stubs view only includes custom entities
		dao.getCustomEntityMetadataStub(
			datastoreInfo,
			id
		)?.also { requireExpectedType(it, entityType) }

	override fun getCustomEntitiesMetadataStubStrict(
		entityType: String,
		ids: List<String>,
	): Flow<CustomEntityBase> = flow {
		getDefinition(entityType)
		emitAll(doGetCustomEntitiesMetadataStubsStrict(getInstanceAndGroup(), entityType, ids))
	}

	override fun getCustomEntitiesMetadataStubLenient(
		entityType: String,
		ids: List<String>,
	): Flow<CustomEntityBase> = flow {
		getDefinition(entityType)
		emitAll(doGetCustomEntitiesMetadataStubsLenient(getInstanceAndGroup(), entityType, ids))
	}

	private fun doGetCustomEntitiesMetadataStubsStrict(
		datastoreInfo: IDatastoreInformation,
		entityType: String,
		ids: List<String>,
	): Flow<CustomEntityBase> =
		// No need to handle builtin entities: the stubs view only includes custom entities
		dao.getCustomEntitiesMetadataStubs(
			datastoreInfo,
			ids
		).onEach { requireExpectedType(it, entityType) }

	private fun doGetCustomEntitiesMetadataStubsLenient(
		datastoreInfo: IDatastoreInformation,
		entityType: String,
		ids: List<String>,
	): Flow<CustomEntityBase> =
		dao.getCustomEntitiesMetadataStubs(
			datastoreInfo,
			ids
		).filter { it.entityTypeId == entityType }

	override suspend fun modifyCustomEntity(
		entityType: String,
		entity: CustomEntityBase,
	): CustomEntityBase = withDefinitionContext(
		entityType
	) {
		val datastoreInfo = getInstanceAndGroup()
		dao.save(
			datastoreInfo,
			checkAndMapValidModification(
				currentEntityStub = doGetCustomEntityMetadataStub(
					datastoreInfo,
					entityType,
					entity.id
				) ?: throw NotFoundRequestException("Entity ${entity.id} does not exist"),
				updatedEntity = entity
			)
		)
	}

	override fun modifyCustomEntities(
		entityType: String,
		entities: List<CustomEntityBase>,
	): Flow<CustomEntityBase> = flowWithDefinitionContext(entityType, entities) {
		val datastoreInfo = getInstanceAndGroup()
		dao.saveBulk(
			datastoreInfo,
			filterAndMapValidModifications(
				// Full entities in input: fail if any of them is stored with a different type
				currentEntitiesStubs = doGetCustomEntitiesMetadataStubsStrict(
					datastoreInfo,
					entityType,
					entities.map { it.id }
				).toList(),
				updatedEntities = entities
			).toList()
		).filterSuccessfulUpdates()
	}

	override suspend fun deleteCustomEntity(
		entityType: String,
		id: String,
		rev: String?,
	): CustomEntityBase {
		getDefinition(entityType)
		val datastoreInfo = getInstanceAndGroup()
		return dao.remove(datastoreInfo, doGetCustomEntityWithExpectedRev(datastoreInfo, entityType, id, rev))
	}

	override fun deleteCustomEntities(
		entityType: String,
		identifiers: Collection<IdAndRev>,
	): Flow<CustomEntityBase> = flow {
		getDefinition(entityType)
		val datastoreInfo = getInstanceAndGroup()
		val entities = dao.getEntitiesWithExpectedRev(datastoreInfo, identifiers).filter { it.entityTypeId == entityType }
		emitAll(dao.remove(datastoreInfo, entities).filterSuccessfulUpdates())
	}

	override suspend fun undeleteCustomEntity(
		entityType: String,
		id: String,
		rev: String?,
	): CustomEntityBase {
		getDefinition(entityType)
		val datastoreInfo = getInstanceAndGroup()
		return dao.unRemove(datastoreInfo, doGetCustomEntityWithExpectedRev(datastoreInfo, entityType, id, rev))
	}

	override fun undeleteCustomEntities(
		entityType: String,
		identifiers: Collection<IdAndRev>,
	): Flow<CustomEntityBase> = flow {
		getDefinition(entityType)
		val datastoreInfo = getInstanceAndGroup()
		val entities = dao.getEntitiesWithExpectedRev(datastoreInfo, identifiers).filter { it.entityTypeId == entityType }
		emitAll(dao.unRemove(datastoreInfo, entities).filterSuccessfulUpdates())
	}

	override suspend fun purgeCustomEntity(
		entityType: String,
		id: String,
		rev: String,
	): DocIdentifier = withDefinitionContext(entityType) {
		val datastoreInfo = getInstanceAndGroup()
		val entity = doGetCustomEntityMetadataStub(datastoreInfo, entityType, id)?.also {
			if (it.rev != rev) throw ConflictRequestException("Revision does not match for entity with id $id")
		} ?: throw NotFoundRequestException("Entity with id $id not found")
		dao.purge(datastoreInfo, entity).also { cleanupPurgedEntity(entity) }
	}

	override fun purgeCustomEntities(
		entityType: String,
		identifiers: Collection<IdAndRev>,
	): Flow<DocIdentifier> = flowWithDefinitionContext(entityType) {
		require(identifiers.mapTo(mutableSetOf()) { it.id }.size == identifiers.size) {
			"Duplicate identifiers provided"
		}
		val datastoreInfo = getInstanceAndGroup()
		val expectedRevById = identifiers.associate { it.id to it.rev }
		val entitiesById = doGetCustomEntitiesMetadataStubsLenient(
			datastoreInfo,
			entityType,
			identifiers.map { it.id }
		).filter {
			it.rev == expectedRevById[it.id]
		}.map {
			it.id to it
		}.toMap()
		dao.purge(datastoreInfo, entitiesById.values).filterSuccessfulUpdates().onEach {
			cleanupPurgedEntity(checkNotNull(entitiesById[it.id]))
		}
	}

	override fun bulkShareOrUpdateMetadata(
		entityType: String,
		requests: BulkShareOrUpdateMetadataParams
	): Flow<EntityBulkShareResult<CustomEntityBase>> = flowWithDefinitionContext(entityType) {
		val helper = requireHelper(CustomEntityLogicContextHelper.EncryptionMetadata)
		val datastoreInfo = datastoreInstanceProvider.getInstanceAndGroup()
		helper.doBulkShareOrUpdateMetadata(
			requests = requests,
			entities = dao.getEntities(datastoreInfo, requests.requestsByEntityId.keys).filter {
				it.entityTypeId == entityType
			}.toList().associateBy { it.id },
			doSaveBulk = { dao.saveBulk(datastoreInfo, it) },
			doCreateExchangeDataMapById = { exchangeDataMapLogic.createOrUpdateExchangeDataMapBatchById(it) }
		)
	}

	override suspend fun getAttachment(
		entityType: String,
		entity: CustomEntityBase,
		attachmentKey: String,
	): Flow<DataBuffer> = withDefinitionContext(entityType, entity) {
		// Helper only available if attachments are supported and enabled
		requireHelper(CustomEntityLogicContextHelper.AttachmentLoader).contentFlowOfNullable(entity, attachmentKey)
			?: throw NotFoundRequestException("Entity ${entity.id} has no attachment with key $attachmentKey")
	}

	override suspend fun updateAttachments(
		entityType: String,
		entityId: String,
		entityRev: String,
		changes: Map<String, DataAttachmentChange>,
	): CustomEntityBase? = withDefinitionContext(entityType) {
		// Helper only available if attachments are supported and enabled
		val helper = requireHelper(CustomEntityLogicContextHelper.AttachmentModification)
		// Ensures the entity exists and is of the expected type, revision is checked by the helper.
		// Loading the full entity, since the helper will need it as well (may be served by the cache)
		doGetCustomEntity(getInstanceAndGroup(), entityType, entityId)
			?: throw NotFoundRequestException("Entity $entityId does not exist")
		helper.updateAttachments(entityId, entityRev, changes)
	}
}
