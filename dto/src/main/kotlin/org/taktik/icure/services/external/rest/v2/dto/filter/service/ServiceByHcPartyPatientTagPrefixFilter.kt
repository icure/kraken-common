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
 * Filter that matches services by healthcare party, patient, tag prefix, and value date range.
 */
data class ServiceByHcPartyPatientTagPrefixFilter(
	/** The identifier of the healthcare party. */
	val healthcarePartyId: String,
	/** The set of secret foreign keys for patient matching. */
	val patientSecretForeignKeys: Set<String>,
	/** The type of the tag to match. */
	val tagType: String,
	/** The tag code prefix to match. */
	val tagCodePrefix: String,
	/** The start of the value date range. */
	val startValueDate: Long? = null,
	/** The end of the value date range. */
	val endValueDate: Long? = null,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<ServiceDto>
