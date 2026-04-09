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
	/** The start date (unix epoch in ms) of the schooling period. */
	val startDate: Long? = null,
	/** The end date (unix epoch in ms) of the schooling period. */
	val endDate: Long? = null,
	/** The name of the school. */
	val school: String? = null,
	/** A code describing the type of education. */
	val typeOfEducation: CodeStub? = null,
	/** The base64-encoded encrypted content of this schooling info. */
	override val encryptedSelf: String? = null,
) : Serializable,
	Encryptable
