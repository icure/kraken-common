/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto

data class IdWithRevDto(
	val id: String,
	val rev: String? = null,
)
