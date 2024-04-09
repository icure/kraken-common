/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.web.server.WebSession
import org.taktik.icure.asynclogic.AsyncSessionLogic
import org.taktik.icure.security.AbstractAuthenticationManager
import org.taktik.icure.security.loadSecurityContext

@Service
@Profile("app")
class AsyncSessionLogicImpl(
	private val authenticationManager: AbstractAuthenticationManager<*, *>,
) : AsyncSessionLogic {
	/* Generic */

	val log: Logger = LoggerFactory.getLogger(this::class.java)

	override suspend fun login(username: String, password: String, session: WebSession?, groupId: String?): Authentication? {
		val token = UsernamePasswordAuthenticationToken(username, password)
		val authentication = authenticationManager.authenticateWithUsernameAndPassword(token, groupId).awaitFirstOrNull()
		if (session != null) session.attributes[SESSION_LOCALE_ATTRIBUTE] = "fr" // TODO MB : add locale support
		return authentication
	}


	override suspend fun logout() {
		invalidateCurrentAuthentication()
	}

	override suspend fun getAuthentication(): Authentication =
		loadSecurityContext()?.map { it.authentication }?.awaitFirstOrNull()
			?: throw AuthenticationServiceException("Authentication is not available in the current context")

	companion object {
		const val SESSION_LOCALE_ATTRIBUTE = "locale"

		private suspend fun invalidateCurrentAuthentication() {
			loadSecurityContext()?.map { it.authentication.isAuthenticated = false }?.awaitFirstOrNull()
				?: throw AuthenticationServiceException("Could not find authentication object in ReactorContext")
		}
	}
}
