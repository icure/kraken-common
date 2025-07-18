package org.taktik.icure.services.external.rest.v2.mapper

import org.taktik.icure.asynclogic.UserLogic
import org.taktik.icure.entities.User
import org.taktik.icure.entities.security.AuthenticationToken
import org.taktik.icure.security.AbstractSecureUserMapper
import org.taktik.icure.security.SecureUserMapper
import org.taktik.icure.services.external.rest.v2.dto.UserDto
import org.taktik.icure.services.external.rest.v2.dto.security.AuthenticationTokenDto
import org.taktik.icure.services.external.rest.v2.mapper.security.UnsecureAuthenticationTokenV2Mapper

interface SecureUserV2Mapper : SecureUserMapper<UserDto>

open class SecureUserV2MapperImpl(
	userLogic: UserLogic,
	private val unsecureMapper: UnsecureUserV2Mapper,
	private val unsecureTokenMapper: UnsecureAuthenticationTokenV2Mapper,
) : AbstractSecureUserMapper<UserDto, AuthenticationTokenDto>(userLogic),
	SecureUserV2Mapper {
	override fun unsecureMapDtoToUserIgnoringAuthenticationTokensWithNullValue(userDto: UserDto): User = unsecureMapper.map(
		userDto.copy(
			authenticationTokens = userDto.authenticationTokens.filterValues { it.token != null },
		),
	)

	override fun mapTokenOmittingValue(token: AuthenticationToken): AuthenticationTokenDto = unsecureTokenMapper.map(token).copy(token = null)

	override fun unsecureMapUserToDto(user: User): UserDto = unsecureMapper.map(user)

	override fun UserDto.withAuthenticationTokens(tokens: Map<String, AuthenticationTokenDto>): UserDto = copy(authenticationTokens = tokens)

	override fun UserDto.deletedTokensKeys(): Set<String> = authenticationTokens.filterValues { it.deletionDate != null }.keys
}
