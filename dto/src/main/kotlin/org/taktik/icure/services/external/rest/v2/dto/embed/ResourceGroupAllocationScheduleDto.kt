package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents an allocation schedule for a resource group, defining time table items within a date range.
 */
data class ResourceGroupAllocationScheduleDto(
	/** When representing different resource groups in a single agenda you need to provide a (custom) code stub to distinguish them. Different ResourceGroupAllocationSchedule in a single agenda for the same resourceGroup can't be active at overlapping times. Note that if any of the items are public this will also be public. */
	val resourceGroup: CodeStubDto? = null,
	/** Tags associated with this schedule. */
	val tags: Set<CodeStubDto> = emptySet(),
	/** Codes associated with this schedule. */
	val codes: Set<CodeStubDto> = emptySet(),
	/** Can be used for human-readable name to help identify the schedule. Note that if any of the items are public this will also be public. */
// 	val medicalLocationId: String? = null,
	val name: String? = null,
	/** The date and time when the schedule starts being valid / available, as a fuzzy date time. If null the schedule is going to be considered always valid until [endDateTime]. If both are null the schedule is considered always valid */
	val startDateTime: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	/** The date and time when the schedule ends being valid / available, as a fuzzy date time. If null the schedule is going to be considered always valid since [startDateTime]. If both are null the schedule is considered always valid */
	val endDateTime: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	/** Details of the availabilities for this schedule. The availabilities may be truncated by the startTime or endTime. */
	val items: List<EmbeddedTimeTableItemDto> = emptyList(),
) : Serializable
