/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import java.net.URI
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ReplicatorResponse
import org.taktik.couchdb.entity.ActiveTask
import org.taktik.couchdb.entity.DatabaseInfoWrapper
import org.taktik.couchdb.entity.ReplicateCommand
import org.taktik.couchdb.entity.ReplicatorDocument
import org.taktik.couchdb.entity.Scheduler
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.embed.DatabaseSynchronization

interface ICureDAO {
	suspend fun getIndexingStatus(datastoreInformation: IDatastoreInformation): Map<String, Int>
	suspend fun getPendingChanges(datastoreInformation: IDatastoreInformation): Map<DatabaseSynchronization, Long>
	suspend fun getReplicatorInfo(dbInstanceUri: URI, id: String): ReplicatorDocument?
	suspend fun replicate(dbInstanceUri: URI, command: ReplicateCommand): ReplicatorResponse
	suspend fun deleteReplicatorDoc(dbInstanceUri: URI, docId: String): ReplicatorResponse
	suspend fun getSchedulerDocs(dbInstanceUri: URI): Scheduler.Docs
	fun getDatabasesInfos(dbInstanceUri: URI, dbIds: List<String>): Flow<DatabaseInfoWrapper>
}
