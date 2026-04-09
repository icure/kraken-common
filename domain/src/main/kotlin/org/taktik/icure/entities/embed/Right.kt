/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.mergers.annotations.Mergeable
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Right(
	/** The identifier of the user these rights apply to. */
	val userId: String? = null,
	/** Whether the user has read permission. */
	val read: Boolean = false,
	/** Whether the user has write permission. */
	val write: Boolean = false,
	/** Whether the user has administration permission. */
	val administration: Boolean = false,
) : Serializable
