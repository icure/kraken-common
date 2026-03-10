/*
 *  iCure Data Stack. Copyright (c) 2020 Taktik SA
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public
 *     License along with this program.  If not, see
 *     <https://www.gnu.org/licenses/>.
 */
package org.taktik.icure.services.external.rest.v2.dto.embed

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
/**
 * Represents the value content of a medical service. A content can hold different types of values
 * such as strings, numbers, dates, measurements, medications, time series, or compound sub-services.
 */
data class ContentDto(
	/** A string value. */
	val stringValue: String? = null,
	/** A numeric value. */
	val numberValue: Double? = null,
	/** A boolean value. */
	val booleanValue: Boolean? = null,
	/** An instant value. */
	@param:JsonSerialize(using = InstantSerializer::class)
	@param:JsonInclude(JsonInclude.Include.NON_NULL)
	@param:JsonDeserialize(using = InstantDeserializer::class)
	val instantValue: Instant? = null,
	@param:Schema(
		description = "Value as date. The format could have a all three (day, month and year) or values on any of these three, whatever is known.",
	/** Value as a fuzzy date, which may contain day, month, and/or year components. */
	) val fuzzyDateValue: Long? = null,
	/** A binary value encoded as a byte array. */
	@param:Schema(type = "string", format = "byte") val binaryValue: ByteArray? = null,
	/** The identifier of a linked document. */
	@param:Schema(description = "Linked document.") val documentId: String? = null,
	@param:Schema(
		description = "Values of measurements recorded. Fields included would be the value, permissible range (min. and max.), severity, unit of measurement, etc ",
	/** Values of measurements recorded, including value, range, severity, and unit. */
	) val measureValue: MeasureDto? = null,
	/** The details of prescribed or suggested medication. */
	@param:Schema(description = "The details of prescribed or suggested medication") val medicationValue: MedicationDto? = null,
	/** A high frequency time-series containing timestamps in ms and their values. */
	@param:Schema(description = "A high frequency time-series containing the ts in ms from the start (double) and the values") val timeSeries: TimeSeriesDto? = null,
	/** A list of sub-services forming a compound value. */
	val compoundValue: List<ServiceDto>? = null,
	/** A list of measures representing a ratio. */
	val ratio: List<MeasureDto>? = null,
	/** A list of measures representing a range. */
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
