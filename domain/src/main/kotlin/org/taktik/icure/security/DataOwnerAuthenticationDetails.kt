package org.taktik.icure.security

import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.entities.utils.Sha256HexString

/**
 * Represents the authentication details for a data owner. This class also allows to authenticate
 * data owners "anonymously" in order to perform some entity-related operations by providing only
 * some access control keys instead of actually authenticating the data owner.
 *
 * The rights of the data owner are checked using his details including potential parents if he
 * was not authenticated anonymously, and all the provided [accessControlKeys]. Note that some
 * data owners may have access through some entities only through [accessControlKeys]: if the
 * data owner is identifiable but didn't provide the appropriate [accessControlKeys] he may not
 * be able to retrieve some entities which he should be able to access.
 */
interface DataOwnerAuthenticationDetails {
	/**
	 * Details of the data owner, if the data owner is not performing anonymous authentication.
	 */
	val dataOwner: DataOwnerDetails?

	/**
	 * Decoded access control keys, mandatory in case of anonymous authentication.
	 * The data owner has access to entities where the sha256 hash of at least one of these keys
	 * matches the key of a delegation in the [SecurityMetadata.secureDelegations] of the entity.
	 */
	val accessControlKeys: List<ByteArray>

	/**
	 * Hex representation of the sha256 hash of the [accessControlKeys]. The authenticated data owner has access to all
	 * delegations associated with at least one of these hashes, even if the data owner id is not explicitly indicated
	 * in the delegation.
	 */
	val accessControlKeysHashes: Set<Sha256HexString>

	interface DataOwnerDetails {
		/**
		 * Id of the data owner
		 */
		val id: String

		/**
		 * Type of the data owner
		 */
		val type: DataOwnerType

		/**
		 * Details of the data owner parent, retrieved on request but implementations should cache the result the first
		 * time it is requested in case the retrieval may be costly (e.g. it requires to retrieve data from a database)
		 */
		suspend fun parent(): DataOwnerDetails?

		/**
		 * Returns if the predicate applies to any of the data owners in this data owner hierarchy (this data owner and
		 * all of his parents)
		 */
		suspend fun anyInHierarchy(predicate: suspend (DataOwnerDetails) -> Boolean): Boolean = if (predicate(this)) true else parent()?.anyInHierarchy(predicate) ?: false

		/**
		 * Load ids of all data owners in the hierarchy of this data owner
		 * @return a list containing this data owner and all of his parents ids
		 */
		suspend fun fullHierarchyIds(): List<String> = listOf(id) + (parent()?.fullHierarchyIds() ?: emptyList())
	}
}
