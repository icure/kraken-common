package org.taktik.icure.services.external.rest.v2.dto.filter.calendarItem

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.CalendarItemDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches calendar items within a time period for a specific data owner.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.filter.calendarItem.CalendarItemByPeriodAndDataOwnerIdFilter")
data class CalendarItemByPeriodAndDataOwnerIdFilter(
	/** The identifier of the data owner. */
	@ActiveField val dataOwnerId: String,
	/** The start of the time period. */
	@ActiveField val startTime: Long,
	/** The end of the time period. */
	@ActiveField val endTime: Long,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<CalendarItemDto>
