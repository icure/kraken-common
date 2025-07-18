/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import org.taktik.icure.services.external.rest.v1.dto.base.EnumVersionDto

/**
 * Created by aduchate on 21/01/13, 15:38
 */
@EnumVersionDto(1L)
enum class InsuranceParameterDto {
	status,
	tc1,
	tc2,
	preferentialstatus,
	chronicaldisease,
	paymentapproval,
	mdaInputReference,
}
