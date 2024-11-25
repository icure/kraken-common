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
data class SchoolingInfoDto(
	val startDate: Long? = null,
	val endDate: Long? = null,
	val school: String? = null,
	val typeOfEducation: CodeStubDto? = null,
	override val encryptedSelf: String?
) : Serializable, EncryptableDto
