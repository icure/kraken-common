/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.base

/**
 * An interface for root entities that can be extended with additional custom fields.
 */
interface ExtendableRoot : Extendable {
	val extensionsVersion: Int?
}
