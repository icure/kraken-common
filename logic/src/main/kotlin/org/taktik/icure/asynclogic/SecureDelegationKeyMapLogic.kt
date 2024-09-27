package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.entities.SecureDelegationKeyMap

interface SecureDelegationKeyMapLogic :
    EntityWithSecureDelegationsLogic<SecureDelegationKeyMap>,
    EntityPersister<SecureDelegationKeyMap> {
    /**
     * Creates a [SecureDelegationKeyMap].
     */
    suspend fun createSecureDelegationKeyMap(map: SecureDelegationKeyMap): SecureDelegationKeyMap

    /**
     * Get all [SecureDelegationKeyMap]s for some given delegation key.
     */
    fun findByDelegationKeys(delegationKeys: List<String>): Flow<SecureDelegationKeyMap>
}