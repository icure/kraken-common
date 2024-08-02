package org.taktik.icure.security

import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import reactor.core.publisher.Mono

interface CustomReactiveAuthenticationManager: ReactiveAuthenticationManager {

	fun authenticateWithUsernameAndPassword(authentication: Authentication, groupId: String?, applicationId: String?): Mono<Authentication>

}
