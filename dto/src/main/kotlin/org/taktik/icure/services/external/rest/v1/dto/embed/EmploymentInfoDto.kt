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
data class EmploymentInfoDto(
	val startDate: Long? = null,
	val endDate: Long? = null,
	val professionType: CodeStubDto? = null,
	val employer: EmployerDto? = null,
	override val encryptedSelf: String?,
) : Serializable,
	EncryptableDto
