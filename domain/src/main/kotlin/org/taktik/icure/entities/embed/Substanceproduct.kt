/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.ValidCode
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Substanceproduct(
	/** The list of coded identifiers for the intended substance product. */
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE)
	val intendedcds: List<CodeStub> = emptyList(),

	/** The list of coded identifiers for the actually delivered substance product. */
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE)
	val deliveredcds: List<CodeStub> = emptyList(),
	/** The name of the intended substance product. */
	val intendedname: String? = null,
	/** The name of the actually delivered substance product. */
	val deliveredname: String? = null,
	/** The product identifier. */
	val productId: String? = null,
) : Serializable
