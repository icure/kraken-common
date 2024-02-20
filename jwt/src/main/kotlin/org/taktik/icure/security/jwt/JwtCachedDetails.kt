package org.taktik.icure.security.jwt

import java.io.Serializable

data class JwtRefreshCachedDetails(
    /**
     * The original username used to generate the token.
     */
    val credentials: String,
    val authenticatorClass: String?,
    /**
     * A hash of the group id and the user id.
     */
    val groupIdHashes: Set<Long> = emptySet(),
    val needsPwFor2faRefresh: Boolean = true,
) : Serializable
