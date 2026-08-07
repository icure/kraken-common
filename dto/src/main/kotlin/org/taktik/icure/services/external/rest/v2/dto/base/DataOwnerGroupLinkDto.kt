/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.dto.base

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * A link from a crypto actor to a data owner that represents a group it belongs to. The type of the link is not
 * declared here: it is intrinsic to the linked data owner itself, see [CryptoActorDto.groupLinkType].
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class DataOwnerGroupLinkDto(
	/** The id of the data owner representing the group. */
	@ActiveField val dataOwnerId: String,
)
