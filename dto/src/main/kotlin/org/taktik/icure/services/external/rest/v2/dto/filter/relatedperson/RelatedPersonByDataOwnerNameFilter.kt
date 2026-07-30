package org.taktik.icure.services.external.rest.v2.dto.filter.relatedperson

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.dto.annotations.filtering.ActiveField
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.RelatedPersonDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches related persons with a delegation for a data owner, where the concatenation of last name and
 * first name contains the provided (sanitized) name.
 */
data class RelatedPersonByDataOwnerNameFilter(
	/** Optional description of this filter. */
	override val desc: String? = null,
	/** The name to search. */
	@ActiveField val name: String? = null,
	/** The identifier of the data owner. When null, the current data owner is used. */
	@ActiveField val dataOwnerId: String? = null,
) : AbstractFilterDto<RelatedPersonDto>
