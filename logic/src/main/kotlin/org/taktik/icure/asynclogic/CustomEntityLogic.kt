package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.springframework.core.io.buffer.DataBuffer
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.asynclogic.objectstorage.DataAttachmentChange
import org.taktik.icure.entities.CustomEntityBase
import org.taktik.icure.entities.requests.BulkShareOrUpdateMetadataParams
import org.taktik.icure.entities.requests.EntityBulkShareResult

interface CustomEntityLogic {
	// region Common

	suspend fun createCustomEntity(entityType: String, entity: CustomEntityBase): CustomEntityBase
	fun createCustomEntities(entityType: String, entities: List<CustomEntityBase>): Flow<CustomEntityBase>

	suspend fun getCustomEntity(entityType: String, id: String): CustomEntityBase?
	fun getCustomEntities(entityType: String, ids: List<String>): Flow<CustomEntityBase>

	suspend fun getCustomEntityMetadataStub(entityType: String, id: String): CustomEntityBase?
	/**
	 * Strict bulk version of [getCustomEntityMetadataStub]: fails if any of the requested ids belongs to a custom
	 * entity of a different type.
	 * Use when the ids come from full entities provided by the user (e.g. to verify a bulk modification), where
	 * putting an entity in the wrong type is an error.
	 *
	 * Note: ids of builtin entities can't be distinguished from non-existing entities, and are ignored. Since the
	 * modification methods only apply changes to entities with an existing stub, builtin entities are never modified
	 * through custom entity methods: they are ignored by bulk methods, and not found by single methods.
	 */
	fun getCustomEntitiesMetadataStubStrict(entityType: String, ids: List<String>): Flow<CustomEntityBase>

	/**
	 * Lenient bulk version of [getCustomEntityMetadataStub]: entities of a different type (custom or builtin) are
	 * ignored, as if they did not exist.
	 * Use when only the ids are known (e.g. bulk delete/purge), since an id alone doesn't tell what type of entity it
	 * represents.
	 */
	fun getCustomEntitiesMetadataStubLenient(entityType: String, ids: List<String>): Flow<CustomEntityBase>

	suspend fun modifyCustomEntity(entityType: String, entity: CustomEntityBase): CustomEntityBase
	fun modifyCustomEntities(entityType: String, entities: List<CustomEntityBase>): Flow<CustomEntityBase>

	suspend fun deleteCustomEntity(entityType: String, id: String, rev: String?): CustomEntityBase
	fun deleteCustomEntities(entityType: String, identifiers: Collection<IdAndRev>): Flow<CustomEntityBase>

	suspend fun undeleteCustomEntity(entityType: String, id: String, rev: String?): CustomEntityBase
	fun undeleteCustomEntities(entityType: String, identifiers: Collection<IdAndRev>): Flow<CustomEntityBase>

	suspend fun purgeCustomEntity(entityType: String, id: String, rev: String): DocIdentifier
	fun purgeCustomEntities(entityType: String, identifiers: Collection<IdAndRev>): Flow<DocIdentifier>

	// endregion

	// region Encryptable

	/**
	 * Equivalent to [EntityWithSecureDelegationsLogic.bulkShareOrUpdateMetadata]
	 */
	fun bulkShareOrUpdateMetadata(
		entityType: String,
		requests: BulkShareOrUpdateMetadataParams,
	): Flow<EntityBulkShareResult<CustomEntityBase>>

	// endregion

	// region Attachments

	/**
	 * Get the content of the data attachment with key [attachmentKey] of a custom entity.
	 * @param entity the entity owning the attachment, can be a metadata stub.
	 * @throws org.taktik.icure.exceptions.NotFoundRequestException if the attachment doesn't exist.
	 */
	suspend fun getAttachment(
		entityType: String,
		entity: CustomEntityBase,
		attachmentKey: String
	): Flow<DataBuffer>

	/**
	 * Equivalent to [org.taktik.icure.asynclogic.objectstorage.DataAttachmentModificationLogic.updateAttachments]
	 */
	suspend fun updateAttachments(
		entityType: String,
		entityId: String,
		entityRev: String,
		changes: Map<String, DataAttachmentChange>,
	): CustomEntityBase?

	// endregion
}