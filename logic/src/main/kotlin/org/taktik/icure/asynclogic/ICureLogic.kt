/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import org.taktik.icure.domain.IndexingInfo
import org.taktik.icure.entities.ReplicationInfo

interface ICureLogic {
	suspend fun getIndexingStatus(): IndexingInfo
	suspend fun updateDesignDocForCurrentUser(daoEntityName: String, warmup: Boolean = false)
	fun getVersion(): String
	fun getSemanticVersion(): String
	suspend fun getReplicationInfo(): ReplicationInfo
	fun getProcessInfo(): String
}
