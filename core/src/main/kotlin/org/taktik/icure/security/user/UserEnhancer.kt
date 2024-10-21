package org.taktik.icure.security.user

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.entities.EnhancedUser
import org.taktik.icure.entities.User

interface UserEnhancer {

    /**
     * Enhances a single [User] from a group database with the security data from the fallback db.
     *
     * @param user the [User] to enhance.
     * @param includeMetadataFromGlobalUser if true the global user information will be used to enhance the user (doing
     * an additional request), else only readily available information are used by the enhancer. This parameter is
     * ignored on lite.
     * @return an [EnhancedUser].
     */
    suspend fun enhance(
        user: User,
        includeMetadataFromGlobalUser: Boolean
    ): EnhancedUser

    /**
     * Enhances a [Flow] of [User]s from a group database with the security data from the fallback db.
     *
     * @param usersFlow a [Flow] of [User]s to enhance.
     * @param includeMetadataFromGlobalUser if true the global user information will be used to enhance the user (doing
     * an additional request), else only readily available information are used by the enhancer. This parameter is
     * ignored on lite.
     * @return a [Flow] containing the [EnhancedUser]s, each one with the security data from the fallback db.
     */
    suspend fun enhanceFlow(
        usersFlow: Flow<User>,
        includeMetadataFromGlobalUser: Boolean
    ): Flow<EnhancedUser>

    /**
     * Enhances a [Flow] of [User]s, each one wrapped in a [ViewQueryResultEvent], from a group database with the
     * security data from the fallback db.
     *
     * @param usersFlow a [Flow] of [ViewQueryResultEvent]s of [User] to enhance.
     * @param includeMetadataFromGlobalUser if true the global user information will be used to enhance the user (doing
     * an additional request), else only readily available information are used by the enhancer. This parameter is
     * ignored on lite.
     * @return a [Flow] containing the [ViewQueryResultEvent]s, all the [EnhancedUser]s contained in the events are enhanced
     * with the data from the fallback db.
     */
    suspend fun enhanceViewFlow(
        usersFlow: Flow<ViewQueryResultEvent>,
        includeMetadataFromGlobalUser: Boolean
    ): Flow<ViewQueryResultEvent>
}