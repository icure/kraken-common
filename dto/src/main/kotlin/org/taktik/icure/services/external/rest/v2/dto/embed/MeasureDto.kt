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
import org.taktik.icure.ExposedToCustomEntities
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a measured value with its unit, reference value, severity, evolution, and reference ranges.
 */
@ExposedToCustomEntities
data class MeasureDto(
	/** The measured numeric value. */
	@ActiveField val value: Double? = null,
	/** The reference value for comparison. */
	@ActiveField val ref: Double? = null,
	/** The severity level as an integer. */
	@ActiveField val severity: Int? = null,
	/** The severity code as a string. */
	@ActiveField val severityCode: String? = null,
	/** The evolution indicator as an integer. */
	@ActiveField val evolution: Int? = null,
	/** The unit of measurement as a string. */
	@ActiveField val unit: String? = null,
	/** The coded units of measurement. */
	@ActiveField val unitCodes: Set<CodeStubDto>? = null,
	/** A comment about the measurement. */
	@ActiveField val comment: String? = null,
	/** A comparator string (e.g., "<", ">", "<="). */
	@ActiveField val comparator: String? = null,
	/** The sign of the value. */
	@ActiveField val sign: String? = null,
	/** The list of reference ranges for this measurement. */
	@ActiveField val referenceRanges: List<ReferenceRangeDto> = emptyList(),
	/** The value with its precision information. */
	@ActiveField val valueWithPrecision: ValueWithPrecisionDto? = null,
) : Serializable
