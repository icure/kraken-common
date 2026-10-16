package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.entities.CustomEntityBase
import org.taktik.icure.entities.requests.BulkShareOrUpdateMetadataParams
import org.taktik.icure.entities.requests.EntityBulkShareResult

interface CustomEntityLogic {
	suspend fun createCustomEntity(entityType: String, entity: CustomEntityBase): CustomEntityBase
	fun createCustomEntities(entityType: String, entities: List<CustomEntityBase>): Flow<CustomEntityBase>

	suspend fun getCustomEntity(entityType: String, id: String): CustomEntityBase?
	fun getCustomEntities(entityType: String, ids: List<String>): Flow<CustomEntityBase>

	suspend fun getCustomEntityMetadataStub(entityType: String, id: String): CustomEntityBase?
	fun getCustomEntitiesMetadataStub(entityType: String, ids: List<String>): Flow<CustomEntityBase>

	suspend fun modifyCustomEntity(entityType: String, entity: CustomEntityBase): CustomEntityBase
	fun modifyCustomEntities(entityType: String, entities: List<CustomEntityBase>): Flow<CustomEntityBase>

	suspend fun deleteCustomEntity(entityType: String, id: String, rev: String?): CustomEntityBase
	fun deleteCustomEntities(entityType: String, identifiers: Collection<IdAndRev>): Flow<CustomEntityBase>

	suspend fun undeleteCustomEntity(entityType: String, id: String, rev: String?): CustomEntityBase
	fun undeleteCustomEntities(entityType: String, identifiers: Collection<IdAndRev>): Flow<CustomEntityBase>

	suspend fun purgeCustomEntity(entityType: String, id: String, rev: String): DocIdentifier
	fun purgeCustomEntities(entityType: String, identifiers: Collection<IdAndRev>): Flow<DocIdentifier>

	/**
	 * Equivalent to [EntityWithSecureDelegationsLogic.bulkShareOrUpdateMetadata]
	 */
	fun bulkShareOrUpdateMetadata(
		entityType: String,
		requests: BulkShareOrUpdateMetadataParams,
	): Flow<EntityBulkShareResult<CustomEntityBase>>
}