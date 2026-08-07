package org.taktik.icure.services.external.rest.v2.dto.filter.hcparty

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.dto.annotations.filtering.ActiveField
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.HealthcarePartyDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches the healthcare parties directly linked to a data owner group, through the legacy parentId
 * or a dataOwnerGroups link. Only direct links match: membership is not propagated through the group hierarchies.
 */
data class HealthcarePartyByDataOwnerGroupIdFilter(
	/** The identifier of the data owner representing the group. */
	@ActiveField val dataOwnerGroupId: String,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<HealthcarePartyDto>
