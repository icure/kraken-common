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
 * Filter that matches calendar items by data owner and lifecycle timestamp range.
 */
data class CalendarItemByDataOwnerLifecycleBetween(
	/** The identifier of the data owner. */
	val dataOwnerId: String,
	/** The start of the lifecycle timestamp range (inclusive). */
	val startTimestamp: Long? = null,
	/** The end of the lifecycle timestamp range (inclusive). */
	val endTimestamp: Long? = null,
	/** Whether to return results in descending order. */
	val descending: Boolean = false,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<CalendarItemDto>
