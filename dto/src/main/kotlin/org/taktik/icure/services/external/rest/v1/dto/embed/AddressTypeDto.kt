/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import java.io.Serializable

enum class AddressTypeDto : Serializable {
	home,
	work,
	vacation,
	hospital,
	clinic,
	hq,
	other,
	temporary,
	postal,
	diplomatic,
	reference,
	careaddress,
}
