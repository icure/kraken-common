/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v1.dto.base.CodeStubDto
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class MedicinalproductDto(
	val intendedcds: List<CodeStubDto> = emptyList(),
	val deliveredcds: List<CodeStubDto> = emptyList(),
	val intendedname: String? = null,
	val deliveredname: String? = null,
	val productId: String? = null,
) : Serializable
