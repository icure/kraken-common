@file:Suppress("DEPRECATION")

package org.taktik.icure.security.jwt

import org.taktik.icure.services.external.rest.v1.dto.AuthenticationResponse
import org.springframework.security.core.Authentication
import org.taktik.icure.entities.security.jwt.JwtResponse

interface JwtToResponseMapper {

    fun toAuthenticationResponse(authentication: Authentication, username: String, jwtDuration: Long? = null): AuthenticationResponse
    fun toJwtResponse(authentication: Authentication, jwtDuration: Long? = null): JwtResponse

}