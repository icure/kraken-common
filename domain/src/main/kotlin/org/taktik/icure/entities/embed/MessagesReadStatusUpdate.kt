/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class MessagesReadStatusUpdate(
	/** The list of message identifiers to update. */
	val ids: List<String>? = null,
	/** The identifier of the user whose read status is being updated. */
	val userId: String? = null,
	/** The timestamp (unix epoch in ms) of the status update. */
	val time: Long? = null,
	/** The new read status (true for read, false for unread). */
	val status: Boolean? = null,
) : Serializable
