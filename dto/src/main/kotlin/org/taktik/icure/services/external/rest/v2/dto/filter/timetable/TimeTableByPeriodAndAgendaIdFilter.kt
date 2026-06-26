package org.taktik.icure.services.external.rest.v2.dto.filter.timetable

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.TimeTableDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches timetables within a date range for a specific agenda.
 */
data class TimeTableByPeriodAndAgendaIdFilter(
	/** The identifier of the agenda. */
	@ActiveField val agendaId: String,
	/** The start of the date range (inclusive). */
	@ActiveField val startDate: Long? = null,
	/** The end of the date range (inclusive). */
	@ActiveField val endDate: Long? = null,
	/** Whether to return results in descending order. */
	@ActiveField val descending: Boolean? = null,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<TimeTableDto>
