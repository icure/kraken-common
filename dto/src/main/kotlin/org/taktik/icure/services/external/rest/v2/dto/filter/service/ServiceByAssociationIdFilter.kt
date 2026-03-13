package org.taktik.icure.services.external.rest.v2.dto.filter.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.embed.ServiceDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches services by their association identifier.
 */
data class ServiceByAssociationIdFilter(
	/** The association identifier to match. */
	val associationId: String,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<ServiceDto>
