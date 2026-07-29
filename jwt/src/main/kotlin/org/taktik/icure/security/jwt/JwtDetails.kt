package org.taktik.icure.security.jwt

import org.springframework.security.core.GrantedAuthority
import org.taktik.icure.entities.DataOwnerType

const val DATA_OWNER_ID = "doId"
const val DATA_OWNER_TYPE = "doTp"

/**
 * Hierarchy claim: flat list of the ids of all the (transitive) ancestor data owner groups of the data owner,
 * deduplicated, unordered, excluding the data owner itself (which is in [DATA_OWNER_ID]). Omitted when empty.
 *
 * Each entry encodes the rights the ancestor group grants:
 * - a bare id is a parent group ([hcpHierarchyIds]): the data owner is reachable exclusively through `parent`-type
 *   links, which grant administrative rights;
 * - an id prefixed with [HCP_HIERARCHY_OTHER_PREFIX] is a group providing membership only ([hcpOtherGroupIds]).
 *
 * The prefix is a character forbidden in entity ids (see `EntityIdValidation`), which makes the encoding safe
 * against downgrade attacks: a token issued by a kraken aware of the distinction and replayed on an older kraken
 * that treats every `hh` entry as a parent can never match a real data owner id with a prefixed entry, so
 * membership-only groups can fail closed but never escalate to parent rights.
 *
 * Tokens issued before multi-parent support wrote the linear parentId chain (topmost ancestor first) with bare ids:
 * they parse identically, as parent groups only, with relaxed order semantics.
 */
const val HCP_HIERARCHY = "hh"

/**
 * Prefix marking an [HCP_HIERARCHY] entry as a membership-only group (see [HCP_HIERARCHY]). Must be a character
 * forbidden in entity ids by `EntityIdValidation`.
 */
const val HCP_HIERARCHY_OTHER_PREFIX = "/"
const val AUTHORITIES = "a"

interface JwtDetails : Jwt {
	val dataOwnerId: String?
	val dataOwnerType: DataOwnerType?

	/**
	 * Ids of the (transitive) ancestor data owner groups of the data owner reachable exclusively through
	 * `parent`-type links (including the legacy parentId), deduplicated, excluding the data owner itself.
	 * These are the groups that grant administrative (parent) rights over the data owner.
	 */
	val hcpHierarchyIds: Set<String>

	/**
	 * Ids of the (transitive) ancestor data owner groups of the data owner whose every path from the data owner
	 * includes at least one `other`-type link, deduplicated, disjoint from [hcpHierarchyIds]. These groups provide
	 * membership for data sharing purposes but never grant administrative rights.
	 */
	val hcpOtherGroupIds: Set<String> get() = emptySet()
	val authorities: Set<GrantedAuthority>
}
