package org.taktik.icure.asynclogic

import org.springframework.security.core.Authentication
import org.springframework.web.server.WebSession
import org.taktik.icure.security.jwt.JwtAuthentication

interface AsyncSessionLogic {
    suspend fun login(
        username: String,
        password: String,
        session: WebSession? = null,
        groupId: String?,
        applicationId: String?
    ): JwtAuthentication

    suspend fun logout()

    suspend fun getAuthentication(): Authentication
}