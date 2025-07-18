/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.utils.InstantDeserializer
import org.taktik.icure.utils.InstantSerializer
import java.io.Serializable
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ReferralPeriodDto(
	@JsonSerialize(using = InstantSerializer::class)
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonDeserialize(using = InstantDeserializer::class)
	@Schema(description = "The date (unix epoch in ms) when the referral period initiated, will be filled instantaneously.") val startDate: Instant? = null,
	@JsonSerialize(using = InstantSerializer::class)
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonDeserialize(using = InstantDeserializer::class)
	@Schema(
		description = "The date (unix epoch in ms) the referral period ended, will be instantaneously filled.",
	) val endDate: Instant? = null,
	@Schema(description = "Comments made during the referral.") val comment: String? = null,
) : Serializable,
	Comparable<ReferralPeriodDto> {
	override fun compareTo(other: ReferralPeriodDto): Int = when {
		this == other -> 0
		startDate != other.startDate -> {
			if (startDate == null) {
				1
			} else if (other.startDate == null) {
				0
			} else {
				startDate.compareTo(other.startDate)
			}
		}
		endDate != other.endDate -> {
			if (endDate == null) {
				1
			} else if (other.endDate == null) {
				0
			} else {
				endDate.compareTo(other.endDate)
			}
		}
		else -> 1
	}
}
