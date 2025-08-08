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
data class MeasureDto(
	val value: Double? = null,
	val min: Double? = null,
	val max: Double? = null,
	val ref: Double? = null,
	val severity: Int? = null,
	val severityCode: String? = null,
	val evolution: Int? = null,
	val unit: String? = null,
	val unitCodes: Set<CodeStubDto>? = null,
	val comment: String? = null,
	val comparator: String? = null,
	val sign: String? = null,
) : Serializable
