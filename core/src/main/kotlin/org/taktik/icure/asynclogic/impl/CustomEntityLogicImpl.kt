package org.taktik.icure.asynclogic.impl

import com.fasterxml.jackson.databind.JsonMappingException
import com.icure.cardinal.customentities.config.VersionedCustomEntitiesConfiguration
import com.icure.cardinal.customentities.util.CachedCustomEntitiesConfigurationProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import org.springframework.core.io.buffer.DataBuffer
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
import org.taktik.icure.entities.ExchangeDataMap
import org.taktik.icure.entities.requests.BulkShareOrUpdateMetadataParams
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.entities.utils.Base64String
import org.taktik.icure.entities.utils.HexString
import org.taktik.icure.entities.utils.KeypairFingerprintV2String
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.utils.toMap


open class CustomEntityLogicImpl(
	val dao: CustomEntityDAO,
	val datastoreInstanceProvider: DatastoreInstanceProvider,
	val customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
	val customEntityContextFactory: CustomEntityDefinitionLogicContextFactory,
	val exchangeDataMapLogic: ExchangeDataMapLogic
) : CustomEntityLogic {
	/**
	 * Where the operations of the logic are applied: the datastore, and the custom entities configuration and exchange
	 * data maps that go with it.
	 * The public methods of this logic always apply to the datastore of the current user, subclasses may use the
	 * protected methods to apply the same operations elsewhere (e.g. in a different group).
	 */
	protected interface Target {
		suspend fun getDatastoreInformation(): IDatastoreInformation
		suspend fun getCustomEntitiesConfiguration(): VersionedCustomEntitiesConfiguration?
		fun createOrUpdateExchangeDataMapBatchById(
			batch: Map<HexString, Map<KeypairFingerprintV2String, Base64String>>
		): Flow<ExchangeDataMap>
	}

	private val currentUserTarget = object : Target {
		override suspend fun getDatastoreInformation(): IDatastoreInformation =
			datastoreInstanceProvider.getInstanceAndGroup()

		override suspend fun getCustomEntitiesConfiguration(): VersionedCustomEntitiesConfiguration? =
			customEntitiesConfigurationProvider.getConfigForCurrentUser()

		override fun createOrUpdateExchangeDataMapBatchById(
			batch: Map<HexString, Map<KeypairFingerprintV2String, Base64String>>
		): Flow<ExchangeDataMap> = exchangeDataMapLogic.createOrUpdateExchangeDataMapBatchById(batch)
	}

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
	private suspend fun loadCustomEntity(
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

	private suspend fun loadCustomEntityWithExpectedRev(
		datastoreInfo: IDatastoreInformation,
		entityType: String,
		id: String,
		rev: String?,
	): CustomEntityBase {
		val retrieved = loadCustomEntity(datastoreInfo, entityType, id)
			?: throw NotFoundRequestException("Entity with id $id not found")
		if (rev != null && retrieved.rev != rev) {
			throw ConflictRequestException("Revision does not match for entity with id $id")
		}
		return retrieved
	}

	private suspend inline fun <T> withDefinitionContext(
		target: Target,
		entityType: String,
		entities: List<CustomEntityBase>,
		block: suspend CustomEntityDefinitionLogicContext.() -> T,
	): T {
		entities.forEach { requireExpectedType(it, entityType) }
		return withDefinitionContext(target, entityType, block)
	}

	private suspend inline fun <T> withDefinitionContext(
		target: Target,
		entityType: String,
		entity: CustomEntityBase,
		block: suspend CustomEntityDefinitionLogicContext.() -> T,
	): T {
		requireExpectedType(entity, entityType)
		return withDefinitionContext(target, entityType, block)
	}

	private suspend inline fun <T> withDefinitionContext(
		target: Target,
		entityType: String,
		block: suspend CustomEntityDefinitionLogicContext.() -> T,
	): T = customEntityContextFactory.getContextFor(getDefinition(target, entityType)).block()

	/**
	 * Get the definition of the custom entity type in the configuration of the target, failing if it doesn't exist or
	 * is disabled (consistently with the controller, a disabled custom entity is treated as non-existing).
	 */
	private suspend fun getDefinition(target: Target, entityType: String) =
		target.getCustomEntitiesConfiguration()?.customEntities[entityType]?.takeIf { !it.disabled }
			?: throw NotFoundRequestException("No custom entity config found for $entityType")

	private inline fun <T> flowWithDefinitionContext(
		target: Target,
		entityType: String,
		crossinline block: suspend CustomEntityDefinitionLogicContext.() -> Flow<T>,
	): Flow<T> = flow {
		withDefinitionContext(target, entityType) {
			emitAll(block())
		}
	}

	private inline fun <T> flowWithDefinitionContext(
		target: Target,
		entityType: String,
		entities: List<CustomEntityBase>,
		crossinline block: suspend CustomEntityDefinitionLogicContext.() -> Flow<T>,
	): Flow<T> = flow {
		withDefinitionContext(target, entityType, entities) {
			emitAll(block())
		}
	}

	// region Current user

	override suspend fun createCustomEntity(
		entityType: String,
		entity: CustomEntityBase,
	): CustomEntityBase = doCreateCustomEntity(currentUserTarget, entityType, entity)

	override fun createCustomEntities(
		entityType: String,
		entities: List<CustomEntityBase>,
	): Flow<CustomEntityBase> = doCreateCustomEntities(currentUserTarget, entityType, entities)

	override suspend fun getCustomEntity(
		entityType: String,
		id: String,
	): CustomEntityBase? = doGetCustomEntity(currentUserTarget, entityType, id)

	override fun getCustomEntities(
		entityType: String,
		ids: List<String>,
	): Flow<CustomEntityBase> = doGetCustomEntities(currentUserTarget, entityType, ids)

	override suspend fun getCustomEntityMetadataStub(
		entityType: String,
		id: String,
	): CustomEntityBase? = doGetCustomEntityMetadataStub(currentUserTarget, entityType, id)

	override fun getCustomEntitiesMetadataStubStrict(
		entityType: String,
		ids: List<String>,
	): Flow<CustomEntityBase> = doGetCustomEntitiesMetadataStubStrict(currentUserTarget, entityType, ids)

	override fun getCustomEntitiesMetadataStubLenient(
		entityType: String,
		ids: List<String>,
	): Flow<CustomEntityBase> = doGetCustomEntitiesMetadataStubLenient(currentUserTarget, entityType, ids)

	override suspend fun modifyCustomEntity(
		entityType: String,
		entity: CustomEntityBase,
	): CustomEntityBase = doModifyCustomEntity(currentUserTarget, entityType, entity)

	override fun modifyCustomEntities(
		entityType: String,
		entities: List<CustomEntityBase>,
	): Flow<CustomEntityBase> = doModifyCustomEntities(currentUserTarget, entityType, entities)

	override suspend fun deleteCustomEntity(
		entityType: String,
		id: String,
		rev: String?,
	): CustomEntityBase = doDeleteCustomEntity(currentUserTarget, entityType, id, rev)

	override fun deleteCustomEntities(
		entityType: String,
		identifiers: Collection<IdAndRev>,
	): Flow<CustomEntityBase> = doDeleteCustomEntities(currentUserTarget, entityType, identifiers)

	override suspend fun undeleteCustomEntity(
		entityType: String,
		id: String,
		rev: String?,
	): CustomEntityBase = doUndeleteCustomEntity(currentUserTarget, entityType, id, rev)

	override fun undeleteCustomEntities(
		entityType: String,
		identifiers: Collection<IdAndRev>,
	): Flow<CustomEntityBase> = doUndeleteCustomEntities(currentUserTarget, entityType, identifiers)

	override suspend fun purgeCustomEntity(
		entityType: String,
		id: String,
		rev: String,
	): DocIdentifier = doPurgeCustomEntity(currentUserTarget, entityType, id, rev)

	override fun purgeCustomEntities(
		entityType: String,
		identifiers: Collection<IdAndRev>,
	): Flow<DocIdentifier> = doPurgeCustomEntities(currentUserTarget, entityType, identifiers)

	override fun bulkShareOrUpdateMetadata(
		entityType: String,
		requests: BulkShareOrUpdateMetadataParams
	): Flow<EntityBulkShareResult<CustomEntityBase>> = doBulkShareOrUpdateMetadata(currentUserTarget, entityType, requests)

	override suspend fun getAttachment(
		entityType: String,
		entity: CustomEntityBase,
		attachmentKey: String,
	): Flow<DataBuffer> = withDefinitionContext(currentUserTarget, entityType, entity) {
		// Helper only available if attachments are supported and enabled
		requireHelper(CustomEntityLogicContextHelper.AttachmentLoader).contentFlowOfNullable(entity, attachmentKey)
			?: throw NotFoundRequestException("Entity ${entity.id} has no attachment with key $attachmentKey")
	}

	override suspend fun updateAttachments(
		entityType: String,
		entityId: String,
		entityRev: String,
		changes: Map<String, DataAttachmentChange>,
	): CustomEntityBase? = withDefinitionContext(currentUserTarget, entityType) {
		// Helper only available if attachments are supported and enabled
		val helper = requireHelper(CustomEntityLogicContextHelper.AttachmentModification)
		// Ensures the entity exists and is of the expected type, revision is checked by the helper.
		// Loading the full entity, since the helper will need it as well (may be served by the cache)
		loadCustomEntity(currentUserTarget.getDatastoreInformation(), entityType, entityId)
			?: throw NotFoundRequestException("Entity $entityId does not exist")
		helper.updateAttachments(entityId, entityRev, changes)
	}

	// endregion

	// region Any target

	protected suspend fun doCreateCustomEntity(
		target: Target,
		entityType: String,
		entity: CustomEntityBase,
	): CustomEntityBase = withDefinitionContext(target, entityType, entity) {
		dao.create(target.getDatastoreInformation(), validateAndMapForCreation(entity))
	}

	protected fun doCreateCustomEntities(
		target: Target,
		entityType: String,
		entities: List<CustomEntityBase>,
	): Flow<CustomEntityBase> = flowWithDefinitionContext(target, entityType, entities) {
		dao.createBulk(target.getDatastoreInformation(), entities.map { validateAndMapForCreation(it) }).filterSuccessfulUpdates()
	}

	protected suspend fun doGetCustomEntity(
		target: Target,
		entityType: String,
		id: String,
	): CustomEntityBase? {
		getDefinition(target, entityType)
		return loadCustomEntity(target.getDatastoreInformation(), entityType, id)
	}

	protected fun doGetCustomEntities(
		target: Target,
		entityType: String,
		ids: List<String>,
	): Flow<CustomEntityBase> = flow {
		getDefinition(target, entityType)
		emitAll(dao.getEntities(target.getDatastoreInformation(), ids).filter { it.entityTypeId == entityType })
	}

	protected suspend fun doGetCustomEntityMetadataStub(
		target: Target,
		entityType: String,
		id: String,
	): CustomEntityBase? {
		getDefinition(target, entityType)
		return loadCustomEntityMetadataStub(target.getDatastoreInformation(), entityType, id)
	}

	private suspend fun loadCustomEntityMetadataStub(
		datastoreInfo: IDatastoreInformation,
		entityType: String,
		id: String,
	): CustomEntityBase? =
		// No need to handle builtin entities: the stubs view only includes custom entities
		dao.getCustomEntityMetadataStub(
			datastoreInfo,
			id
		)?.also { requireExpectedType(it, entityType) }

	protected fun doGetCustomEntitiesMetadataStubStrict(
		target: Target,
		entityType: String,
		ids: List<String>,
	): Flow<CustomEntityBase> = flow {
		getDefinition(target, entityType)
		emitAll(loadCustomEntitiesMetadataStubsStrict(target.getDatastoreInformation(), entityType, ids))
	}

	protected fun doGetCustomEntitiesMetadataStubLenient(
		target: Target,
		entityType: String,
		ids: List<String>,
	): Flow<CustomEntityBase> = flow {
		getDefinition(target, entityType)
		emitAll(loadCustomEntitiesMetadataStubsLenient(target.getDatastoreInformation(), entityType, ids))
	}

	private fun loadCustomEntitiesMetadataStubsStrict(
		datastoreInfo: IDatastoreInformation,
		entityType: String,
		ids: List<String>,
	): Flow<CustomEntityBase> =
		// No need to handle builtin entities: the stubs view only includes custom entities
		dao.getCustomEntitiesMetadataStubs(
			datastoreInfo,
			ids
		).onEach { requireExpectedType(it, entityType) }

	private fun loadCustomEntitiesMetadataStubsLenient(
		datastoreInfo: IDatastoreInformation,
		entityType: String,
		ids: List<String>,
	): Flow<CustomEntityBase> =
		dao.getCustomEntitiesMetadataStubs(
			datastoreInfo,
			ids
		).filter { it.entityTypeId == entityType }

	protected suspend fun doModifyCustomEntity(
		target: Target,
		entityType: String,
		entity: CustomEntityBase,
	): CustomEntityBase = withDefinitionContext(
		target,
		entityType
	) {
		val datastoreInfo = target.getDatastoreInformation()
		dao.save(
			datastoreInfo,
			checkAndMapValidModification(
				currentEntityStub = loadCustomEntityMetadataStub(
					datastoreInfo,
					entityType,
					entity.id
				) ?: throw NotFoundRequestException("Entity ${entity.id} does not exist"),
				updatedEntity = entity
			)
		)
	}

	protected fun doModifyCustomEntities(
		target: Target,
		entityType: String,
		entities: List<CustomEntityBase>,
	): Flow<CustomEntityBase> = flowWithDefinitionContext(target, entityType, entities) {
		val datastoreInfo = target.getDatastoreInformation()
		dao.saveBulk(
			datastoreInfo,
			filterAndMapValidModifications(
				// Full entities in input: fail if any of them is stored with a different type
				currentEntitiesStubs = loadCustomEntitiesMetadataStubsStrict(
					datastoreInfo,
					entityType,
					entities.map { it.id }
				).toList(),
				updatedEntities = entities
			).toList()
		).filterSuccessfulUpdates()
	}

	protected suspend fun doDeleteCustomEntity(
		target: Target,
		entityType: String,
		id: String,
		rev: String?,
	): CustomEntityBase {
		getDefinition(target, entityType)
		val datastoreInfo = target.getDatastoreInformation()
		return dao.remove(datastoreInfo, loadCustomEntityWithExpectedRev(datastoreInfo, entityType, id, rev))
	}

	protected fun doDeleteCustomEntities(
		target: Target,
		entityType: String,
		identifiers: Collection<IdAndRev>,
	): Flow<CustomEntityBase> = flow {
		getDefinition(target, entityType)
		val datastoreInfo = target.getDatastoreInformation()
		val entities = dao.getEntitiesWithExpectedRev(datastoreInfo, identifiers).filter { it.entityTypeId == entityType }
		emitAll(dao.remove(datastoreInfo, entities).filterSuccessfulUpdates())
	}

	protected suspend fun doUndeleteCustomEntity(
		target: Target,
		entityType: String,
		id: String,
		rev: String?,
	): CustomEntityBase {
		getDefinition(target, entityType)
		val datastoreInfo = target.getDatastoreInformation()
		return dao.unRemove(datastoreInfo, loadCustomEntityWithExpectedRev(datastoreInfo, entityType, id, rev))
	}

	protected fun doUndeleteCustomEntities(
		target: Target,
		entityType: String,
		identifiers: Collection<IdAndRev>,
	): Flow<CustomEntityBase> = flow {
		getDefinition(target, entityType)
		val datastoreInfo = target.getDatastoreInformation()
		val entities = dao.getEntitiesWithExpectedRev(datastoreInfo, identifiers).filter { it.entityTypeId == entityType }
		emitAll(dao.unRemove(datastoreInfo, entities).filterSuccessfulUpdates())
	}

	protected suspend fun doPurgeCustomEntity(
		target: Target,
		entityType: String,
		id: String,
		rev: String,
	): DocIdentifier = withDefinitionContext(target, entityType) {
		val datastoreInfo = target.getDatastoreInformation()
		val entity = loadCustomEntityMetadataStub(datastoreInfo, entityType, id)?.also {
			if (it.rev != rev) throw ConflictRequestException("Revision does not match for entity with id $id")
		} ?: throw NotFoundRequestException("Entity with id $id not found")
		dao.purge(datastoreInfo, entity).also { cleanupPurgedEntity(entity) }
	}

	protected fun doPurgeCustomEntities(
		target: Target,
		entityType: String,
		identifiers: Collection<IdAndRev>,
	): Flow<DocIdentifier> = flowWithDefinitionContext(target, entityType) {
		require(identifiers.mapTo(mutableSetOf()) { it.id }.size == identifiers.size) {
			"Duplicate identifiers provided"
		}
		val datastoreInfo = target.getDatastoreInformation()
		val expectedRevById = identifiers.associate { it.id to it.rev }
		val entitiesById = loadCustomEntitiesMetadataStubsLenient(
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

	protected fun doBulkShareOrUpdateMetadata(
		target: Target,
		entityType: String,
		requests: BulkShareOrUpdateMetadataParams
	): Flow<EntityBulkShareResult<CustomEntityBase>> = flowWithDefinitionContext(target, entityType) {
		val helper = requireHelper(CustomEntityLogicContextHelper.EncryptionMetadata)
		val datastoreInfo = target.getDatastoreInformation()
		helper.doBulkShareOrUpdateMetadata(
			requests = requests,
			entities = dao.getEntities(datastoreInfo, requests.requestsByEntityId.keys).filter {
				it.entityTypeId == entityType
			}.toList().associateBy { it.id },
			doSaveBulk = { dao.saveBulk(datastoreInfo, it) },
			doCreateExchangeDataMapById = { target.createOrUpdateExchangeDataMapBatchById(it) }
		)
	}

	// endregion
}
