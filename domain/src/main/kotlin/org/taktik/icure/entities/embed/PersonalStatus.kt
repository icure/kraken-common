/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import org.taktik.icure.entities.base.EnumVersion
import java.io.Serializable

@EnumVersion(1L)
enum class PersonalStatus : Serializable {
	single,
	in_couple,
	married,
	separated,
	divorced,
	divorcing,
	widowed,
	widower,
	complicated,
	unknown,
	contract,
	other,
	annulled,
	polygamous,
}
