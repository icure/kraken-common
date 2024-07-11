/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.ValidCode

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Medicinalproduct(
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE)
	val intendedcds: List<CodeStub> = emptyList(),

	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE)
	val deliveredcds: List<CodeStub> = emptyList(),
	val intendedname: String? = null,
	val deliveredname: String? = null,
	val productId: String? = null
) : Serializable
