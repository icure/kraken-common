/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asynclogic.PatientLogic.Companion.PatientSearchField
import org.taktik.icure.asyncservice.base.EntityWithConflictResolutionService
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.db.Sorting
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.MissingRequirementsException
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.pagination.PaginationElement
import java.time.Instant

interface PatientService :
	EntityWithSecureDelegationsService<Patient>,
	EntityWithConflictResolutionService {
	suspend fun countByHcParty(healthcarePartyId: String): Int
	fun listOfMergesAfter(date: Long?): Flow<Patient>

	/**
	 * Retrieves all the ids of the [Patient]s for a given healthcare party in a format for pagination.
	 *
	 * @param healthcarePartyId the id of the healthcare party.
	 * @param offset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [PaginationElement] containing the ids.
	 * @throws AccessDeniedException if the current user does not meet the precondition to find [Patient]s.
	 */
	fun findByHcPartyIdsOnly(healthcarePartyId: String, offset: PaginationOffset<ComplexKey>): Flow<PaginationElement>

	/**
	 * Retrieves all the [Patient]s for a healthcare party in a format for pagination.
	 * If [sorting] field is to [PatientSearchField.ssin], then [searchString] will be interpreted as a SSIN and all the patients with
	 * a matching [Patient.ssin] will be retrieved.
	 * If [sorting] field is equal to [PatientSearchField.dateOfBirth]`, then [searchString] will be interpreted as a fuzzy date and all the
	 * patients with a [Patient.dateOfBirth] equal or greater than [searchString] will be returned.
	 * Otherwise, a fuzzy search will be performed and all the patients with a normalized [Patient.lastName] + [Patient.firstName]
	 * that start with the normalized [searchString] will be returned.
	 * This method will filter out all the [Patient]s that the current user cannot access, but it will ensure that the
	 * page limit specified in the [offset] is reached as long as there are available elements.
	 *
	 * @param healthcarePartyId the healthcare party id.
	 * @param offset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @param searchString the value to search, it will be interpreted differently according to the [sorting] value.
	 * @param sorting a [Sorting] that specifies the interpretation of [searchString] and the sort order of the
	 * results (desc for descending).
	 * @return a [Flow] of [PaginationElement] containing the patients.
	 * @throws AccessDeniedException if the user does not meet the precondition to find [Patient]s.
	 */
	fun findByHcPartyAndSsinOrDateOfBirthOrNameContainsFuzzy(healthcarePartyId: String, offset: PaginationOffset<ComplexKey>, searchString: String?, sorting: Sorting<PatientSearchField>): Flow<PaginationElement>
	fun listPatients(paginationOffset: PaginationOffset<*>, filterChain: FilterChain<Patient>, sort: String?, desc: Boolean?): Flow<ViewQueryResultEvent>
	fun findByHcPartyNameContainsFuzzy(searchString: String?, healthcarePartyId: String, offset: PaginationOffset<ComplexKey>, descending: Boolean): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [Patient]s for which a healthcare party is responsible, i.e. is listed in
	 * [Patient.patientHealthCareParties], in a format for pagination.
	 * If [sorting] field is equal to [PatientSearchField.ssin], then [searchString] will be interpreted as a SSIN and all the patients with
	 * a matching [Patient.ssin] will be retrieved.
	 * If [sorting] field is equal to [PatientSearchField.dateOfBirth], then [searchString] will be interpreted as a fuzzy date and all the
	 * patients with a [Patient.dateOfBirth] equal or greater than [searchString] will be returned.
	 * Otherwise, a fuzzy search will be performed and all the patients with a normalized [Patient.lastName] + [Patient.firstName]
	 * that start with the normalized [searchString] will be returned.
	 * This method will filter out all the [Patient]s that the current user cannot access, but it will ensure that the
	 * page limit specified in the [offset] is reached as long as there are available elements.
	 *
	 * @param healthcarePartyId the healthcare party id.
	 * @param offset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @param searchString the value to search, it will be interpreted differently according to the [sorting] value.
	 * @param sorting a [Sorting] that specifies the interpretation of [searchString] and the sort order of the
	 * results (desc for descending).
	 * @return a [Flow] of [PaginationElement] containing the patients.
	 */
	fun findOfHcPartyAndSsinOrDateOfBirthOrNameContainsFuzzy(healthcarePartyId: String, offset: PaginationOffset<ComplexKey>, searchString: String?, sorting: Sorting<PatientSearchField>): Flow<PaginationElement>
	fun findByHcPartyAndSsin(ssin: String?, healthcarePartyId: String, paginationOffset: PaginationOffset<List<String>>): Flow<ViewQueryResultEvent>
	fun findByHcPartyDateOfBirth(date: Int?, healthcarePartyId: String, paginationOffset: PaginationOffset<List<String>>): Flow<ViewQueryResultEvent>

	suspend fun findByUserId(id: String): Patient?

	suspend fun getPatient(patientId: String): Patient?
	fun findByHcPartyAndIdentifier(healthcarePartyId: String, system: String, id: String): Flow<Patient>
	fun getPatients(patientIds: List<String>): Flow<Patient>

	suspend fun addDelegation(patientId: String, delegation: Delegation): Patient?

	suspend fun addDelegations(patientId: String, delegations: Collection<Delegation>): Patient?

	@Throws(MissingRequirementsException::class)
	suspend fun createPatient(patient: Patient): Patient?
	fun createPatients(patients: List<Patient>): Flow<Patient>

	suspend fun modifyPatient(patient: Patient): Patient?
	fun modifyPatients(patients: List<Patient>): Flow<Patient>

	suspend fun modifyPatientReferral(patient: Patient, referralId: String?, start: Instant?, end: Instant?): Patient?

	suspend fun getByExternalId(externalId: String): Patient?

	override fun solveConflicts(limit: Int?, ids: List<String>?): Flow<IdAndRev>

	@Deprecated(message = "A DataOwner may now have multiple AES Keys. Use getAesExchangeKeysForDelegate instead")
	suspend fun getHcPartyKeysForDelegate(healthcarePartyId: String): Map<String, String>

	suspend fun getAesExchangeKeysForDelegate(healthcarePartyId: String): Map<String, Map<String, Map<String, String>>>

	/**
	 * Returns all the [Patient]s where [Patient.modified] is after [date] in a format for pagination.
	 * This method will filter out all the [Patient]s that the current user cannot access, but it will ensure that the
	 * page limit specified in the [paginationOffset] is reached as long as there are available elements.
	 *
	 * @param date the lower bound for [Patient.modified] as timestamp.
	 * @param paginationOffset a [PaginationOffset] of [Long] for pagination.
	 * @return a [Flow] of [PaginationElement] containing the [Patient]s.
	 */
	fun listOfPatientsModifiedAfter(date: Long, paginationOffset: PaginationOffset<Long>): Flow<PaginationElement>

	/**
	 * Retrieves all the [Patient]s for a given healthcare party that appear multiple times in the `by_ssin` view in a
	 * format for pagination.
	 * This method will filter out all the [Patient]s that the current user cannot access, but it will ensure that the
	 * page limit specified in the [paginationOffset] is reached as long as there are available elements.
	 *
	 * @param healthcarePartyId the id of the delegated healthcare party.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] pf [PaginationElement] containing the [Patient]s.
	 * @throws AccessDeniedException if the user does not meet the precondition to find [Patient]s.
	 */
	fun getDuplicatePatientsBySsin(healthcarePartyId: String, paginationOffset: PaginationOffset<ComplexKey>): Flow<PaginationElement>

	/**
	 * Retrieves all the [Patient]s for a given healthcare party that appear multiple times in the `by_name` view in a
	 * format for pagination.
	 * This method will filter out all the [Patient]s that the current user cannot access, but it will ensure that the
	 * page limit specified in the [paginationOffset] is reached as long as there are available elements.
	 *
	 * @param healthcarePartyId the id of the delegated healthcare party.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] pf [PaginationElement] containing the [Patient]s.
	 * @throws AccessDeniedException if the user does not meet the precondition to find [Patient]s.
	 */
	fun getDuplicatePatientsByName(healthcarePartyId: String, paginationOffset: PaginationOffset<ComplexKey>): Flow<PaginationElement>
	fun fuzzySearchPatients(firstName: String?, lastName: String?, dateOfBirth: Int?, healthcarePartyId: String? = null): Flow<Patient>

	/**
	 * Marks a batch of entities as deleted.
	 * The data of the entities is preserved, but they won't appear in most queries.
	 * Ignores entities that:
	 * - don't exist
	 * - the user can't delete due to limited lack of write access
	 * - don't match the provided revision (if provided)
	 *
	 * @param ids a [List] containing the ids and optionally the revisions of the entities to delete.
	 * @return a [Flow] containing the deleted [Patient]s.
	 */
	fun deletePatients(ids: List<IdAndRev>): Flow<Patient>

	/**
	 * Marks an entity as deleted.
	 * The data of the entity is preserved, but the entity won't appear in most queries.
	 *
	 * @param id the id of the entity to delete.
	 * @param rev the latest rev of the entity to delete.
	 * @return the deleted [Patient].
	 * @throws AccessDeniedException if the current user doesn't have the permission to delete the entity.
	 * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
	 * @throws ConflictRequestException if the entity rev doesn't match.
	 */
	suspend fun deletePatient(id: String, rev: String?): Patient

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
	suspend fun purgePatient(id: String, rev: String): DocIdentifier

	/**
	 * Restores an entity marked as deleted.
	 * The user needs to have write access to the entity
	 * @param id the id of the entity marked to restore
	 * @param rev the revision of the entity after it was marked as deleted
	 * @return the restored entity
	 */
	suspend fun undeletePatient(id: String, rev: String): Patient

	/**
	 * Returns all the [Patient]s where [Patient.deletionDate] is after [start] and before [end], if provided, sorted
	 * by [Patient.deletionDate] and provided in a format for pagination.
	 * This method will filter out all the [Patient]s that the current user cannot access, but it will ensure that the
	 * page limit specified in the [paginationOffset] is reached as long as there are available elements.
	 *
	 * @param start the lower bound for [Patient.deletionDate] as timestamp.
	 * @param end the upper bound for [Patient.deletionDate] as timestamp. If null, there will be no lower bound.
	 * @param paginationOffset a [PaginationOffset] of [Long] for pagination.
	 * @return a [Flow] of [PaginationElement] containing the [Patient]s.
	 */
	fun findDeletedPatientsByDeleteDate(start: Long, end: Long?, descending: Boolean, paginationOffset: PaginationOffset<Long>): Flow<PaginationElement>
	fun listDeletedPatientsByNames(firstName: String?, lastName: String?): Flow<Patient>
	fun undeletePatients(ids: List<IdAndRev>): Flow<Patient>
	fun getEntityIds(): Flow<String>

	/**
	 * Merges two different patients into one. `fromId` and `fromRev` are the id and revisions of a patient which will
	 * be merged with [updatedInto].
	 * The [Patient.mergeToPatientId] of the `from` patient will be set to the id of the `into` patient, and it will be
	 * soft-deleted.
	 * The into patient will be updated to be the result of the merge, being:
	 * - The content from [updatedInto]
	 * - The merged metadata from the `from` and `into` patient calculated as follows:
	 *   - All encrypted secret ids (from secure delegations, and legacy `delegations` if any)
	 *   - All encrypted owning entity ids (from secure delegations, and legacy `cryptedForeignKeys` if any)
	 *   - Only the encrypted encryption keys of the `into` patient (from secure delegations, and legacy
	 *   ` encryptionKeys` if any).
	 *   - All secret foreign keys (if any)
	 *   - The delegation graphs is merged in the following way
	 *     - If a delegation is root in the `from` and/OR `into` patient, it will be a root delegation in the merged
	 *       patient as well (potentially removing links which exist in one of the delegations).
	 *     - If a delegation is not a root in any of the original patients it will have as parents the parents from both
	 *       the `from` and `into` patient.
	 * This method will succeed only if both updates to the `from` and `into` patient succeed: in case only one of the
	 * update operation fails the method will roll back the change before returning.
	 *
	 * @param fromId id of the `from` patient.
	 * @param expectedFromRev expected revision of the `from` patient.
	 * @param updatedInto the `into` patient with already the encrypted content.
	 * @return the updated `into` patient.
	 * @throws NotFoundRequestException if the `from` and/or `into` patient do not exist.
	 * @throws IllegalArgumentException if:
	 * - The `from` and `into` patient have the same id.
	 * - The metadata of the [updatedInto] patient does not match the current metadata for the patient.
	 * @throws ConflictRequestException if the [expectedFromRev] or [updatedInto] revision does not match the actual
	 * revisions of the respective patients.
	 * @throws AccessDeniedException if the current user does not have write rights to the `into` and/or `from` patient.
	 */
	suspend fun mergePatients(
		fromId: String,
		expectedFromRev: String,
		updatedInto: Patient,
		omitEncryptionKeysOfFrom: Boolean,
	): Patient

	/**
	 * Retrieves the ids of the [Patient]s matching the provided [filter].
	 *
	 * @param filter an [AbstractFilter] of [Patient].
	 * @return a [Flow] of the ids matching the filter.
	 * @throws AccessDeniedException if the filter does not specify any data owner id and the current user does not have
	 * the ExtendedRead.Any permission or if the filter specified a data owner id and the current user does not have the
	 * rights to access their data.
	 */
	fun matchPatientsBy(filter: AbstractFilter<Patient>): Flow<String>
}
