package org.taktik.icure.security.jwt

import org.springframework.security.core.GrantedAuthority
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.security.IdWithHierarchy

const val DATA_OWNER_ID = "doId"
const val DATA_OWNER_TYPE = "doTp"

/**
 * Legacy single-parent hierarchy claim (a list of ids, topmost ancestor first, direct parent last). Only read for
 * backward compatibility with tokens issued before [HCP_HIERARCHIES] was introduced, never written anymore.
 */
const val HCP_HIERARCHY = "hh"
const val HCP_HIERARCHIES = "hhh"
const val AUTHORITIES = "a"

interface JwtDetails : Jwt {
	val dataOwnerId: String?
	val dataOwnerType: DataOwnerType?
	val hcpHierarchies: List<IdWithHierarchy>
	val authorities: Set<GrantedAuthority>
}
