/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v1.dto.base.CodeStubDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class MedicinalproductDto(
	val intendedcds: List<CodeStubDto> = emptyList(),
	val deliveredcds: List<CodeStubDto> = emptyList(),
	val intendedname: String? = null,
	val deliveredname: String? = null,
	val productId: String? = null,
	@Schema(description = "Codes that indicates the diseases or conditions that the medicinal product is intended to treat, cure, or vaccinate against") val diseaseCodes: List<CodeStubDto> = emptyList()
) : Serializable
