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
 * Filter that matches services by healthcare party, patient, code prefix, and value date range.
 */
data class ServiceByHcPartyPatientCodePrefixFilter(
	/** The identifier of the healthcare party. */
	@ActiveField val healthcarePartyId: String,
	/** The set of secret foreign keys for patient matching. */
	@ActiveField val patientSecretForeignKeys: Set<String>,
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
