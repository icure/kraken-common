/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CalendarItemTag(
	/** The code identifying this tag. */
	val code: String? = null,
	/** The timestamp (unix epoch in ms) when the tag was applied. */
	val date: Long? = null,
	/** The identifier of the user who applied the tag. */
	val userId: String? = null,
	/** The display name of the user who applied the tag. */
	val userName: String? = null,
	/** The base64-encoded encrypted content of this tag. */
	override val encryptedSelf: String? = null,
) : Serializable,
	Encryptable
