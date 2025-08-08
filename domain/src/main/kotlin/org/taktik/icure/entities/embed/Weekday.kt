/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import org.taktik.icure.entities.base.CodeStub
import java.io.Serializable

data class Weekday(
	val weekday: CodeStub? = null, // CD-WEEKDAY
	val weekNumber: Int? = null, // Can be null
) : Serializable
