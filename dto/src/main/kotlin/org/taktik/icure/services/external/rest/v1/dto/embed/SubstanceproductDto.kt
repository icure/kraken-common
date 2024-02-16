/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v1.dto.base.CodeStubDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class SubstanceproductDto(
	@JsonInclude(JsonInclude.Include.NON_EMPTY) val intendedcds: List<CodeStubDto> = emptyList(),
	@JsonInclude(JsonInclude.Include.NON_EMPTY) val deliveredcds: List<CodeStubDto> = emptyList(),
	val intendedname: String? = null,
	val deliveredname: String? = null,
	val productId: String? = null
) : Serializable
