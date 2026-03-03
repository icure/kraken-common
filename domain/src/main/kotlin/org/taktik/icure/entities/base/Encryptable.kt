package org.taktik.icure.entities.base

interface Encryptable {
	/**
	 * The base64 encoded data of this object, formatted as JSON and encrypted in AES using the random master key from encryptionKeys.
	 */
	val encryptedSelf: String?

	fun solveConflictsWith(other: Encryptable): Map<String, Any?> = mapOf(
		"encryptedSelf" to (this.encryptedSelf ?: other.encryptedSelf),
	)
}
