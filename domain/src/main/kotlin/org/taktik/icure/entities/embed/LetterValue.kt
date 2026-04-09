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
	/** The letter key identifier. */
	val letter: String? = null,
	/** The index associated with this letter value. */
	val index: String? = null,
	/** The coefficient multiplier. */
	val coefficient: Double? = null,
	/** The numeric value. */
	val value: Double? = null,
)
