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
 * Represents an allocation schedule for a resource group, defining time table items within a date range.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.embed.ResourceGroupAllocationScheduleDto")
data class ResourceGroupAllocationScheduleDto(
	/** The code identifying the resource group. */
	@ActiveField val resourceGroup: CodeStubDto? = null,
	/** Tags associated with this schedule. */
	@ActiveField val tags: Set<CodeStubDto> = emptySet(),
	/** Codes associated with this schedule. */
	@ActiveField val codes: Set<CodeStubDto> = emptySet(),
// 	val medicalLocationId: String? = null,
	/** The name of this allocation schedule. */
	@ActiveField val name: String? = null,
	/** The start date-time in YYYYMMDDHHMMSS format. */
	@ActiveField val startDateTime: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	/** The end date-time in YYYYMMDDHHMMSS format. */
	@ActiveField val endDateTime: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	/** The list of time table items in this schedule. */
	@ActiveField val items: List<EmbeddedTimeTableItemDto> = emptyList(),
) : Serializable
