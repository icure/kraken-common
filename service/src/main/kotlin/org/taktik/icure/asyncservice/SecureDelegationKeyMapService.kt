package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.entities.SecureDelegationKeyMap

interface SecureDelegationKeyMapService : EntityWithSecureDelegationsService<SecureDelegationKeyMap> {
	/**
	 * Creates a [SecureDelegationKeyMap].
	 */
	suspend fun createSecureDelegationKeyMap(map: SecureDelegationKeyMap): SecureDelegationKeyMap

	/**
	 * Get all [SecureDelegationKeyMap]s for some given delegation keys.
	 */
	fun findByDelegationKeys(delegationKeys: List<String>): Flow<SecureDelegationKeyMap>
}
