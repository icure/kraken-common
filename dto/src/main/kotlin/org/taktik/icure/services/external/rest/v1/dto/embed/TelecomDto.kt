/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """This entity represents available contact details of a user, reachable by telecom methods""")
data class TelecomDto(
	@param:Schema(description = "The type of telecom method being used, ex: landline phone, mobile phone, email, fax, etc.") val telecomType: TelecomTypeDto? = null,
	val telecomNumber: String? = null,
	val telecomDescription: String? = null,
	override val encryptedSelf: String? = null,
) : EncryptableDto,
	Serializable,
	Comparable<TelecomDto> {

	override fun compareTo(other: TelecomDto): Int = telecomType?.compareTo(other.telecomType ?: TelecomTypeDto.other) ?: 0
}
