/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asyncservice.base.EntityWithConflictResolutionService
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.User
import org.taktik.icure.entities.base.PropertyStub
import org.taktik.icure.pagination.PaginationElement

interface UserService : EntityWithConflictResolutionService {
	// region create

	suspend fun createUser(user: User): User?

	// endregion

	// region get

	suspend fun getUser(id: String): User?
	suspend fun getUserByEmail(email: String): User?
	/**
	 * Get a user matching the given identifier in the same group as the current user.
	 * The generic identifier can be an id, login, email or phone number, and the search is done in this order.
	 * This means for example that if the given [genericIdentifier] matches the [User.id] of a user and the [User.login]
	 * of a different user, the user with the matching [User.id] will be returned.
	 * @param genericIdentifier the identifier to search for.
	 * @return a user matching the given identifier in the given group, or null if no user matches the given identifier
	 */
	suspend fun getUserByGenericIdentifier(genericIdentifier: String): User?
	suspend fun getUserByLogin(login: String): User?
	suspend fun getUserByPhone(phone: String): User?
	fun listUserIdsByHcpartyId(hcpartyId: String): Flow<String>
	fun findByPatientId(patientId: String): Flow<String>
	fun findByNameEmailPhone(searchString: String, pagination: PaginationOffset<String>): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves multiple users by their [ids]. All the ids that do not correspond to existing users will be ignored.
	 * Also, this method will filter out all the [User]s that the current user is not allowed to access.
	 *
	 * @param ids the ids of the users to retrieve
	 * @return a [Flow] of [User].
	 * @throws AccessDeniedException if the current user does not meet the precondition to retrieve users.
	 */
	fun getUsers(ids: List<String>): Flow<User>
	fun getUsersByLogin(login: String): Flow<User>
	fun listUserIdsByNameEmailPhone(searchString: String): Flow<String>

	/**
	 * Retrieves all the [User]s in a group in a format for pagination.
	 * If [skipPatients] is true, only the users where [User.patientId] is null or [User.healthcarePartyId] is not null
	 * will be returned (this is because there are legacy users that can be both, and they should be considered as
	 * healthcare party users for the scope of this method).
	 * This method will filter out all the entities that the current user is not allowed to access, but it will guarantee
	 * that the limit specified in the [paginationOffset] is reached as long as there are available entities.
	 *
	 * @param paginationOffset a [PaginationOffset] of [String] for pagination.
	 * @param skipPatients true if the patient-only users should be skipped.
	 * @return a [Flow] of [PaginationElement] containing the [User]s.
	 * @throws AccessDeniedException if the current user does not meet the precondition to search any user.
	 */
	fun listUsers(paginationOffset: PaginationOffset<String>, skipPatients: Boolean): Flow<PaginationElement>
	fun filterUsers(paginationOffset: PaginationOffset<Nothing>, filter: FilterChain<User>): Flow<ViewQueryResultEvent>
	// endregion

	// region modify

	suspend fun modifyUser(modifiedUser: User): User?
	suspend fun setProperties(userId: String, properties: List<PropertyStub>): User?
	suspend fun disableUser(userId: String): User? // TODO use?
	suspend fun enableUser(userId: String): User? // TODO use?

	/**
	 * Create a token for a user matching the generic identifier [userIdentifier] in the current user group.
	 * If there is already a token for [key] in the user it will be replaced by a new token.
	 * @param userIdentifier the identifier of the user to create a token for. This can be an id, login, email or a phone number.
	 * @param key the key to associate with the token
	 * @param tokenValidity the validity of the token in seconds
	 * @param token the token to use. If null a token will be automatically generated
	 * @param useShortToken if true and [token] is null the generated token will be a short numeric token, else it will be a full uuid.
	 * @return the created token
	 */
	suspend fun createOrUpdateToken(userIdentifier: String, key: String, tokenValidity: Long = 3600, token: String? = null, useShortToken: Boolean = false): String

	// endregion

	// region delete

	suspend fun deleteUser(userId: String): DocIdentifier?
	suspend fun undeleteUser(userId: String)

	// endregion

}
