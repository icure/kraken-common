/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.support

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
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
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
import org.taktik.icure.security.jwt.JwtDetails
import org.taktik.icure.security.jwt.JwtRefreshDetails
import org.taktik.icure.security.jwt.JwtToResponseMapper
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.services.external.rest.v2.dto.LoginCredentials
import org.taktik.icure.services.external.rest.v2.mapper.JwtResponseV2Mapper
import org.taktik.icure.spring.asynccache.AsyncCacheManager
import reactor.core.publisher.Mono
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

@RestController("loginControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/auth")
@Tag(name = "auth")
class LoginController(
	private val sessionLogic: AsyncSessionLogic,
	private val authenticationManager: AbstractAuthenticationManager<JwtDetails, JwtRefreshDetails>,
	private val jwtUtils: JwtUtils,
	private val jwtToResponseMapper: JwtToResponseMapper,
	private val jwtResponseV2Mapper: JwtResponseV2Mapper,
	asyncCacheManager: AsyncCacheManager
) {
	val cache = asyncCacheManager.getCache<String, SecurityToken>("spring.security.tokens")

	@Value("\${spring.session.enabled}")
	private val sessionEnabled: Boolean = false

	@Operation(summary = "login", description = "Login using username and password")
	@PostMapping("/login")
	fun login(
		@Parameter(description = "The duration of the generated token in seconds. It cannot exceed the one defined in the system settings", required = false) @RequestParam duration: Long? = null,
		@RequestBody loginCredentials: LoginCredentials,
		@Parameter(hidden = true) session: WebSession?,
		@Parameter(description = "If the credentials are valid for the provided group id the token created will be already in that group context, without requiring a switch group call after") @RequestParam(required = false) groupId: String? = null
	) = mono {
		try {
			val authentication = sessionLogic.login(loginCredentials.username!!, loginCredentials.password!!, if (sessionEnabled) session else null, groupId)
			if (authentication != null && authentication.isAuthenticated && sessionEnabled) {
				val secContext = SecurityContextImpl(authentication)
				val securityContext = kotlin.coroutines.coroutineContext[ReactorContext]?.context?.put(SecurityContext::class.java, Mono.just(secContext))
				withContext(kotlin.coroutines.coroutineContext.plus(securityContext?.asCoroutineContext() as CoroutineContext)) {
					ResponseEntity.ok().body(
						jwtToResponseMapper.toJwtResponse(authentication, duration?.seconds?.inWholeMilliseconds).also {
							if (session != null) {
								session.attributes["SPRING_SECURITY_CONTEXT"] = secContext
							}
						}.let(jwtResponseV2Mapper::map)
					)
				}
			} else if (authentication != null && authentication.isAuthenticated && !sessionEnabled) {
				ResponseEntity.ok().body(
					jwtToResponseMapper.toJwtResponse(authentication, duration?.seconds?.inWholeMilliseconds).let(jwtResponseV2Mapper::map)
				)
			} else ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(JwtResponse(successful = false))
		} catch (e: Exception) {
			ResponseEntity.status(
				when(e){
					is PasswordTooShortException -> HttpStatus.PRECONDITION_FAILED
					is Missing2FAException -> HttpStatus.EXPECTATION_FAILED
					is Invalid2FAException -> HttpStatus.NOT_ACCEPTABLE
					is BadCredentialsException -> HttpStatus.UNAUTHORIZED
					is TooManyRequestsException -> HttpStatus.TOO_MANY_REQUESTS
					else -> HttpStatus.UNAUTHORIZED
				}
			).body(JwtResponse(successful = false))
		}
	}

	@Operation(summary = "refresh", description = "Get a new authentication token using a refresh token")
	@PostMapping("/refresh")
	fun refresh(
		@RequestHeader(name = "Refresh-Token") rawRefreshToken: String,
		@RequestParam(required = false) totp: String?
	) = mono {
		val refreshToken = rawRefreshToken.replace("Bearer ", "")
		val newJwtDetails = authenticationManager.regenerateJwtDetails(refreshToken, totpToken = totp)
		JwtResponse(
			successful = true,
			token = jwtUtils.createJWT(
				newJwtDetails,
				jwtUtils.getJwtDurationFromRefreshToken(refreshToken)
			)
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
			ResponseEntity.noContent()
		} catch (e: Exception) {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED, e.message)
		}
	}
	@Operation(summary = "token", description = "Get token for subsequent operation")
	@GetMapping("/token/{method}/{path}")
	fun token(@PathVariable method: String, @PathVariable path: String) = mono {
		val token = UUID.randomUUID().toString()
		cache.put(token, SecurityToken(HttpMethod.valueOf(method), path, sessionLogic.getAuthentication()))
		token
	}
	@Operation(summary = "logout", description = "Logout")
	@GetMapping("/logout")
	@ConditionalOnProperty(prefix = "spring", name = ["session.enabled"], havingValue = "true", matchIfMissing = false)
	fun logout() = mono {
		sessionLogic.logout()
		JwtResponse(successful = true)
	}

	@Operation(summary = "logout", description = "Logout")
	@PostMapping("/logout")
	@ConditionalOnProperty(prefix = "spring", name = ["session.enabled"], havingValue = "true", matchIfMissing = false)
	fun logoutPost() = mono {
		sessionLogic.logout()
		JwtResponse(successful = true)
	}
}
