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
data class Periodicity(
	/** The code associated with this periodicity. */
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) val relatedCode: CodeStub? = null,
	/** The periodicity code defining the recurrence pattern. */
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) val relatedPeriodicity: CodeStub? = null,
) : Serializable
