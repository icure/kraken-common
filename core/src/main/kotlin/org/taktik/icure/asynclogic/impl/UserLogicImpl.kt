/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.exception.DocumentNotFoundException
import org.taktik.icure.asyncdao.UserDAO
import org.taktik.icure.asyncdao.results.BulkSaveResult
import org.taktik.icure.asyncdao.results.filterSuccessfulUpdates
import org.taktik.icure.asynclogic.ConflictResolutionLogic
import org.taktik.icure.asynclogic.UserLogic
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.constants.Users
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.EnhancedUser
import org.taktik.icure.entities.User
import org.taktik.icure.entities.base.PropertyStub
import org.taktik.icure.entities.security.AuthenticationToken
import org.taktik.icure.exceptions.DuplicateDocumentException
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.mergers.generated.UserMerger
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.security.credentials.SecretType
import org.taktik.icure.security.credentials.SecretValidator
import org.taktik.icure.security.user.GlobalUserUpdater
import org.taktik.icure.security.user.UserEnhancer
import org.taktik.icure.utils.bufferedChunks
import org.taktik.icure.validation.aspect.Fixer
import java.text.DecimalFormat
import java.util.*

open class UserLogicImpl(
	datastoreInstanceProvider: DatastoreInstanceProvider,
	filters: Filters,
	protected val userDAO: UserDAO,
	protected val secretValidator: SecretValidator,
	private val userEnhancer: UserEnhancer,
	fixer: Fixer,
	private val globalUserUpdater: GlobalUserUpdater,
	userMerger: UserMerger,
) : GenericLogicImpl<User, UserDAO>(fixer, datastoreInstanceProvider, filters),
	ConflictResolutionLogic by ConflictResolutionLogicImpl(userDAO, userMerger, datastoreInstanceProvider),
	UserLogic {
	private val shortTokenFormatter = DecimalFormat("000000")

	override suspend fun getUser(
		id: String,
		includeMetadataFromGlobalUser: Boolean,
	): EnhancedUser? {
		val datastoreInformation = getInstanceAndGroup()
		return userEnhancer.enhance(userDAO.getUserOnUserDb(datastoreInformation, id, false), includeMetadataFromGlobalUser)
	}

	override suspend fun getUserByPhone(phone: String): EnhancedUser? {
		val datastoreInformation = getInstanceAndGroup()
		val findByPhone = userDAO.listUsersByPhone(datastoreInformation, phone).toList()

		return findByPhone.firstOrNull()?.let { userEnhancer.enhance(it, false) }
	}

	override fun findByPatientId(patientId: String): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(userDAO.listUserIdsByPatientId(datastoreInformation, patientId))
	}

	override suspend fun getUserByEmail(email: String): EnhancedUser? {
		val datastoreInformation = getInstanceAndGroup()
		val findByEmail = userDAO.listUsersByEmail(datastoreInformation, email).toList()

		return findByEmail.firstOrNull()?.let { userEnhancer.enhance(it, false) }
	}

	override fun listUserIdsByHcpartyId(hcpartyId: String): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(userDAO.listUserIdsByHcpId(datastoreInformation, hcpartyId))
	}

	override fun findByNameEmailPhone(
		searchString: String,
		pagination: PaginationOffset<String>,
	): Flow<ViewQueryResultEvent> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(userEnhancer.enhanceViewFlow(userDAO.findUsersByNameEmailPhone(datastoreInformation, searchString, pagination), false))
	}

	override fun getUsersByLogin(login: String): Flow<EnhancedUser> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			userEnhancer.enhanceFlow(
				userDAO.listUsersByUsername(datastoreInformation, UserLogic.formatLogin(login)),
				false,
			),
		)
	}

	override suspend fun getUserByLogin(login: String): EnhancedUser? {
		val datastoreInformation = getInstanceAndGroup()
		return userDAO
			.listUsersByUsername(datastoreInformation, UserLogic.formatLogin(login))
			.firstOrNull()
			?.let { userEnhancer.enhance(it, false) }
	}

	override suspend fun createUser(user: User): EnhancedUser {
		checkValidityForCreation(user)
		return createOrModifyUser(user)
	}

	override suspend fun modifyUser(modifiedUser: User): EnhancedUser {
		checkValidityForModification(modifiedUser)
		return createOrModifyUser(modifiedUser)
	}

	private suspend fun createOrModifyUser(user: User): EnhancedUser {
		val created: User =
			doCreateOrModifyUsers(
				getInstanceAndGroup(),
				listOf(user),
				true,
			).single().entityOrThrow()
		return userEnhancer.enhance(globalUserUpdater.tryUpdate(created), false)
	}

	override fun createEntities(entities: Collection<User>): Flow<EnhancedUser> {
		entities.forEach { checkValidityForCreation(it) }
		return createOrModifyEntities(entities)
	}

	protected fun ensureContactsAreUnique(users: Collection<User>) {
		val logins = mutableSetOf<String>()
		users.forEach { user ->
			user.login?.also {
				if (logins.contains(it)) {
					throw IllegalArgumentException("User login $it is duplicated in the batch")
				}
			}
			user.email?.also {
				if (logins.contains(it)) {
					throw IllegalArgumentException("User email $it is duplicated in the batch")
				}
			}
			user.mobilePhone?.also {
				if (logins.contains(it)) {
					throw IllegalArgumentException("User mobile phone $it is duplicated in the batch")
				}
			}
			user.login?.also { logins.add(it) }
			user.email?.also { logins.add(it) }
			user.mobilePhone?.also { logins.add(it) }
		}
	}

	override fun modifyEntities(entities: Collection<User>): Flow<User> {
		entities.forEach { checkValidityForModification(it) }
		return createOrModifyEntities(entities)
	}

	private fun createOrModifyEntities(entities: Collection<User>): Flow<EnhancedUser> = flow {
		ensureContactsAreUnique(entities)
		emitAll(
			userEnhancer.enhanceFlow(
				globalUserUpdater.tryingUpdates(
					doCreateOrModifyUsers(
						getInstanceAndGroup(),
						entities,
						false,
					).filterSuccessfulUpdates(),
				),
				includeMetadataFromGlobalUser = false,
			),
		)
	}

	override suspend fun createOrUpdateToken(
		userIdentifier: String,
		key: String,
		tokenValidity: Long,
		token: String?,
		useShortToken: Boolean,
	): String {
		val datastoreInformation = getInstanceAndGroup()
		return doCreateToken(key, tokenValidity, token, useShortToken, datastoreInformation) {
			getUserByGenericIdentifier(userIdentifier)
				?: throw DocumentNotFoundException("User $userIdentifier not found")
		}.let {
			globalUserUpdater.tryUpdate(it.first)
			it.second
		}
	}

	override suspend fun disableUser(userId: String): User? = getUser(userId, false)?.let { user ->
		val datastoreInformation = getInstanceAndGroup()
		userDAO.save(datastoreInformation, user.copy(status = Users.Status.DISABLED)).also {
			globalUserUpdater.tryUpdate(it)
		}
	}

	override suspend fun enableUser(userId: String): User? = getUser(userId, false)?.let { user ->
		val datastoreInformation = getInstanceAndGroup()
		userDAO.save(datastoreInformation, user.copy(status = Users.Status.ACTIVE)).also {
			globalUserUpdater.tryUpdate(it)
		}
	}

	override fun getEntities(): Flow<EnhancedUser> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(userEnhancer.enhanceFlow(userDAO.getEntities(datastoreInformation), false))
	}

	override fun getEntityIds(): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(userDAO.getEntityIds(datastoreInformation))
	}

	override suspend fun hasEntities(): Boolean {
		val datastoreInformation = getInstanceAndGroup()
		return userDAO.hasAny(datastoreInformation)
	}

	override suspend fun exists(id: String): Boolean {
		val datastoreInformation = getInstanceAndGroup()
		return userDAO.contains(datastoreInformation, id)
	}

	override suspend fun getEntity(id: String): User? = getUser(id, false)

	override fun listUsers(
		paginationOffset: PaginationOffset<String>,
		skipPatients: Boolean,
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			userEnhancer
				.enhanceViewFlow(
					userDAO.findUsers(datastoreInformation, paginationOffset.limitIncludingKey(), skipPatients),
					false,
				).toPaginatedFlow<User>(paginationOffset.limit),
		)
	}

	override fun filterUsers(
		paginationOffset: PaginationOffset<Nothing>,
		filter: FilterChain<User>,
	): Flow<ViewQueryResultEvent> = flow {
		val datastoreInformation = getInstanceAndGroup()
		val ids = filters.resolve(filter.filter, datastoreInformation)
		val sortedIds =
			paginationOffset
				.takeUnless {
					it.startDocumentId == null
				}?.let { paginationOffset ->
					// Sub-set starting from startDocId to the end (including last element)
					ids.dropWhile { id -> id != paginationOffset.startDocumentId }
				} ?: ids

		val selectedIds = sortedIds.take(paginationOffset.limit + 1) // Fetching one more healthcare parties for the start key of the next page
		emitAll(userEnhancer.enhanceViewFlow(userDAO.findUsersByIds(datastoreInformation, selectedIds), false))
	}

	override suspend fun setProperties(
		userId: String,
		properties: List<PropertyStub>,
	): User? {
		val user = getUser(userId, false) ?: throw NotFoundRequestException("User with id $userId not found")
		val updatedProperties =
			properties.fold(user.properties) { props, p ->
				val prop = user.properties.find { pp -> pp.type?.identifier == p.type?.identifier }
				prop?.let {
					props -
						it +
						it.copy(
							type = if (it.type?.type != null) it.type else it.type?.copy(type = p.typedValue?.type),
							typedValue = p.typedValue,
						)
				} ?: (props + p)
			}
		return modifyUser(user.copy(properties = updatedProperties))
	}

	override fun getUsers(ids: List<String>): Flow<EnhancedUser> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(userEnhancer.enhanceFlow(userDAO.getEntities(datastoreInformation, ids), false))
	}

	override fun getGenericDAO(): UserDAO = userDAO

	override suspend fun getUserByGenericIdentifier(genericIdentifier: String): User? = getUser(genericIdentifier, false)
		?: getUserByLogin(genericIdentifier)
		?: getUserByEmail(genericIdentifier)
		?: getUserByPhone(genericIdentifier)

	/**
	 * Creates a new user, abstracting all the differences between the different implementations of the user logic.
	 * The responsibilities of this function are:
	 * - To check that user with the specified login, email, or phone number does not exit yet.
	 * - To remove all the system fields that do not belong to the user (roles, permissions, application tokens).
	 * - To hash the password, if set.
	 * - To create the user.
	 */
	protected fun doCreateOrModifyUsers(
		datastoreInformation: IDatastoreInformation,
		users: Collection<User>,
		throwOnDuplicate: Boolean,
	): Flow<BulkSaveResult<User>> = flow {
		val existingUsers =
			users
				.filter { it.rev != null }
				.takeIf { it.isNotEmpty() }
				?.let { updates ->
					userDAO.getEntities(datastoreInformation, updates.map { it.id }).toList().associateBy { it.id }
				}.orEmpty()
		users.forEach { validateUser(it, existingUsers[it.id]) }
		val nonDuplicateUsers = users.toMutableList()
		checkOrFilterDuplicates(
			nonDuplicateUsers,
			existingUsers,
			throwOnDuplicate,
			"logins",
			{ it.login },
			{ userDAO.findUsedUsernames(datastoreInformation, it) },
		)
		checkOrFilterDuplicates(
			nonDuplicateUsers,
			existingUsers,
			throwOnDuplicate,
			"emails",
			{ it.email },
			{ userDAO.findUsedEmails(datastoreInformation, it) },
		)
		checkOrFilterDuplicates(
			nonDuplicateUsers,
			existingUsers,
			throwOnDuplicate,
			"phones",
			{ it.login },
			{ userDAO.findUsedPhones(datastoreInformation, it) },
		)
		nonDuplicateUsers
			.map { user ->
				fix(
					user
						.copy(
							status = user.status ?: Users.Status.ACTIVE,
							login = user.login ?: user.email ?: user.mobilePhone,
							email = user.email?.takeIf { it.isNotBlank() },
							mobilePhone = user.mobilePhone?.takeIf { it.isNotBlank() },
							applicationTokens = emptyMap(),
						).hashPasswordAndTokens(secretValidator::encodeAndValidateSecrets),
					isCreate = user.rev == null,
				)
			}.takeIf { it.isNotEmpty() }
			?.also {
				emitAll(userDAO.saveBulk(datastoreInformation, it))
			}
	}

	private suspend inline fun checkOrFilterDuplicates(
		users: MutableList<User>,
		existingUsers: Map<String, User>,
		throwOnDuplicate: Boolean,
		fieldsName: String,
		crossinline checkField: (User) -> String?,
		getExisting: (Collection<String>) -> Flow<String>,
	) {
		users
			.mapNotNull { u ->
				val updatedField = checkField(u)
				if (updatedField == existingUsers[u.id]?.let(checkField)) {
					// If entity existed and the field has been unchanged we trust it is still unique
					null
				} else {
					updatedField
				}
			}.takeIf {
				it.isNotEmpty()
			}?.also { fields ->
				val existingFields = getExisting(fields).toSet()
				if (throwOnDuplicate && existingFields.isNotEmpty()) {
					throw DuplicateDocumentException("Users with $fieldsName $existingFields already exist")
				} else {
					users.removeAll { checkField(it) != null && checkField(it) in existingFields }
				}
			}
	}

	private val EMAIL_REGEX = Regex(".+@.+")
	private val PHONE_REGEX = Regex("\\+?[0-9]{6,20}")

	fun validateUser(
		user: User,
		existingUser: User?,
	) {
		require(
			!user.login.isNullOrBlank() ||
				!user.email.isNullOrBlank() ||
				!user.mobilePhone.isNullOrBlank(),
		) {
			"Invalid user ${user.id} - one of `login`, `email` or `mobilePhone` must be not blank"
		}
		require(
			user.mobilePhone?.let {
				it == existingUser?.mobilePhone || PHONE_REGEX.matchEntire(it) != null
			} ?: true,
		) {
			"Invalid mobilePhone \"${user.mobilePhone}\" for user ${user.id}"
		}
		require(
			user.email?.let {
				it == existingUser?.email || EMAIL_REGEX.matchEntire(it) != null
			} ?: true,
		) {
			"Invalid email \"${user.email}\" for user ${user.id}"
		}
	}

	/**
	 * Creates a new authentication token for a [User].
	 * This method will format the token, ensure that its validity it is within the bounds and will update the user.
	 *
	 * @param key a key for the token.
	 * @param tokenValidity the validity duration of the token, in seconds.
	 * @param token the token to create. If it is null, a random one will be generated using a different strategy
	 * according to the value of the [useShortToken] parameter.
	 * @param useShortToken whether to use a short token (6 digits) or a long token. A short token duration cannot
	 * exceed 600 seconds.
	 * @param datastoreInformation an [IDatastoreInformation] that specifies where to save the user.
	 * @param getUser a suspend function that will return the [User] to update.
	 * @return the generated token.
	 */
	protected suspend fun doCreateToken(
		key: String,
		tokenValidity: Long,
		token: String?,
		useShortToken: Boolean,
		datastoreInformation: IDatastoreInformation,
		getUser: suspend () -> User,
	): Pair<User, String> {
		val user = getUser()

		// Short tokens generated by this code are not perfectly uniformly distributed but good enough
		val authenticationToken =
			token ?: (
				if (useShortToken) {
					UUID.randomUUID().leastSignificantBits.let { shortTokenFormatter.format(it % 1000000) }
				} else {
					UUID.randomUUID().toString()
				}
				)

		val tokenValidityWithinBounds =
			tokenValidity
				.coerceAtLeast(1)
				.coerceAtMost(
					if (useShortToken || authenticationToken.length < 10) 600 else Long.MAX_VALUE,
				)

		return Pair(
			userDAO.save(
				datastoreInformation,
				user.copy(
					authenticationTokens =
					user.authenticationTokens +
						(
							key to
								AuthenticationToken(
									secretValidator.encodeAndValidateSecrets(
										authenticationToken,
										if (tokenValidityWithinBounds in 1..AuthenticationToken.MAX_SHORT_LIVING_TOKEN_VALIDITY) {
											SecretType.SHORT_TOKEN
										} else {
											SecretType.LONG_TOKEN
										},
									),
									validity = tokenValidity,
								)
							),
				),
			),
			authenticationToken,
		)
	}

	override fun deleteEntities(identifiers: Collection<IdAndRev>): Flow<User> = globalUserUpdater.tryingUpdates(super.deleteEntities(identifiers))

	override suspend fun undeleteEntity(
		id: String,
		rev: String?,
	): User = globalUserUpdater.tryUpdate(super.undeleteEntity(id, rev))

	override fun undeleteEntities(identifiers: Collection<IdAndRev>): Flow<User> = globalUserUpdater.tryingUpdates(super.undeleteEntities(identifiers))

	override suspend fun deleteEntity(
		id: String,
		rev: String?,
	): User = globalUserUpdater.tryUpdate(super.deleteEntity(id, rev))

	override suspend fun purgeEntity(
		id: String,
		rev: String,
	): DocIdentifier = super.purgeEntity(id, rev).also {
		globalUserUpdater.tryPurge(localId = id, localRev = rev)
	}

	override fun purgeEntities(identifiers: Collection<IdAndRev>): Flow<DocIdentifier> = flow {
		super.purgeEntities(identifiers).bufferedChunks(20, 200).collect { users ->
			globalUserUpdater.tryPurge(
				users.mapNotNull { if (it.id != null) IdAndRev(it.id!!, it.rev) else null }
			)
			emitAll(users.asFlow())
		}
	}
}
