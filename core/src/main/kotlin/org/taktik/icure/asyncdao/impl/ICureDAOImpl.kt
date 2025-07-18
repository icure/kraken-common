/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import java.net.URI
import io.icure.asyncjacksonhttpclient.net.addSinglePathComponent
import io.icure.asyncjacksonhttpclient.net.web.WebClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.springframework.beans.factory.annotation.Qualifier
import org.taktik.couchdb.ClientImpl
import org.taktik.couchdb.ReplicatorResponse
import org.taktik.couchdb.entity.DatabaseInfoWrapper
import org.taktik.couchdb.entity.Indexer
import org.taktik.couchdb.entity.ReplicateCommand
import org.taktik.couchdb.entity.ReplicationTask
import org.taktik.couchdb.entity.ReplicatorDocument
import org.taktik.couchdb.entity.Scheduler
import org.taktik.couchdb.get
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.ICureDAO
import org.taktik.icure.asyncdao.components.ActiveTasksProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.embed.DatabaseSynchronization
import org.taktik.icure.security.CouchDbCredentialsProvider

@OptIn(ExperimentalCoroutinesApi::class)
open class ICureDAOImpl(
	private val httpClient: WebClient,
	private val couchDbCredentialsProvider: CouchDbCredentialsProvider,
	@Qualifier("baseCouchDbDispatcher") protected val couchDbDispatcher: CouchDbDispatcher,
	private val activeTasksProvider: ActiveTasksProvider
) : ICureDAO {

	override suspend fun getIndexingStatus(datastoreInformation: IDatastoreInformation): Map<String, Int> {
		return activeTasksProvider.getActiveTasks(datastoreInformation).filterIsInstance<Indexer>().associate {
			"${it.database}/${it.design_document}" to (it.progress ?: 0)
		}
	}

	override suspend fun getPendingChanges(datastoreInformation: IDatastoreInformation): Map<DatabaseSynchronization, Long> {
		return activeTasksProvider.getActiveTasks(datastoreInformation).filterIsInstance<ReplicationTask>().associate {
			DatabaseSynchronization(it.source, it.target) to (it.changes_pending?.toLong() ?: 0)
		}
	}

	override suspend fun getReplicatorInfo(dbInstanceUri: URI, id: String): ReplicatorDocument? {
		return client(dbInstanceUri.addSinglePathComponent("_replicator")).get(id)
	}

	override suspend fun replicate(dbInstanceUri: URI, command: ReplicateCommand): ReplicatorResponse {
		return client(dbInstanceUri.addSinglePathComponent("_replicator")).replicate(command)
	}

	override suspend fun deleteReplicatorDoc(dbInstanceUri: URI, docId: String): ReplicatorResponse {
		return client(dbInstanceUri.addSinglePathComponent("_replicator")).deleteReplication(docId)
	}

	override suspend fun getSchedulerDocs(dbInstanceUri: URI): Scheduler.Docs {
		return client(dbInstanceUri.addSinglePathComponent("_replicator")).schedulerDocs()
	}

	override fun getDatabasesInfos(dbInstanceUri: URI, dbIds: List<String>): Flow<DatabaseInfoWrapper> {
		return client(dbInstanceUri).databaseInfos(dbIds.asFlow())
	}

	private fun client(uri: URI) =
		ClientImpl(httpClient, uri, "", couchDbCredentialsProvider)
}
