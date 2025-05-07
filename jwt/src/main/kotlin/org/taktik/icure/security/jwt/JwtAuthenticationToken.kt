package org.taktik.icure.security.jwt

import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.taktik.icure.entities.security.jwt.JwtResponse
import org.taktik.icure.services.external.rest.v1.dto.AuthenticationResponse


interface JwtAuthentication : Authentication {
    fun toAuthenticationResponse(jwtUtils: JwtUtils, username: String, jwtDuration: Long? = null): AuthenticationResponse
    fun toJwtResponse(jwtUtils: JwtUtils, jwtDuration: Long? = null): JwtResponse
}

abstract class AbstractJwtAuthentication<out T : JwtDetails>(
    authorities: Set<GrantedAuthority>,
    protected val authClaims: T,
    private val details: Map<String, Any> = mapOf(),
    private var authenticated: Boolean = false
): JwtAuthentication {
    private val _authorities = authorities.toMutableSet()
    override fun getName(): String = "jwt"
    override fun getAuthorities(): MutableCollection<out GrantedAuthority> = _authorities
    override fun getCredentials(): Any = authClaims
    override fun getDetails(): Any = details
    override fun getPrincipal(): Any = authClaims
    override fun isAuthenticated(): Boolean = authenticated
    override fun setAuthenticated(isAuthenticated: Boolean) {
        authenticated = isAuthenticated
    }
}