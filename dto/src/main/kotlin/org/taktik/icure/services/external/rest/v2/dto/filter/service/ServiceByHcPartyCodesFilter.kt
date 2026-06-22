/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v2.dto.filter.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.embed.ServiceDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByHcPartyCodesFilter")
data class ServiceByHcPartyCodesFilter(
	@ActiveField val healthcarePartyId: String,
	@ActiveField val codeCodes: Map<String, Collection<String>>,
	@ActiveField val startValueDate: Long? = null,
	@ActiveField val endValueDate: Long? = null,
	override val desc: String? = null,
) : AbstractFilterDto<ServiceDto>
