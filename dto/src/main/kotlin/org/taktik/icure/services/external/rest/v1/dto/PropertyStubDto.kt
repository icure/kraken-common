/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v1.dto.embed.EncryptableDto
import org.taktik.icure.services.external.rest.v1.dto.embed.TypedValueDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PropertyStubDto(
	val id: String? = null,
	val type: PropertyTypeStubDto? = null,
	val typedValue: TypedValueDto<*>? = null,
	@Deprecated("Remove from list instead") val deletionDate: Long? = null,
	override val encryptedSelf: String? = null
) : EncryptableDto
