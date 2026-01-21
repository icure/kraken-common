package org.taktik.icure.security

import org.taktik.icure.asynclogic.UserLogic
import org.taktik.icure.entities.User
import org.taktik.icure.entities.security.AuthenticationToken
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.NotFoundRequestException

interface SecureUserMapper<UserDto> {
	/**
	 * Maps a user DTO to a user entity, filling any omitted secret data if the user is not a new user. Does not suspend
	 * when [userDto] represents a new user.
	 * This operation is the inverse of [mapOmittingSecrets].
	 * Note that conversion of old application token to the new format, and hashing of updated passwords, new tokens, or
	 * converted application tokens will not be done by this method: these operations are done in the logic.
	 * @throws NotFoundRequestException if [userDto] represents an existing user that does not exist
	 * @throws ConflictRequestException if [userDto] represents an existing user but the revision is outdated
	 */
	suspend fun mapFillingOmittedSecrets(userDto: UserDto, isCreate: Boolean = false): User

	/**
	 * Maps a user entity to a user DTO, omitting any secret data.
	 * Differently from [mapOmittingSecrets] this operation will return null if the user is not found on the db and
	 * will not check that the revision of the user on the db matches the one on [userDto].
	 */
	suspend fun mapFillingOmittedSecretsOrNull(userDto: UserDto, isCreate: Boolean = false): User?

	/**
	 * Maps a user entity to a user DTO, omitting any secret data.
	 * This operation is the inverse of [mapFillingOmittedSecrets].
	 */
	fun mapOmittingSecrets(user: User): UserDto
}

abstract class AbstractSecureUserMapper<UserDto, AuthenticationTokenDto>(
	private val userLogic: UserLogic,
) : SecureUserMapper<UserDto> {
	override suspend fun mapFillingOmittedSecrets(userDto: UserDto, isCreate: Boolean): User =
		mapFillingOmittedSecrets(userDto, isCreate) { userLogic.getUser(it, includeMetadataFromGlobalUser = false) }

	override suspend fun mapFillingOmittedSecretsOrNull(userDto: UserDto, isCreate: Boolean): User? =
		mapFillingOmittedSecretsOrNull(userDto, isCreate) { userLogic.getUser(it, includeMetadataFromGlobalUser = false) }

	protected suspend fun mapFillingOmittedSecrets(
		userDto: UserDto,
		isCreate: Boolean,
		getExistingUser: suspend (id: String) -> User?,
	): User = checkNotNull(mapFillingOmittedSecretsOrNull(userDto, getExistingUser, failOnMissingUser = true, failOnMismatchedRev = true, isCreate = isCreate)) {
		"Mapping returned null when failure was expected"
	}

	protected suspend fun mapFillingOmittedSecretsOrNull(
		userDto: UserDto,
		isCreate: Boolean,
		getExistingUser: suspend (id: String) -> User?,
	): User? = mapFillingOmittedSecretsOrNull(userDto, getExistingUser, failOnMissingUser = false, failOnMismatchedRev = false, isCreate = isCreate)

	protected suspend fun mapFillingOmittedSecretsOrNull(
		userDto: UserDto,
		getExistingUser: suspend (id: String) -> User?,
		failOnMissingUser: Boolean,
		failOnMismatchedRev: Boolean,
		isCreate: Boolean
	): User? {
		val modifiedUser = unsecureMapDtoToUserIgnoringAuthenticationTokensWithNullValue(userDto)
		return if (modifiedUser.rev != null && !isCreate) {
			val existingUser = getExistingUser(modifiedUser.id)
				?: if (failOnMissingUser) throw NotFoundRequestException("User ${modifiedUser.id} does not exist") else return null
			if (existingUser.rev != modifiedUser.rev && failOnMismatchedRev) {
				throw ConflictRequestException("Outdated revision for user ${modifiedUser.id}")
			} else if (existingUser.rev != modifiedUser.rev) {
				return null
			}
			val filledPassword = if (modifiedUser.passwordHash == "*") existingUser.passwordHash else modifiedUser.passwordHash
			if (modifiedUser.use2fa == true) {
				requireNotNull(modifiedUser.secret ?: existingUser.secret) {
					"Secret is required when 2FA is enabled"
				}
			}
			val filledApplicationTokens =
				existingUser.applicationTokens?.let {
					it + (modifiedUser.applicationTokens ?: emptyMap())
				} ?: modifiedUser.applicationTokens
			val filledAuthenticationTokens =
				existingUser.authenticationTokens + modifiedUser.authenticationTokens - userDto.deletedTokensKeys()
			modifiedUser.copy(
				passwordHash = filledPassword,
				applicationTokens = filledApplicationTokens,
				authenticationTokens = filledAuthenticationTokens,
			)
		} else {
			modifiedUser
		}
	}

	/**
	 * Maps a user entity to a user DTO, omitting any secret data.
	 * This operation is the inverse of [mapFillingOmittedSecrets].
	 */
	override fun mapOmittingSecrets(user: User): UserDto = unsecureMapUserToDto(
		user.copy(
			passwordHash = user.passwordHash?.let { "*" },
			applicationTokens = emptyMap(),
		),
	).withAuthenticationTokens(
		user.authenticationTokens.mapValues { mapTokenOmittingValue(it.value) },
	)

	/**
	 * Dtos accept null value for authentication tokens, but the real entity no: this mapping should ignore all tokens
	 * with null value
	 */
	protected abstract fun unsecureMapDtoToUserIgnoringAuthenticationTokensWithNullValue(userDto: UserDto): User

	/**
	 * Map an authentication token to the dto counterpart but omitting the token value
	 */
	protected abstract fun mapTokenOmittingValue(token: AuthenticationToken): AuthenticationTokenDto

	/**
	 * Map a user to the dto counterpart without omitting anything
	 */
	protected abstract fun unsecureMapUserToDto(user: User): UserDto

	/**
	 * Creates a copy of the dto with the authentication tokens set to the given value
	 */
	protected abstract fun UserDto.withAuthenticationTokens(tokens: Map<String, AuthenticationTokenDto>): UserDto

	/**
	 * Returns the keys of authentication tokens which should be deleted.
	 * These will usually be more than the keys of tokens with non-null [AuthenticationToken.deletionDate] obtained in
	 * a [User] result of [unsecureMapDtoToUserIgnoringAuthenticationTokensWithNullValue] since deleted tokens will
	 * usually have a null value.
	 */
	protected abstract fun UserDto.deletedTokensKeys(): Set<String>
}
