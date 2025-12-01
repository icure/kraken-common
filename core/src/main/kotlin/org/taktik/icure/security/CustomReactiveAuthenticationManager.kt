package org.taktik.icure.security

import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.taktik.icure.entities.security.jwt.JwtResponse
import org.taktik.icure.security.jwt.JwtAuthentication
import org.taktik.icure.services.external.rest.v1.dto.AuthenticationResponse
import reactor.core.publisher.Mono

interface CustomReactiveAuthenticationManager : ReactiveAuthenticationManager {
	suspend fun authenticateWithUsernameAndPassword(
		authentication: Authentication,
		groupId: String?,
		applicationId: String?,
		scopeDataOwner: String?,
	): JwtAuthentication
}
