/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.embed

import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CalendarItemTagDto(
	val code: String? = null,
	val date: Long? = null,
	val userId: String? = null,
	val userName: String? = null,
	override val encryptedSelf: String? = null
) : Serializable, EncryptableDto
