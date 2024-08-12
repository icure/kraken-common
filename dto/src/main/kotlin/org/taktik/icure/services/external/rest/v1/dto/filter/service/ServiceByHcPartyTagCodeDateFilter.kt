/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.filter.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v1.dto.embed.ServiceDto
import org.taktik.icure.services.external.rest.v1.dto.filter.AbstractFilterDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ServiceByHcPartyTagCodeDateFilter(
	override val desc: String? = null,
	val healthcarePartyId: String? = null,
	@Deprecated("Use patientSecretForeignKeys instead")
	val patientSecretForeignKey: String? = null,
	val patientSecretForeignKeys: List<String>? = null,
	val tagType: String? = null,
	val tagCode: String? = null,
	val codeType: String? = null,
	val codeCode: String? = null,
	val startValueDate: Long? = null,
	val endValueDate: Long? = null,
	val descending: Boolean = false,
) : AbstractFilterDto<ServiceDto>
