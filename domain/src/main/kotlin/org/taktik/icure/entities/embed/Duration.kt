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
data class Duration(
	/** The numeric value of the duration. */
	val value: Double? = null,
	/** The coded time unit (CD-TIMEUNIT) for this duration. */
	val unit: CodeStub? = null, // CD-TIMEUNIT
) : Serializable
