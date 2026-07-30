/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.dto.base

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.taktik.icure.dto.annotations.filtering.ActiveField
import org.taktik.icure.services.external.rest.v2.dto.base.DataOwnerGroupLinkTypeDto.simple

/**
 * A link from a crypto actor to a data owner that represents a group it belongs to.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class DataOwnerGroupLinkDto(
	/** The nature of the link. Membership propagates transitively through all link types. */
	@ActiveField val linkType: DataOwnerGroupLinkTypeDto = simple,
	/** The id of the data owner representing the group. */
	@ActiveField val dataOwnerId: String,
)
