package org.taktik.icure.security.jwt

import org.springframework.security.core.GrantedAuthority
import org.taktik.icure.entities.DataOwnerType

const val DATA_OWNER_ID = "doId"
const val DATA_OWNER_TYPE = "doTp"

/**
 * Hierarchy claim: flat list of the ids of all the (transitive) ancestor data owner groups of the data owner,
 * deduplicated, unordered, excluding the data owner itself (which is in [DATA_OWNER_ID]). Omitted when empty.
 * Tokens issued before multi-parent support wrote the same wire shape (the linear parentId chain, topmost ancestor
 * first): they parse identically, only the order semantics were relaxed.
 */
const val HCP_HIERARCHY = "hh"
const val AUTHORITIES = "a"

interface JwtDetails : Jwt {
	val dataOwnerId: String?
	val dataOwnerType: DataOwnerType?
	val hcpHierarchyIds: Set<String>
	val authorities: Set<GrantedAuthority>
}
