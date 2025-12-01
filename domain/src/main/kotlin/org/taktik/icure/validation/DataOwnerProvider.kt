package org.taktik.icure.validation

interface DataOwnerProvider {
	suspend fun getCurrentDataOwnerId(): String
	suspend fun getCurrentDataOwnerIdOrNull(): String?
	suspend fun getCurrentUserId(): String

	/**
	 * If the data owner requests anonymity the data owner id or user id should not be automatically included
	 * unencrypted in data created / modified by the data owner.
	 * The default value depends on the data owner type, but the user can change the value using request headers.
	 */
	suspend fun requestsAutofixAnonymity(): Boolean
}
