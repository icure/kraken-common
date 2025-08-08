/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import java.io.Serializable

/**
 * Created by aduchate on 21/01/13, 14:53
 */
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
