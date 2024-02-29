/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto

import org.taktik.icure.services.external.rest.v1.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v1.dto.embed.ServiceDto

class ResultInfoDto(
	val ssin: String? = null,
	val lastName: String? = null,
	val firstName: String? = null,
	val dateOfBirth: Long? = null,
	val sex: String? = null,
	val documentId: String? = null,
	val protocol: String? = null,
	val complete: Boolean? = null,
	val demandDate: Long? = null,
	val labo: String? = null,
	val engine: String? = null,
	val codes: Set<CodeStubDto> = emptySet(),
	val services: List<ServiceDto>? = null
)
