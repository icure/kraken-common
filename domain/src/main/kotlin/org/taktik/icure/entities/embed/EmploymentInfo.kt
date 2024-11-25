/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.base.CodeStub

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class EmploymentInfo(
	val startDate: Long? = null,
	val endDate: Long? = null,
	val professionType: CodeStub? = null,
	val employer: Employer? = null,
	override val encryptedSelf: String?
) : Serializable, Encryptable
