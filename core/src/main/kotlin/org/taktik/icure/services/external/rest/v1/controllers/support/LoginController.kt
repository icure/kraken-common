/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.support

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.reactor.asCoroutineContext
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.WebSession
import org.taktik.icure.asynclogic.AsyncSessionLogic
import org.taktik.icure.entities.security.jwt.JwtResponse
import org.taktik.icure.exceptions.Invalid2FAException
import org.taktik.icure.exceptions.Missing2FAException
import org.taktik.icure.exceptions.PasswordTooShortException
import org.taktik.icure.exceptions.TooManyRequestsException
import org.taktik.icure.security.AbstractAuthenticationManager
import org.taktik.icure.security.SecurityToken
import org.taktik.icure.security.jwt.JwtAuthentication
import org.taktik.icure.security.jwt.JwtDetails
import org.taktik.icure.security.jwt.JwtRefreshDetails
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.services.external.rest.v1.dto.AuthenticationResponse
import org.taktik.icure.services.external.rest.v1.dto.LoginCredentials
import org.taktik.icure.spring.asynccache.AsyncCacheManager
import reactor.core.publisher.Mono
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

@RestController
@Profile("app")
@RequestMapping("/rest/v1/auth")
@Tag(name = "auth")
class LoginController(
	private val sessionLogic: AsyncSessionLogic,
	private val authenticationManager: AbstractAuthenticationManager<JwtDetails, JwtRefreshDetails>,
	private val jwtUtils: JwtUtils,
	asyncCacheManager: AsyncCacheManager
) {
	val cache = asyncCacheManager.getCache<String, SecurityToken>("spring.security.tokens")

	@Value("\${spring.session.enabled}")
	private val sessionEnabled: Boolean = false

	@Suppress("DEPRECATION")
	private suspend fun produceAuthenticationResponse(
		authentication: JwtAuthentication?,
		username: String,
		jwtDuration: Long? = null,
		session: WebSession? = null
	): ResponseEntity<AuthenticationResponse> = if (authentication != null && authentication.isAuthenticated && sessionEnabled) {
		val secContext = SecurityContextImpl(authentication)
		val securityContext = coroutineContext[ReactorContext]?.context?.put(SecurityContext::class.java, Mono.just(secContext))
		withContext(coroutineContext.plus(securityContext?.asCoroutineContext() as CoroutineContext)) {
			ResponseEntity.ok().body(
				authentication.toAuthenticationResponse(username, jwtDuration).also {
					if (session != null) {
						session.attributes["SPRING_SECURITY_CONTEXT"] = secContext
					}
				}
			)
		}
	} else if (authentication != null && authentication.isAuthenticated && !sessionEnabled) {
		ResponseEntity.ok().body(
			authentication.toAuthenticationResponse(username, jwtDuration)
		)
	} else ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AuthenticationResponse(successful = false))

	@Operation(summary = "login", description = "Login using username and password")
	@PostMapping("/login")
	fun login(
		request: ServerHttpRequest,
		@Parameter(description = "The duration of the generated token. It cannot exceed the one defined in the system settings", required = false) @RequestParam duration: Long? = null,
		@RequestBody loginCredentials: LoginCredentials,
		@Parameter(hidden = true) session: WebSession?,
		@Parameter(description = "If the credentials are valid for the provided group id the token created will be already in that group context, without requiring a switch group call after") @RequestParam(required = false) groupId: String? = null
	) = mono {
		try {
			val username = loginCredentials.username!!

			val authentication = sessionLogic.login(username, loginCredentials.password!!, if (sessionEnabled) session else null, groupId, null)
			produceAuthenticationResponse(authentication, username, duration?.seconds?.inWholeMilliseconds, session)
		} catch (e: Exception) {
			@Suppress("DEPRECATION")
			ResponseEntity.status(
				when(e){
					is PasswordTooShortException -> HttpStatus.PRECONDITION_FAILED
					is Missing2FAException -> HttpStatus.EXPECTATION_FAILED
					is Invalid2FAException -> HttpStatus.NOT_ACCEPTABLE
					is BadCredentialsException -> HttpStatus.UNAUTHORIZED
					is TooManyRequestsException -> HttpStatus.TOO_MANY_REQUESTS
					else -> HttpStatus.UNAUTHORIZED
				}
			).body(AuthenticationResponse(successful = false))
		}
	}

	@Operation(summary = "refresh", description = "Get a new authentication token using a refresh token")
	@PostMapping("/refresh")
	fun refresh(
		request: ServerHttpRequest,
		response: ServerHttpResponse,
		@RequestParam(required = false) totp: String?
	) = mono {
		val refreshToken = jwtUtils.extractRawRefreshTokenFromRequest(request)
		val newJwtDetails = authenticationManager.regenerateAuthJwt(refreshToken, totpToken = totp)
		JwtResponse(
			successful = true,
			token = jwtUtils.createAuthJWT(newJwtDetails.first, newJwtDetails.second)
		)
	}

	@Operation(summary = "check", description = "Check login using groupId/userId and password")
	@PostMapping("/check")
	fun check(@RequestBody loginCredentials: LoginCredentials) = mono {
		try {
			authenticationManager.checkAuthentication(
				loginCredentials.username ?: throw IllegalArgumentException("Username is required"),
				loginCredentials.password ?: throw IllegalArgumentException("Password is required")
			)
			@Suppress("DEPRECATION")
			AuthenticationResponse(successful = true)
		} catch (e: Exception) {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED, e.message)
		}
	}

	@Operation(summary = "logout", description = "Logout")
	@GetMapping("/logout")
	@ConditionalOnProperty(prefix = "spring", name = ["session.enabled"], havingValue = "true", matchIfMissing = false)
	fun logout() = mono {
		sessionLogic.logout()
		@Suppress("DEPRECATION")
		AuthenticationResponse(successful = true)
	}

	@Operation(summary = "logout", description = "Logout")
	@PostMapping("/logout")
	@ConditionalOnProperty(prefix = "spring", name = ["session.enabled"], havingValue = "true", matchIfMissing = false)
	fun logoutPost() = mono {
		sessionLogic.logout()
		@Suppress("DEPRECATION")
		AuthenticationResponse(successful = true)
	}

	@Deprecated("OTT Websocket auth is deprecated, use the JWT instead")
	@Operation(summary = "token", description = "Get token for subsequent operation")
	@GetMapping("/token/{method}/{path}")
	fun token(@PathVariable method: String, @PathVariable path: String) = mono {
		val token = UUID.randomUUID().toString()
		cache.put(token, SecurityToken(HttpMethod.valueOf(method), path, sessionLogic.getAuthentication()))
		token
	}
}
