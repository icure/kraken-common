package org.taktik.icure.services.external.rest.v2.dto.filter.device

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.DeviceDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches devices by their responsible healthcare party.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.filter.device.DeviceByHcPartyFilter")
data class DeviceByHcPartyFilter(
	/** Optional description of this filter. */
	override val desc: String? = null,
	/** The identifier of the responsible healthcare party. */
	@ActiveField val responsibleId: String? = null,
) : AbstractFilterDto<DeviceDto>
