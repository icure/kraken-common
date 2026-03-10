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
 * Filter that matches services by healthcare party and tag prefix.
 */
data class ServiceByHcPartyTagPrefixFilter(
	/** The identifier of the healthcare party. */
	val healthcarePartyId: String,
	/** The type of the tag to match. */
	val tagType: String,
	/** The tag code prefix to match. */
	val tagCodePrefix: String,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<ServiceDto>
