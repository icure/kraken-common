package org.taktik.icure.services.external.rest.v1.mapper

import org.taktik.icure.asynclogic.UserLogic
import org.taktik.icure.customentities.mapping.MapperExtensionsValidationContext
import org.taktik.icure.entities.User
import org.taktik.icure.entities.security.AuthenticationToken
import org.taktik.icure.security.AbstractSecureUserMapper
import org.taktik.icure.security.SecureUserMapper
import org.taktik.icure.services.external.rest.v1.dto.UserDto
import org.taktik.icure.services.external.rest.v1.dto.security.AuthenticationTokenDto
import org.taktik.icure.services.external.rest.v1.mapper.security.UnsecureAuthenticationTokenMapper

interface SecureUserV1Mapper : SecureUserMapper<UserDto> {
	suspend fun mapFillingOmittedSecrets(
		userDto: UserDto,
		isCreate: Boolean = false
	): User

	suspend fun mapFillingOmittedSecretsOrNull(
		userDto: UserDto,
		isCreate: Boolean = false
	): User?

	suspend fun mapFillingOmittedSecretsFromRev(
		userDto: UserDto,
	): User
}

open class SecureUserV1MapperImpl(
	userLogic: UserLogic,
	private val unsecureMapper: UnsecureUserMapper,
	private val unsecureTokenMapper: UnsecureAuthenticationTokenMapper,
) : AbstractSecureUserMapper<UserDto, AuthenticationTokenDto>(userLogic),
	SecureUserV1Mapper {

	override fun getUserRev(userDto: UserDto): String = checkNotNull(userDto.rev) { "User ${userDto.rev} has no rev" }

	override fun unsecureMapDtoToUserIgnoringAuthenticationTokensWithNullValue(
		userDto: UserDto,
		mapperExtensionsValidationContext: MapperExtensionsValidationContext,
	): User = unsecureMapper.map(
		userDto.copy(
			authenticationTokens = userDto.authenticationTokens.filterValues { it.token != null },
		),
	)

	override suspend fun mapFillingOmittedSecrets(
		userDto: UserDto,
		isCreate: Boolean
	): User = mapFillingOmittedSecrets(
		userDto = userDto,
		mapperExtensionsValidationContext = MapperExtensionsValidationContext.Empty,
		isCreate = isCreate,
	)

	override suspend fun mapFillingOmittedSecretsFromRev(userDto: UserDto): User =
		mapFillingOmittedSecretsFromRev(
			userDto = userDto,
			mapperExtensionsValidationContext = MapperExtensionsValidationContext.Empty,
		)

	override suspend fun mapFillingOmittedSecretsOrNull(
		userDto: UserDto,
		isCreate: Boolean
	): User? = mapFillingOmittedSecretsOrNull(
		userDto = userDto,
		mapperExtensionsValidationContext = MapperExtensionsValidationContext.Empty,
		isCreate = isCreate,
	)

	override fun mapTokenOmittingValue(token: AuthenticationToken): AuthenticationTokenDto = unsecureTokenMapper.map(token).copy(token = null)

	override fun unsecureMapUserToDto(user: User): UserDto = unsecureMapper.map(user)

	override fun UserDto.withAuthenticationTokens(tokens: Map<String, AuthenticationTokenDto>): UserDto = copy(authenticationTokens = tokens)

	override fun UserDto.deletedTokensKeys(): Set<String> = authenticationTokens.filterValues { it.deletionDate != null }.keys
}
