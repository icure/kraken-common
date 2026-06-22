package org.taktik.icure.services.external.rest.v2.dto.filter.agenda

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.AgendaDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches agendas readable by a specific user.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.filter.agenda.AgendaReadableByUserIdFilter")
data class AgendaReadableByUserIdFilter(
	/** The identifier of the user who has read access to the agendas. */
	@ActiveField val userId: String,
	/** Optional description of this filter. */
	override val desc: String?,
) : AbstractFilterDto<AgendaDto>
