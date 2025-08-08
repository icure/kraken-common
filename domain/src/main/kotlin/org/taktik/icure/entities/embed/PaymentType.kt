/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import org.taktik.icure.entities.base.EnumVersion

/**
 * Created by aduchate on 21/01/13, 15:59
 */
@EnumVersion(1L)
enum class PaymentType {
	cash,
	wired,
	insurance,
	creditcard,
	debitcard,
	paypal,
	bitcoin,
	other,
}
