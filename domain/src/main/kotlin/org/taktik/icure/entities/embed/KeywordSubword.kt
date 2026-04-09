/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class KeywordSubword(
	/** The string value of this subword. */
	val value: String? = null,
	/** The list of child subwords forming a tree. */
	val subWords: List<KeywordSubword>? = null,
) : Serializable
