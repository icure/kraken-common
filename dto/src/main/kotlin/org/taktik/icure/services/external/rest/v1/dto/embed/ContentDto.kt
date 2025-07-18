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
data class ContentDto(
	val stringValue: String? = null,
	val numberValue: Double? = null,
	val booleanValue: Boolean? = null,
	@param:JsonSerialize(using = InstantSerializer::class)
	@param:JsonInclude(JsonInclude.Include.NON_NULL)
	@param:JsonDeserialize(using = InstantDeserializer::class)
	val instantValue: Instant? = null,
	@get:Schema(
		description = "Value as date. The format could have a all three (day, month and year) or values on any of these three, whatever is known.",
	) val fuzzyDateValue: Long? = null,
	@get:Schema(type = "string", format = "byte") val binaryValue: ByteArray? = null,
	@get:Schema(description = "Linked document.") val documentId: String? = null,
	@get:Schema(
		description = "Values of measurements recorded. Fields included would be the value, permissible range (min. and max.), severity, unit of measurement, etc ",
	) val measureValue: MeasureDto? = null,
	@get:Schema(description = "The details of prescribed or suggested medication") val medicationValue: MedicationDto? = null,
	@get:Schema(description = "A high frequency time-series containing the ts in ms from the start (double) and the values") val timeSeries: TimeSeriesDto? = null,
	val compoundValue: List<ServiceDto>? = null,
	val ratio: List<MeasureDto>? = null,
	val range: List<MeasureDto>? = null,
) : Serializable {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is ContentDto) return false

		if (stringValue != other.stringValue) return false
		if (numberValue != other.numberValue) return false
		if (booleanValue != other.booleanValue) return false
		if (instantValue != other.instantValue) return false
		if (fuzzyDateValue != other.fuzzyDateValue) return false
		if (binaryValue != null) {
			if (other.binaryValue == null) return false
			if (!binaryValue.contentEquals(other.binaryValue)) return false
		} else if (other.binaryValue != null) {
			return false
		}
		if (documentId != other.documentId) return false
		if (measureValue != other.measureValue) return false
		if (medicationValue != other.medicationValue) return false
		if (compoundValue != other.compoundValue) return false
		if (timeSeries != other.timeSeries) return false

		if (ratio != other.ratio) return false
		if (range != other.range) return false

		return true
	}

	override fun hashCode(): Int {
		var result = stringValue?.hashCode() ?: 0
		result = 31 * result + (numberValue?.hashCode() ?: 0)
		result = 31 * result + (booleanValue?.hashCode() ?: 0)
		result = 31 * result + (instantValue?.hashCode() ?: 0)
		result = 31 * result + (fuzzyDateValue?.hashCode() ?: 0)
		result = 31 * result + (binaryValue?.contentHashCode() ?: 0)
		result = 31 * result + (documentId?.hashCode() ?: 0)
		result = 31 * result + (measureValue?.hashCode() ?: 0)
		result = 31 * result + (medicationValue?.hashCode() ?: 0)
		result = 31 * result + (compoundValue?.hashCode() ?: 0)
		return result
	}
}
