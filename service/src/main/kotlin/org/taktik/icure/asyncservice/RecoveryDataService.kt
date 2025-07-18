package org.taktik.icure.asyncservice

import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.entities.RecoveryData

/**
 * Service to manage recovery data.
 *
 * Note the absence of "find" methods: recovery data is useful only when you know the encryption key, and the encryption
 * key + recipient id is enough to find the recovery data.
 */
interface RecoveryDataService {
	/**
	 * Create some recovery data.
	 */
	suspend fun createRecoveryData(recoveryData: RecoveryData): RecoveryData

	/**
	 * Get some recovery data.
	 */
	suspend fun getRecoveryData(id: String): RecoveryData?

	// No need for update

	/**
	 * Delete some recovery data
	 */
	suspend fun purgeRecoveryData(id: String): DocIdentifier

	/**
	 * Deletes all recovery data of a given recipient.
	 * @return the amount of recovery data that has been deleted
	 */
	suspend fun deleteAllRecoveryDataForRecipient(recipientId: String): Int

	/**
	 * Deletes all recovery data of a given type for a given recipient.
	 * @return the amount of recovery data that has been deleted
	 */
	suspend fun deleteAllRecoveryDataOfTypeForRecipient(type: RecoveryData.Type, recipientId: String): Int

	/**
	 * Gets recovery data if it exists, or waits for up to [waitSeconds] for it to be created before returning.
	 */
	suspend fun waitForRecoveryData(id: String, waitSeconds: Int): RecoveryData?
}
