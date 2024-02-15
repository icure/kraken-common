/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.exception.DocumentNotFoundException
import org.taktik.icure.asyncdao.UserDAO
import org.taktik.icure.asynclogic.UserLogic
import org.taktik.icure.asynclogic.base.AutoFixableLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.constants.Users
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.EnhancedUser
import org.taktik.icure.entities.User
import org.taktik.icure.entities.base.PropertyStub
import org.taktik.icure.entities.security.AuthenticationToken
import org.taktik.icure.exceptions.DuplicateDocumentException
import org.taktik.icure.exceptions.MissingRequirementsException
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.security.credentials.SecretType
import org.taktik.icure.security.credentials.SecretValidator
import org.taktik.icure.security.user.UserEnhancer
import org.taktik.icure.validation.aspect.Fixer
import java.text.DecimalFormat
import java.time.Instant
import java.util.*

open class UserLogicImpl (
	datastoreInstanceProvider: DatastoreInstanceProvider,
	protected val filters: Filters,
	protected val userDAO: UserDAO,
	protected val secretValidator: SecretValidator,
	private val userEnhancer: UserEnhancer,
	fixer: Fixer
) : GenericLogicImpl<User, UserDAO>(fixer, datastoreInstanceProvider), UserLogic {

	private val log: Logger = LoggerFactory.getLogger(UserLogicImpl::class.java)
	protected val shortTokenFormatter = DecimalFormat("000000")

	override suspend fun getUser(id: String): EnhancedUser? {
		val datastoreInformation = getInstanceAndGroup()
		return userEnhancer.enhance(userDAO.getUserOnUserDb(datastoreInformation, id, false))
	}

	override suspend fun getUserByPhone(phone: String): EnhancedUser? {
		val datastoreInformation = getInstanceAndGroup()
		val findByPhone = userDAO.listUsersByPhone(datastoreInformation, phone).toList()

		return findByPhone.firstOrNull()?.let { userEnhancer.enhance(it) }
	}

	override fun findByPatientId(patientId: String): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(userDAO.listUsersByPatientId(datastoreInformation, patientId).mapNotNull { v: User -> v.id })
	}

	override suspend fun getUserByEmail(email: String): EnhancedUser? {
		val datastoreInformation = getInstanceAndGroup()
		val findByEmail = userDAO.listUsersByEmail(datastoreInformation, email).toList()

		return findByEmail.firstOrNull()?.let { userEnhancer.enhance(it) }
	}

	override fun listUserIdsByNameEmailPhone(searchString: String): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(userDAO.listUserIdsByNameEmailPhone(datastoreInformation, searchString))
	}

	override fun listUserIdsByHcpartyId(hcpartyId: String): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(userDAO.listUsersByHcpId(datastoreInformation, hcpartyId).mapNotNull { v: User -> v.id })
	}

	override fun findByNameEmailPhone(
		searchString: String, pagination: PaginationOffset<String>
	): Flow<ViewQueryResultEvent> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(userEnhancer.enhanceViewFlow(userDAO.findUsersByNameEmailPhone(datastoreInformation, searchString, pagination)))
	}

	override suspend fun deleteUser(userId: String): DocIdentifier? {
		val user = getUser(userId)
		return user?.let { deleteUser(it) }
	}

	override suspend fun undeleteUser(userId: String) {
		val user = getUser(userId)
		user?.let { undeleteUser(it) }
	}

	override fun getUsersByLogin(login: String): Flow<EnhancedUser> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			userEnhancer.enhanceFlow(
				userDAO.listUsersByUsername(datastoreInformation, UserLogic.formatLogin(login))
			)
		)
	}

	override suspend fun getUserByLogin(login: String): EnhancedUser? {
		val datastoreInformation = getInstanceAndGroup()
		return userDAO.listUsersByUsername(datastoreInformation, UserLogic.formatLogin(login)).firstOrNull()
			?.let { userEnhancer.enhance(it) }
	}

	override suspend fun createUser(user: User): EnhancedUser? = doCreateUser(
		user,
		getByEmail = { getUserByEmail(it) },
		getByLogin = { getUserByLogin(it) },
		createUser = { createEntities(setOf(it)).firstOrNull() }
	)?.let { userEnhancer.enhance(it) }

	override fun createEntities(entities: Collection<User>): Flow<EnhancedUser> = flow {
		val datastoreInformation = getInstanceAndGroup()
		for (user in entities) {
			fix(user.hashPasswordAndTokens(secretValidator::encodeAndValidateSecrets)) { fixedUser ->
				userDAO.create(
					datastoreInformation, fixedUser
				)?.let { createdUser -> userEnhancer.enhance(createdUser) }
			}?.let { emit(it) }
		}
	}

	override suspend fun modifyUser(modifiedUser: User): EnhancedUser? = fix(modifiedUser) { fixedUser ->
		// Save user
		val datastoreInformation = getInstanceAndGroup()
		val userToUpdate = fixedUser.hashPasswordAndTokens(secretValidator::encodeAndValidateSecrets)
		userDAO.save(datastoreInformation, userToUpdate)?.let { userEnhancer.enhance(it) }
	}

	override suspend fun createOrUpdateToken(
		userIdentifier: String,
		key: String,
		tokenValidity: Long,
		token: String?,
		useShortToken: Boolean
	): String {
		val datastoreInformation = getInstanceAndGroup()
		return doCreateToken(key, tokenValidity, token, useShortToken, datastoreInformation) {
			getUserByGenericIdentifier(userIdentifier)
				?: throw DocumentNotFoundException("User $userIdentifier not found")
		}
	}

	private suspend fun deleteUser(user: User): DocIdentifier? {
		return user.id.let { userId ->
			getUser(userId)?.let {
				val datastoreInformation = getInstanceAndGroup()
				userDAO.remove(datastoreInformation, user)
			}
		}
	}

	private suspend fun undeleteUser(user: User) {
		user.id.let { userId ->
			getUser(userId)?.let {
				val datastoreInformation = getInstanceAndGroup()
				userDAO.unRemove(datastoreInformation, it)
			}
		}
	}

	override suspend fun disableUser(userId: String): User? {
		return getUser(userId)?.let {
			val datastoreInformation = getInstanceAndGroup()
			userDAO.save(datastoreInformation, it.copy(status = Users.Status.DISABLED))
		}
	}

	override suspend fun enableUser(userId: String): User? {
		return getUser(userId)?.let {
			val datastoreInformation = getInstanceAndGroup()
			userDAO.save(datastoreInformation, it.copy(status = Users.Status.ACTIVE))
		}
	}

	override fun modifyEntities(entities: Collection<User>): Flow<User> = flow {
		emitAll(entities.asFlow().mapNotNull { modifyUser(it) })
	}

	override fun getEntities(): Flow<EnhancedUser> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(userEnhancer.enhanceFlow(userDAO.getEntities(datastoreInformation)))
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

	override suspend fun getEntity(id: String): User? {
		return getUser(id)
	}

	override fun listUsers(paginationOffset: PaginationOffset<String>, skipPatients: Boolean) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(userEnhancer.enhanceViewFlow(listUsers(datastoreInformation, paginationOffset, skipPatients)))
	}

	override fun listUsers(datastoreInformation: IDatastoreInformation, pagination: PaginationOffset<String>, skipPatients: Boolean) = flow {
		emitAll(userDAO.findUsers(datastoreInformation, pagination, skipPatients).let { flw ->
			if (!skipPatients) flw else flw.filter {
				when (it) {
					is ViewRowWithDoc<*, *, *> -> {
						(it.doc as User).patientId === null || (it.doc as User).healthcarePartyId != null
					}
					else -> true
				}
			}
		})
	}

	override fun filterUsers(
		paginationOffset: PaginationOffset<Nothing>,
		filter: FilterChain<User>,
	): Flow<ViewQueryResultEvent> = flow {
		val datastoreInformation = getInstanceAndGroup()
		val ids = filters.resolve(filter.filter)
		val sortedIds = paginationOffset.takeUnless { it.startDocumentId == null }?.let { paginationOffset -> // Sub-set starting from startDocId to the end (including last element)
			ids.dropWhile { id -> id != paginationOffset.startDocumentId }
		} ?: ids

		val selectedIds = sortedIds.take(paginationOffset.limit + 1) // Fetching one more healthcare parties for the start key of the next page
		emitAll(userEnhancer.enhanceViewFlow(userDAO.findUsersByIds(datastoreInformation, selectedIds)))
	}

	override suspend fun setProperties(userId: String, properties: List<PropertyStub>): User? {
		val user = getUser(userId) ?: throw NotFoundRequestException("User with id $userId not found")
		val updatedProperties = properties.fold(user.properties) { props, p ->
			val prop = user.properties.find { pp -> pp.type?.identifier == p.type?.identifier }
			prop?.let {
				props - it + it.copy(
					type = if (it.type?.type != null) it.type else it.type?.copy(type = p.typedValue?.type), typedValue = p.typedValue
				)
			} ?: (props + p)
		}
		return modifyUser(user.copy(properties = updatedProperties))
	}

	override fun getUsers(ids: List<String>): Flow<EnhancedUser> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(userEnhancer.enhanceFlow(userDAO.getEntities(datastoreInformation, ids)))
	}

	override fun getGenericDAO(): UserDAO {
		return userDAO
	}

	override suspend fun getUserByGenericIdentifier(genericIdentifier: String): User? =
		getUser(genericIdentifier)
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
	 *
	 * @receiver an [AutoFixableLogic] of [User]
	 * @param user the [User] to be created.
	 * @param getByEmail a function that receives an email and returns the [User] with that email, or null if it does not
	 * exist.
	 * @param getByLogin a function that receives a user login and returns the [User] with that login, or null if it does
	 * not exist.
	 * @param createUser a function that receives a [User] and creates it, returning the created [User] if the operation
	 * was completed successfully and null otherwise.
	 * @return the created [User] or null.
	 */
	protected suspend fun doCreateUser(
		user: User,
		getByEmail: suspend (email: String) -> User?,
		getByLogin: suspend (login: String) -> User?,
		createUser: suspend (updatedUser: User) -> User?,
	): User? {
		val login = user.login?.takeIf { it.isNotBlank() }
			?: user.email?.takeIf { it.isNotBlank() } // TODO we could check with a regex if it is a valid email
			?: user.mobilePhone?.takeIf { it.isNotBlank() } // TODO we could check with a regex if it is a valid phone number
			?: throw MissingRequirementsException("createUser: Requirements are not met. One of Email, Login or Mobile Phone has to be not null and not blank.")
		if (
			user.email?.takeIf { it.isNotBlank() }?.let { getByEmail(it) } != null
		) {
			throw DuplicateDocumentException("User with email ${user.email} already exists")
		}
		if (
			getByLogin(login) != null
		) {
			throw DuplicateDocumentException("User with login $login already exists")
		}
		return fix(
			user.copy(
				createdDate = Instant.now(),
				status = user.status ?: Users.Status.ACTIVE,
				login = login,
				email = user.email?.takeIf { it.isNotBlank() },
				mobilePhone = user.mobilePhone?.takeIf { it.isNotBlank() },
				applicationTokens = emptyMap(),
			).hashPasswordAndTokens(secretValidator::encodeAndValidateSecrets)
		) {
			createUser(it)
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
		getUser: suspend () -> User
	): String {
		val user = getUser()

		// Short tokens generated by this code are not perfectly uniformly distributed but good enough
		val authenticationToken = token ?: (
				if (useShortToken) UUID.randomUUID().leastSignificantBits.let { shortTokenFormatter.format(it % 1000000) }
				else UUID.randomUUID().toString())

		val tokenValidityWithinBounds = tokenValidity
			.coerceAtLeast(1)
			.coerceAtMost(
				if (useShortToken || authenticationToken.length < 10) 600 else Long.MAX_VALUE
			)

		userDAO.save(
			datastoreInformation,
			user.copy(
				authenticationTokens = user.authenticationTokens + (key to AuthenticationToken(
					secretValidator.encodeAndValidateSecrets(
						authenticationToken,
						if(tokenValidityWithinBounds in 1..AuthenticationToken.MAX_SHORT_LIVING_TOKEN_VALIDITY)
							SecretType.SHORT_TOKEN
						else SecretType.LONG_TOKEN
					),
					validity = tokenValidity))
			)
		) ?: throw IllegalStateException("Cannot create token for user")

		return authenticationToken
	}

}