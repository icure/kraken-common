/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import org.taktik.icure.entities.base.EnumVersion
import java.io.Serializable

/**
 * Created by aduchate on 21/01/13, 14:53
 */
@EnumVersion(1L)
enum class AddressType : Serializable {
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
