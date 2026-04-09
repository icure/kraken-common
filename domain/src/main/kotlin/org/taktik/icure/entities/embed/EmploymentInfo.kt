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
data class EmploymentInfo(
	/** The start date (unix epoch in ms) of the employment. */
	val startDate: Long? = null,
	/** The end date (unix epoch in ms) of the employment. */
	val endDate: Long? = null,
	/** A code describing the profession type. */
	val professionType: CodeStub? = null,
	/** The employer details. */
	val employer: Employer? = null,
	/** The base64-encoded encrypted content of this employment info. */
	override val encryptedSelf: String? = null,
) : Serializable,
	Encryptable
