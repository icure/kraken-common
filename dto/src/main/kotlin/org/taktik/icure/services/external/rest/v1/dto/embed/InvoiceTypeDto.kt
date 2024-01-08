/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import org.taktik.icure.services.external.rest.v1.dto.base.EnumVersionDto

@EnumVersionDto(1L)
enum class InvoiceTypeDto {
	patient, mutualfund, payingagency, //CPAS, complementary assurances...
	insurance, //Deprecated, see MediumTypeDto
	efact, //Deprecated, see MediumTypeDto
	other //Deprecated
}
