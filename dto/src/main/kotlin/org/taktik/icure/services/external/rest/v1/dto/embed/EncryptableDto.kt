/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.embed

import io.swagger.v3.oas.annotations.media.Schema

interface EncryptableDto {
	@param:Schema(
		description = "The base64 encoded data of this object, formatted as JSON and encrypted in AES using the random master key from encryptionKeys.",
	)
	val encryptedSelf: String?

	fun solveConflictsWith(other: EncryptableDto) = mapOf(
		"encryptedSelf" to (this.encryptedSelf ?: other.encryptedSelf),
	)
}
