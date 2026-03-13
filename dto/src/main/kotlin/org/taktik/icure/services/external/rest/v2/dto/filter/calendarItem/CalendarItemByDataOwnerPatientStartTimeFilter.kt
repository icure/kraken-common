package org.taktik.icure.services.external.rest.v2.dto.filter.calendarItem

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.CalendarItemDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches calendar items by data owner, patient, and start time range.
 */
data class CalendarItemByDataOwnerPatientStartTimeFilter(
	/** The identifier of the data owner. */
	val dataOwnerId: String,
	/** The start of the date range (inclusive). */
	val startDate: Long?,
	/** The end of the date range (inclusive). */
	val endDate: Long?,
	/** The set of secret patient identifiers used for secure delegation matching. */
	val secretPatientIds: Set<String>,
	/** Whether to return results in descending order. */
	val descending: Boolean?,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<CalendarItemDto>
