/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
//@Mergeable(["coefficient", "index", "letter"])
data class LetterValue(
	val letter: String? = null,
	val index: String? = null,
	val coefficient: Double? = null,
	val value: Double? = null,
)
