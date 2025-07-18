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
data class CalendarItemByDataOwnerLifecycleBetween(
	val dataOwnerId: String,
	val startTimestamp: Long? = null,
	val endTimestamp: Long? = null,
	val descending: Boolean = false,
	override val desc: String? = null,
) : AbstractFilterDto<CalendarItemDto>
