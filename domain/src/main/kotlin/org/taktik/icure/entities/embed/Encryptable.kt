/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import org.taktik.icure.mergers.annotations.NonMergeable

interface Encryptable {
	@NonMergeable val encryptedSelf: String?

	fun solveConflictsWith(other: Encryptable) = mapOf(
		"encryptedSelf" to (this.encryptedSelf ?: other.encryptedSelf),
	)
}
