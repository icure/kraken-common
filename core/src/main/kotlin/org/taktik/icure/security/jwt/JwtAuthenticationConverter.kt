package org.taktik.icure.security.jwt

import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

data class EncodedJwtAuthenticationToken(
	private val encodedJwt: String,
) : Authentication {
	override fun getName(): String = "jwt"

	override fun getAuthorities(): MutableCollection<out GrantedAuthority> = mutableListOf()

	override fun getCredentials(): Any = encodedJwt

	override fun getDetails(): Any = Any()

	override fun getPrincipal(): Any = Any()

	override fun isAuthenticated(): Boolean = false

	override fun setAuthenticated(isAuthenticated: Boolean) {}
}

@Component
class JwtAuthenticationConverter : ServerAuthenticationConverter {
	override fun convert(exchange: ServerWebExchange?): Mono<Authentication> = mono {
		exchange
			?.request
			?.headers
			?.get("Authorization")
			?.filterNotNull()
			?.first()
			?.let {
				EncodedJwtAuthenticationToken(encodedJwt = it.replace("Bearer ", ""))
			}
	}
}
