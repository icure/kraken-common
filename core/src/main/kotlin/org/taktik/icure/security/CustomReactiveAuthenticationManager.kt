package org.taktik.icure.security

import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.taktik.icure.security.jwt.JwtAuthentication

interface CustomReactiveAuthenticationManager : ReactiveAuthenticationManager {
	suspend fun authenticateWithUsernameAndPassword(
		authentication: Authentication,
		groupId: String?,
		applicationId: String?,
		scopeDataOwner: String?,
		cacheJwtRefreshDetails: Boolean
	): JwtAuthentication
}
