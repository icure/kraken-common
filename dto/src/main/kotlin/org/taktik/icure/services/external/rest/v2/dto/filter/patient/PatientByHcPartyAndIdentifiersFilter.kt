package org.taktik.icure.services.external.rest.v2.dto.filter.patient

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.PatientDto
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches patients by healthcare party and a list of identifiers.
 */
data class PatientByHcPartyAndIdentifiersFilter(
	/** The identifier of the healthcare party. */
	val healthcarePartyId: String? = null,
	/** The list of identifiers to match. */
	val identifiers: List<IdentifierDto> = emptyList(),
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<PatientDto>
