/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto

/**
 * @author Bernard Paulus on 23/05/17.
 */
class WsExceptionDto(
	val level: String,
	val error: String,
	val translations: Map<String, String>,
)
