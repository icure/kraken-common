/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import org.taktik.icure.entities.base.EnumVersion

@EnumVersion(1L)
enum class Visibility {
	maskedfromsummary,
	maskedfromexportedfile,
	proeminent,
	highlighted,
	visible,
}
