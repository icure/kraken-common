package org.taktik.icure.security.jwt

import org.taktik.icure.services.external.rest.v1.dto.AuthenticationResponse
import org.springframework.security.core.Authentication

interface JwtToResponseMapper {

    fun toAuthenticationResponse(authentication: Authentication, username: String, jwtDuration: Long? = null): AuthenticationResponse
    fun toJwtResponse(authentication: Authentication, jwtDuration: Long? = null): JwtResponse

}