/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.base

import org.taktik.icure.entities.RawJson

/**
 * An interface for entities that can be extended with additional custom fields.
 */
interface Extendable {
	val extensions: RawJson.JsonObject?
}
