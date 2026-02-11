/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.taktik.icure.utils.InstantDeserializer
import org.taktik.icure.utils.InstantSerializer
import java.io.Serializable
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * A period during which this patient is under the care of a hcp
 */
data class ReferralPeriod(
	@param:JsonSerialize(using = InstantSerializer::class)
	@param:JsonInclude(JsonInclude.Include.NON_NULL)
	@param:JsonDeserialize(using = InstantDeserializer::class)
	val startDate: Instant? = null,

	@param:JsonSerialize(using = InstantSerializer::class)
	@param:JsonInclude(JsonInclude.Include.NON_NULL)
	@param:JsonDeserialize(using = InstantDeserializer::class)
	val endDate: Instant? = null,

	val comment: String? = null,
) : Serializable,
	Comparable<ReferralPeriod> {

	override fun compareTo(other: ReferralPeriod): Int = when {
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
