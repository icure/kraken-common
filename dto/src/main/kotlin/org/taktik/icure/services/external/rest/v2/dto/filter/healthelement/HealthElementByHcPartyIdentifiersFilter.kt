package org.taktik.icure.services.external.rest.v2.dto.filter.healthelement

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.HealthElementDto
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches health elements by healthcare party and a list of identifiers.
 */
data class HealthElementByHcPartyIdentifiersFilter(
	/** Optional description of this filter. */
	override val desc: String? = null,
	/** The identifier of the healthcare party. */
	val hcPartyId: String,
	/** The list of identifiers to match. */
	val identifiers: List<IdentifierDto> = emptyList(),
) : AbstractFilterDto<HealthElementDto>
