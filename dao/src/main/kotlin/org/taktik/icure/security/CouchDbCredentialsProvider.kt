package org.taktik.icure.security

import org.apache.commons.codec.digest.DigestUtils
import java.io.Serializable

/**
 * Provides access to the credentials to use for couchdb connection.
 */
interface CouchDbCredentialsProvider : () -> Pair<String, String> {
	/**
	 * Get the updated username and password credentials to use for the connection to couchdb.
	 */
	fun getCredentials(): UsernamePassword

	override fun invoke(): Pair<String, String> = getCredentials().let { it.username to it.password }
}

/**
 * Represents username-password credentials.
 */
data class UsernamePassword(
	val username: String,
	val password: String,
) : Serializable {
	/**
	 * String representation of this where the `password` value has been replaced by its hash.
	 */
	fun toHashedString(): String = "UsernamePassword($username, ${DigestUtils.sha256Hex(password.toByteArray())}"
}
