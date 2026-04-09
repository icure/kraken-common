/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RevisionInfo(
	/** The revision identifier. */
	val rev: String? = null,
	/** The status of this revision (e.g., available, missing, deleted). */
	val status: String? = null,
) : Serializable
