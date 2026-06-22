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
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a single item in a medication regimen, defining when and how much medication should be administered.
 * The day can be specified as a specific date, day number in treatment, or weekday. The time can be a day period or exact time.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.embed.RegimenItemDto")
data class RegimenItemDto(
	// Day definition (One and only one of the three following should be not null)
	// The three are null if it applies to every day
	/** A specific date (yyyyMMdd) for this regimen item. */
	@ActiveField val date: Long? = null, // yyyymmdd at this date
	/** The day number in the treatment (1-based). */
	@ActiveField val dayNumber: Int? = null, // day 1 of treatment. 1 based numeration
	/** The weekday for this regimen item. */
	@ActiveField val weekday: Weekday? = null, // on monday
	// Time of day definition (One and only one of the three following should be not null)
	// Both are null if not specified
	/** The period of the day (CD-DAYPERIOD) for administration. */
	@ActiveField val dayPeriod: CodeStubDto? = null, // CD-DAYPERIOD
	/** The time of day (hhmmss) for administration. */
	@ActiveField val timeOfDay: Long? = null, // hhmmss 103010
	/** The quantity to administer. */
	@ActiveField val administratedQuantity: AdministrationQuantity? = null,
) : Serializable
