/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.EnhancedUser
import org.taktik.icure.entities.User
import org.taktik.icure.entities.base.PropertyStub
import org.taktik.icure.pagination.PaginationElement

interface UserLogic : EntityPersister<User> {

	companion object {
		fun formatLogin(login: String) = login.trim { it <= ' ' }
	}

	// region create

	suspend fun createUser(user: User): EnhancedUser

	// endregion

	// region get

	suspend fun getUser(id: String, includeMetadataFromGlobalUser: Boolean): EnhancedUser?
	suspend fun getUserByEmail(email: String): EnhancedUser?

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
	suspend fun getUserByPhone(phone: String): EnhancedUser?
	fun listUserIdsByHcpartyId(hcpartyId: String): Flow<String>
	fun findByPatientId(patientId: String): Flow<String>
	fun findByNameEmailPhone(searchString: String, pagination: PaginationOffset<String>): Flow<ViewQueryResultEvent>
	fun getUsers(ids: List<String>): Flow<User>
	fun getUsersByLogin(login: String): Flow<EnhancedUser>

	/**
	 * Retrieves all the [User]s in a group in a format for pagination.
	 * If [skipPatients] is true, only the users where [User.patientId] is null or [User.healthcarePartyId] is not null
	 * will be returned (this is because there are legacy users that can be both, and they should be considered as
	 * healthcare party users for the scope of this method).
	 *
	 * @param paginationOffset a [PaginationOffset] of [String] for pagination.
	 * @param skipPatients true if the patient-only users should be skipped.
	 * @return a [Flow] of [PaginationElement] containing the [User]s.
	 */
	fun listUsers(paginationOffset: PaginationOffset<String>, skipPatients: Boolean): Flow<PaginationElement>
	fun filterUsers(paginationOffset: PaginationOffset<Nothing>, filter: FilterChain<User>): Flow<ViewQueryResultEvent>

	// endregion

	// region modify

	suspend fun modifyUser(modifiedUser: User): EnhancedUser
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

	fun solveConflicts(limit: Int? = null, ids: List<String>? = null): Flow<IdAndRev>

	/**
	 * Change user email of the user if it matches the provided [previousEmail], conflict otherwise.
	 * This is useful if the user revision is unknown or might change after the SDK calls the corresponding method,
	 * for example, if a new authentication token was created by a secret provider following a "requireElevatedSecurity".
	 */
	suspend fun changeUserEmail(userId: String, newEmail: String, previousEmail: String): User

	/**
	 * Change user mobile phone of the user if it matches the provided [previousMobilePhone], conflict otherwise.
	 * This is useful if the user revision is unknown or might change after the SDK calls the corresponding method,
	 * for example, if a new authentication token was created by a secret provider following a "requireElevatedSecurity".
	 */
	suspend fun changeUserMobilePhone(userId: String, newMobilePhone: String, previousMobilePhone: String): User

	/**
	 * Changes the password of the user.
	 * The previous password is not required, to allow this method to be used even as a "forgot password" flow.
	 */
	suspend fun changeUserPassword(userId: String, newPassword: String): User

	// endregion
}
