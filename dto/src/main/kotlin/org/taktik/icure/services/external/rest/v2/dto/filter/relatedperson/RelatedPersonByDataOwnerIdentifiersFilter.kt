package org.taktik.icure.services.external.rest.v2.dto.filter.relatedperson

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.dto.annotations.filtering.ActiveField
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.RelatedPersonDto
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches related persons with a delegation for a data owner and at least one of the provided identifiers.
 */
data class RelatedPersonByDataOwnerIdentifiersFilter(
	/** Optional description of this filter. */
	override val desc: String? = null,
	/** The identifier of the data owner. When null, the current data owner is used. */
	@ActiveField val dataOwnerId: String? = null,
	/** The list of identifiers to match. */
	@ActiveField val identifiers: List<IdentifierDto> = emptyList(),
) : AbstractFilterDto<RelatedPersonDto>
