/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

/**
 * Created by aduchate on 21/01/13, 14:47
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """This entity represents available contact details of a user, reachable by telecom methods""")
data class TelecomDto(
	@Schema(description = "The type of telecom method being used, ex: landline phone, mobile phone, email, fax, etc.") val telecomType: TelecomTypeDto? = null,
	val telecomNumber: String? = null,
	val telecomDescription: String? = null,
	override val encryptedSelf: String? = null,
) : EncryptableDto,
	Serializable,
	Comparable<TelecomDto> {
	companion object : DynamicInitializer<TelecomDto>

	fun merge(other: TelecomDto) = TelecomDto(args = this.solveConflictsWith(other))

	fun solveConflictsWith(other: TelecomDto) = super.solveConflictsWith(other) +
		mapOf(
			"telecomType" to (this.telecomType ?: other.telecomType),
			"telecomNumber" to (this.telecomNumber ?: other.telecomNumber),
			"telecomDescription" to (this.telecomDescription ?: other.telecomDescription),
		)

	override fun compareTo(other: TelecomDto): Int = telecomType?.compareTo(other.telecomType ?: TelecomTypeDto.other) ?: 0
}
