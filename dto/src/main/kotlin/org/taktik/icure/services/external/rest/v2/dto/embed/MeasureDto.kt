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
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a measured value with its unit, reference value, severity, evolution, and reference ranges.
 */
data class MeasureDto(
	/** The measured numeric value. */
	val value: Double? = null,
	/** The reference value for comparison. */
	val ref: Double? = null,
	/** The severity level as an integer. */
	val severity: Int? = null,
	/** The severity code as a string. */
	val severityCode: String? = null,
	/** The evolution indicator as an integer. */
	val evolution: Int? = null,
	/** The unit of measurement as a string. */
	val unit: String? = null,
	/** The coded units of measurement. */
	val unitCodes: Set<CodeStubDto>? = null,
	/** A comment about the measurement. */
	val comment: String? = null,
	/** A comparator string (e.g., "<", ">", "<="). */
	val comparator: String? = null,
	/** The sign of the value. */
	val sign: String? = null,
	/** The list of reference ranges for this measurement. */
	val referenceRanges: List<ReferenceRangeDto> = emptyList(),
	/** The value with its precision information. */
	val valueWithPrecision: ValueWithPrecisionDto? = null,
) : Serializable
