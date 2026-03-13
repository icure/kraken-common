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
 * Filter that matches calendar items within a time period for a specific agenda.
 */
data class CalendarItemByPeriodAndAgendaIdFilter(
	/** The identifier of the agenda. */
	val agendaId: String,
	/** The start of the time period. */
	val startTime: Long,
	/** The end of the time period. */
	val endTime: Long,
	/** Whether to return results in descending order. */
	val descending: Boolean? = null,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<CalendarItemDto>
