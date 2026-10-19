/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asyncservice.base.EntityWithConflictResolutionService
import org.taktik.icure.asyncservice.base.EntityWithFilterService
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.entities.RelatedPerson
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.NotFoundRequestException

interface RelatedPersonService :
	EntityWithSecureDelegationsService<RelatedPerson>,
	EntityWithConflictResolutionService<RelatedPerson>,
	EntityWithFilterService<RelatedPerson> {
	suspend fun createRelatedPerson(relatedPerson: RelatedPerson): RelatedPerson
	fun createRelatedPersons(relatedPersons: List<RelatedPerson>): Flow<RelatedPerson>

	suspend fun getRelatedPerson(relatedPersonId: String): RelatedPerson?
	fun getRelatedPersons(relatedPersonIds: Collection<String>): Flow<RelatedPerson>

	suspend fun modifyRelatedPerson(relatedPerson: RelatedPerson): RelatedPerson
	fun modifyRelatedPersons(relatedPersons: List<RelatedPerson>): Flow<RelatedPerson>

	/**
	 * Marks a batch of entities as deleted.
	 * The data of the entities is preserved, but they won't appear in most queries.
	 * Ignores entities that:
	 * - don't exist
	 * - the user can't delete due to limited lack of write access
	 * - don't match the provided revision (if provided)
	 *
	 * @param ids a [List] containing the ids and optionally the revisions of the entities to delete.
	 * @return a [Flow] containing the deleted [RelatedPerson]s.
	 */
	fun deleteRelatedPersons(ids: List<IdAndRev>): Flow<RelatedPerson>

	/**
	 * Marks an entity as deleted.
	 * The data of the entity is preserved, but the entity won't appear in most queries.
	 *
	 * @param id the id of the entity to delete.
	 * @param rev the latest rev of the entity to delete.
	 * @return the deleted [RelatedPerson].
	 * @throws AccessDeniedException if the current user doesn't have the permission to delete the entity.
	 * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
	 * @throws ConflictRequestException if the entity rev doesn't match.
	 */
	suspend fun deleteRelatedPerson(id: String, rev: String?): RelatedPerson

	/**
	 * Deletes an entity.
	 * An entity deleted this way can't be restored.
	 * To delete an entity this way, the user needs purge permission in addition to write access to the entity.
	 *
	 * @param id the id of the entity
	 * @param rev the latest known revision of the entity.
	 */
	suspend fun purgeRelatedPerson(id: String, rev: String): DocIdentifier
	fun purgeRelatedPersons(relatedPersonIds: List<IdAndRev>): Flow<DocIdentifier>

	/**
	 * Restores an entity marked as deleted.
	 * The user needs to have write access to the entity.
	 *
	 * @param id the id of the entity marked to restore.
	 * @param rev the latest revision of the entity.
	 */
	suspend fun undeleteRelatedPerson(id: String, rev: String): RelatedPerson
	fun undeleteRelatedPersons(relatedPersonIds: List<IdAndRev>): Flow<RelatedPerson>

	fun createEntities(entities: Flow<RelatedPerson>): Flow<RelatedPerson>
	fun modifyEntities(entities: Flow<RelatedPerson>): Flow<RelatedPerson>
}
