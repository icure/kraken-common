/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import org.taktik.icure.domain.IndexingInfo
import org.taktik.icure.entities.ReplicationInfo

interface ICureSharedService {
	suspend fun getReplicationInfo(): ReplicationInfo
	suspend fun getIndexingStatus(): IndexingInfo
	fun getVersion(): String
	suspend fun getProcessInfo(): String
	suspend fun setLogLevel(logLevel: String, packageName: String): String
	suspend fun updateDesignDocForCurrentUser(daoEntityName: String, warmup: Boolean = false)
}
