/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.base.CodeStub
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class SchoolingInfo(
	val startDate: Long? = null,
	val endDate: Long? = null,
	val school: String? = null,
	val typeOfEducation: CodeStub? = null,
	override val encryptedSelf: String?,
) : Serializable,
	Encryptable
