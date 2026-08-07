/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.base

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * A link to another data owner representing a group. The type of the link (whether it grants
 * administrative rights, is membership-only, or is not allowed at all) is no longer declared here —
 * it is intrinsic to the linked data owner itself, see [CryptoActor.groupLinkType] and
 * [CryptoActor.effectiveGroupLinkType].
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class DataOwnerGroupLink(
	val dataOwnerId: String,
)
