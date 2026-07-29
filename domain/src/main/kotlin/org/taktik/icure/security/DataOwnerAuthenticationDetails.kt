package org.taktik.icure.security

import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.base.DataOwnerGroupLinkType
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
		 * Ids of all the (transitive) ancestor data owner groups of this data owner, excluding [id] itself. Retrieved
		 * on request but implementations should cache the result the first time it is requested in case the retrieval
		 * may be costly (e.g. it requires to retrieve data from a database).
		 * This includes all links, regardless of [DataOwnerGroupLinkType]
		 */
		suspend fun ancestorIds(): Set<String>

		/**
		 * Subset of [ancestorIds] limited to the ancestor data owner groups reachable exclusively through
		 * [DataOwnerGroupLinkType.parent] links (including the legacy parentId). Only these groups grant
		 * administrative (parent) rights over this data owner; the remaining [ancestorIds] provide group
		 * membership only.
		 */
		suspend fun parentIds(): Set<String>

		/**
		 * [id] plus [ancestorIds]: the ids of all the data owners in the hierarchy of this data owner.
		 */
		suspend fun hierarchyIds(): Set<String> = buildSet {
			add(id)
			addAll(ancestorIds())
		}
	}
}
