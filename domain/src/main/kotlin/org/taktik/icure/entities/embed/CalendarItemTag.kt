/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CalendarItemTag(
	val code: String? = null,
	val date: Long? = null,
	val userId: String? = null,
	val userName: String? = null,
	override val encryptedSelf: String? = null,
) : Serializable,
	Encryptable
