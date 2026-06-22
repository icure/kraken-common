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
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * Note: The RFC-5545 rrule is used only to manage the days of the occurrences. The hours and durations of the appointments are specified in the property .hours.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.embed.TimeTableItemDto")
data class TimeTableItemDto(
	@ActiveField val rruleStartDate: Long? = null, // YYYYMMDD
	/** a RFC-5545 recurrence rule specifying the days and recurrence type of the timetable item. ("RRULE:FREQ=WEEKLY;UNTIL=20220930T150400Z;COUNT=30;INTERVAL=2;WKST=MO;BYDAY=TH" = every 2 weeks on Thursday until 30 September 2022.) */
	@ActiveField val rrule: String? = null,
	@ActiveField val notBeforeInMinutes: Int? = null,
	@ActiveField val notAfterInMinutes: Int? = null,
	@ActiveField val zoneId: String? = null,
	@Deprecated("Will be replaced by rrule") @ActiveField val days: List<String> = emptyList(),
	@Deprecated("Will be replaced by rrule") @ActiveField val recurrenceTypes: List<String> = emptyList(),
	@ActiveField val hours: List<TimeTableHourDto> = emptyList(),
	@ActiveField val calendarItemTypeId: String? = null,
	@ActiveField val homeVisit: Boolean = false,
	@ActiveField val placeId: String? = null,
	@ActiveField val publicTimeTableItem: Boolean = false,
	@param:Schema(defaultValue = "true") @ActiveField val acceptsNewPatient: Boolean = true,
	@ActiveField val unavailable: Boolean = false,
	@param:Schema(description = "A list of the ids of the users who are allowed to create a CalendarItem in this slot.")
	@param:JsonInclude(JsonInclude.Include.NON_EMPTY) @ActiveField val reservingRights: Set<String> = emptySet(),
) : Serializable
