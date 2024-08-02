package org.taktik.icure.asynclogic

import org.springframework.security.core.Authentication
import org.springframework.web.server.WebSession

interface AsyncSessionLogic {
    suspend fun login(username: String, password: String, session: WebSession? = null, groupId: String?, applicationId: String?): Authentication?

    suspend fun logout()

    suspend fun getAuthentication(): Authentication
}