/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.base

import org.taktik.icure.entities.embed.Identifier

/**
 *
 * @property identifier identifiers that identify uniquely and unambiguously a particular instance of this entity.
 *
 */
interface HasIdentifier {
	val identifier: List<Identifier>
}
