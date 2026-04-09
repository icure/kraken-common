/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ReplicationInfo(
	/** Whether the replication is currently active. */
	val active: Boolean = false,
	/** Whether the replication process is currently running. */
	val running: Boolean = false,
	/** The number of pending changes to replicate from the source. */
	val pendingFrom: Int? = null,
	/** The number of pending changes to replicate to the target. */
	val pendingTo: Int? = null,
) : Serializable
