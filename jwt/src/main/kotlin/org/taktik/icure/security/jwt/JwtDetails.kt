package org.taktik.icure.security.jwt

import org.taktik.icure.entities.DataOwnerType

const val DATA_OWNER_ID = "doId"
const val DATA_OWNER_TYPE = "doTp"
const val HCP_HIERARCHY = "hh"

interface JwtDetails : Jwt {
    val dataOwnerId: String?
    val dataOwnerType: DataOwnerType?
    val hcpHierarchy: List<String>
}