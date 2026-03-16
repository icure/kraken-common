package org.taktik.icure.services.external.rest.v2.dto.filter.timetable

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.TimeTableDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches timetables by their associated agenda identifier.
 */
data class TimeTableByAgendaIdFilter(
	/** The identifier of the agenda. */
	val agendaId: String,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<TimeTableDto>
