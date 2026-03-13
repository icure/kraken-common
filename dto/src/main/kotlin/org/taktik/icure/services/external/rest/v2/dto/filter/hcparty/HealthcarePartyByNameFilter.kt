/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.dto.filter.hcparty

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.HealthcarePartyDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches healthcare parties by name.
 */
data class HealthcarePartyByNameFilter(
	/** Optional description of this filter. */
	override val desc: String? = null,
	/** The name to search for. */
	@param:Schema(required = true)
	val name: String,
	/** Whether to return results in descending order. */
	val descending: Boolean? = null,
) : AbstractFilterDto<HealthcarePartyDto>
