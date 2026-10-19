package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.core.io.buffer.DataBuffer
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asynclogic.objectstorage.DataAttachmentChange
import org.taktik.icure.entities.CustomEntityBase
import org.taktik.icure.entities.requests.BulkShareOrUpdateMetadataParams
import org.taktik.icure.entities.requests.EntityBulkShareResult

interface CustomEntityService {
	suspend fun createCustomEntity(entityType: String, entity: CustomEntityBase): CustomEntityBase
	fun createCustomEntities(entityType: String, entities: List<CustomEntityBase>): Flow<CustomEntityBase>

	suspend fun getCustomEntity(entityType: String, id: String): CustomEntityBase?
	fun getCustomEntities(entityType: String, ids: List<String>): Flow<CustomEntityBase>

	suspend fun modifyCustomEntity(entityType: String, entity: CustomEntityBase): CustomEntityBase
	fun modifyCustomEntities(entityType: String, entities: List<CustomEntityBase>): Flow<CustomEntityBase>

	suspend fun deleteCustomEntity(entityType: String, id: String, rev: String?): CustomEntityBase
	fun deleteCustomEntities(entityType: String, identifiers: Collection<IdAndRev>): Flow<CustomEntityBase>

	suspend fun undeleteCustomEntity(entityType: String, id: String, rev: String?): CustomEntityBase
	fun undeleteCustomEntities(entityType: String, identifiers: Collection<IdAndRev>): Flow<CustomEntityBase>

	suspend fun purgeCustomEntity(entityType: String, id: String, rev: String): DocIdentifier
	fun purgeCustomEntities(entityType: String, identifiers: Collection<IdAndRev>): Flow<DocIdentifier>

	fun bulkShareOrUpdateMetadata(
		entityType: String,
		requests: BulkShareOrUpdateMetadataParams,
	): Flow<EntityBulkShareResult<CustomEntityBase>>

	/**
	 * Get the content of the data attachment with key [attachmentKey] of a custom entity.
	 * @return the entity owning the attachment (may be only a metadata stub) and the attachment content.
	 */
	suspend fun getAttachment(
		entityType: String,
		entityId: String,
		attachmentKey: String,
	): Pair<CustomEntityBase, Flow<DataBuffer>>

	suspend fun updateAttachments(
		entityType: String,
		entityId: String,
		entityRev: String,
		changes: Map<String, DataAttachmentChange>,
	): CustomEntityBase?
}
