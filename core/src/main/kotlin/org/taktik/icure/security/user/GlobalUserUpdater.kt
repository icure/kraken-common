package org.taktik.icure.security.user

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.entities.User

interface GlobalUserUpdater {
	/**
	 * Does any required global user updates then returns [updatedUser]
	 */
	suspend fun tryUpdate(updatedUser: User): User

	/**
	 * Does any required global user updates as the returned flow is consumed.
	 * Consumes [updatedUsers] and emits the same results.
	 */
	fun tryingUpdates(updatedUsers: Flow<User>): Flow<User>

	/**
	 * Purges the global user corresponding to a purged local user
	 */
	suspend fun tryPurge(
		localId: String,
		localRev: String,
	)

	suspend fun tryPurge(userIds: List<IdAndRev>)
}
