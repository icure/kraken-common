/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.entities.EntityTemplate
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.NotFoundRequestException

interface EntityTemplateService {
	suspend fun createEntityTemplate(entityTemplate: EntityTemplate): EntityTemplate?

	suspend fun modifyEntityTemplate(entityTemplate: EntityTemplate): EntityTemplate?

	suspend fun getEntityTemplate(id: String): EntityTemplate?
	fun getEntityTemplates(selectedIds: Collection<String>): Flow<EntityTemplate>

	fun listEntityTemplatesBy(userId: String, entityType: String, searchString: String?, includeEntities: Boolean?): Flow<EntityTemplate>
	fun listEntityTemplatesBy(entityType: String, searchString: String?, includeEntities: Boolean?): Flow<EntityTemplate>
	fun listEntityTemplatesByKeyword(userId: String, entityType: String, keyword: String?, includeEntities: Boolean?): Flow<EntityTemplate>
	fun listEntityTemplatesByKeyword(entityType: String, keyword: String?, includeEntities: Boolean?): Flow<EntityTemplate>

	/**
	 * Modifies [EntityTemplate]s in batch.
	 *
	 * @param entities a [Collection] of modified [EntityTemplate]s.
	 * @return a [Flow] containing all the [EntityTemplate]s that were successfully modified.
	 */
	fun modifyEntityTemplates(entities: Collection<EntityTemplate>): Flow<EntityTemplate>

	/**
	 * Creates [EntityTemplate]s in batch.
	 *
	 * @param entities a [Collection] of [EntityTemplate]s to create.
	 * @return a [Flow] containing all the [EntityTemplate]s that were successfully created.
	 */
	fun createEntityTemplates(entities: Collection<EntityTemplate>): Flow<EntityTemplate>

    /**
     * Marks a batch of entities as deleted.
     * The data of the entities is preserved, but they won't appear in most queries.
     * Ignores entities that:
     * - don't exist
     * - the user can't delete due to limited lack of write access
     * - don't match the provided revision (if provided)
     *
     * @param ids a [List] containing the ids and optionally the revisions of the entities to delete.
     * @return a [Flow] containing the [DocIdentifier]s of the entities successfully deleted.
     */
    fun deleteEntityTemplates(ids: List<IdAndRev>): Flow<DocIdentifier>

    /**
     * Marks an entity as deleted.
     * The data of the entity is preserved, but the entity won't appear in most queries.
     *
     * @param id the id of the entity to delete.
     * @param rev
     * @return the updated [DocIdentifier] for the entity.
     * @throws AccessDeniedException if the current user doesn't have the permission to delete the entity.
     * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
     * @throws ConflictRequestException if the entity rev doesn't match.
     */
    suspend fun deleteEntityTemplate(id: String, rev: String?): DocIdentifier

    /**
     * Deletes an entity.
     * An entity deleted this way can't be restored.
     * To delete an entity this way, the user needs purge permission in addition to write access to the entity.
     *
     * @param id the id of the entity
     * @param rev the latest known revision of the entity.
     * @throws AccessDeniedException if the current user doesn't have the permission to purge the entity.
     * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
     * @throws ConflictRequestException if the entity rev doesn't match.
     */
    suspend fun purgeEntityTemplate(id: String, rev: String): DocIdentifier

    /**
     * Restores an entity marked as deleted.
     * The user needs to have write access to the entity
     * @param id the id of the entity marked to restore
     * @param rev the revision of the entity after it was marked as deleted
     * @return the restored entity
     */
    suspend fun undeleteEntityTemplate(id: String, rev: String): EntityTemplate
}
