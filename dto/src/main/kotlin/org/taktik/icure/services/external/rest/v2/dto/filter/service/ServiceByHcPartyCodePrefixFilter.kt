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

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches services by healthcare party and code prefix.
 */
data class ServiceByHcPartyCodePrefixFilter(
	/** The identifier of the healthcare party. */
	val healthcarePartyId: String,
	/** The type of the code to match. */
	val codeType: String,
	/** The code prefix to match. */
	val codeCodePrefix: String,
	/** Optional description of this filter. */
	override val desc: String? = null,
	/** Optional start of a range of date for the value date of the service */
	val startValueDate: Long? = null,
	/** Optional end of a range of date for the value date of the service */
	val endValueDate: Long? = null,
	) : AbstractFilterDto<ServiceDto>
