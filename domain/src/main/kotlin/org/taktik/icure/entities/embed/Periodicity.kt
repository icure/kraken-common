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
data class Periodicity(
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) val relatedCode: CodeStub? = null,
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) val relatedPeriodicity: CodeStub? = null
) : Serializable
