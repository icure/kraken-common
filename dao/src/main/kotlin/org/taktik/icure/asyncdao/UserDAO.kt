/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.User

interface UserDAO : GenericDAO<User> {
	fun listUserIdsByNameEmailPhone(datastoreInformation: IDatastoreInformation, searchString: String): Flow<String>
	fun listUsersByUsername(datastoreInformation: IDatastoreInformation, username: String): Flow<User>
	/**
	 * @return all items from [usernames] that appear in at least a [User.login].
	 */
	fun findUsedUsernames(datastoreInformation: IDatastoreInformation, usernames: Collection<String>): Flow<String>
	fun listUsersByEmail(datastoreInformation: IDatastoreInformation, searchString: String): Flow<User>
	/**
	 * @return all items from [emails] that appear in at least a [User.email]
	 */
	fun findUsedEmails(datastoreInformation: IDatastoreInformation, emails: Collection<String>): Flow<String>
	fun listUsersByPhone(datastoreInformation: IDatastoreInformation, phone: String): Flow<User>
	/**
	 * @return all items from [phones] that appear normalized in at least a [User.mobilePhone]
	 */
	fun findUsedPhones(datastoreInformation: IDatastoreInformation, phones: Collection<String>): Flow<String>

	/**
	 * Retrieves all the [User]s in a group in a format for pagination.
	 * If [skipPatients] is true, only the users where [User.patientId] is null or [User.healthcarePartyId] is not null
	 * will be returned (this is because there are legacy users that can be both, and they should be considered as
	 * healthcare party users for the scope of this method).
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param pagination a [PaginationOffset] of [String] for pagination.
	 * @param skipPatients true if the patient-only users should be skipped.
	 * @return a [Flow] of [ViewQueryResultEvent] containing the [User]s.
	 */
	fun findUsers(datastoreInformation: IDatastoreInformation, pagination: PaginationOffset<String>, skipPatients: Boolean = false): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [User]s where [User.healthcarePartyId] is equal to [hcPartyId].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param hcPartyId the healthcare party id to use as key.
	 * @return a [Flow] of [User]s.
	 */
	fun listUsersByHcpId(datastoreInformation: IDatastoreInformation, hcPartyId: String): Flow<User>

	/**
	 * Retrieves the ids of all the [User]s where [User.patientId] is equal to [patientId].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param patientId the patient id to use as key.
	 * @return a [Flow] of [User.id]s.
	 */
	fun listUsersByPatientId(datastoreInformation: IDatastoreInformation, patientId: String): Flow<User>
	suspend fun getUserOnUserDb(datastoreInformation: IDatastoreInformation, userId: String, bypassCache: Boolean): User
	suspend fun findUserOnUserDb(datastoreInformation: IDatastoreInformation, userId: String, bypassCache: Boolean): User?
	fun getUsersOnDb(datastoreInformation: IDatastoreInformation): Flow<User>
	fun findUsersByIds(datastoreInformation: IDatastoreInformation, userIds: Flow<String>): Flow<ViewQueryResultEvent>
	fun findUsersByNameEmailPhone(datastoreInformation: IDatastoreInformation, searchString: String, pagination: PaginationOffset<String>): Flow<ViewQueryResultEvent>
	fun listConflicts(datastoreInformation: IDatastoreInformation): Flow<User>

	/**
	 * Retrieves the ids of all the [User]s where [User.healthcarePartyId] is equal to [hcPartyId].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param hcPartyId the healthcare party id to use as key.
	 * @return a [Flow] of [User.id]s.
	 */
	fun listUserIdsByHcpId(datastoreInformation: IDatastoreInformation, hcPartyId: String): Flow<String>

	/**
	 * Retrieves the ids of all the [User]s where [User.patientId] is equal to [patientId].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param patientId the patient id to use as key.
	 * @return a [Flow] of [User.id]s.
	 */
	fun listUserIdsByPatientId(datastoreInformation: IDatastoreInformation, patientId: String): Flow<String>
}
