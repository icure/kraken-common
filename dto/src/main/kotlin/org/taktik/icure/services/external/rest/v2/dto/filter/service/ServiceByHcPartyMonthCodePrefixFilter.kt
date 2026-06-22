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
/**
 * Filter that matches services by healthcare party, year/month, code prefix, and value date range.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByHcPartyMonthCodePrefixFilter")
data class ServiceByHcPartyMonthCodePrefixFilter(
	/** The identifier of the healthcare party. */
	@ActiveField val healthcarePartyId: String,
	/** The year to filter on. */
	@ActiveField val year: Int? = null,
	/** The month to filter on. */
	@ActiveField val month: Int? = null,
	/** The type of the code to match. */
	@ActiveField val codeType: String,
	/** The code prefix to match. */
	@ActiveField val codeCodePrefix: String,
	/** The start of the value date range. */
	@ActiveField val startValueDate: Long? = null,
	/** The end of the value date range. */
	@ActiveField val endValueDate: Long? = null,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<ServiceDto>
