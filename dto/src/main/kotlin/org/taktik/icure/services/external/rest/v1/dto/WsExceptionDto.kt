/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto

class WsExceptionDto(
	val level: String,
	val error: String,
	val translations: Map<String, String>,
)
