/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import org.taktik.icure.services.external.rest.v1.dto.base.EnumVersionDto

/**
 * Created by aduchate on 21/01/13, 15:59
 */
@Suppress("EnumEntryName")
@EnumVersionDto(1L)
enum class PaymentTypeDto {
	cash,
	wired,
	insurance,
	creditcard,
	debitcard,
	paypal,
	bitcoin,
	other,
}
