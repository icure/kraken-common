/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.entities.MaintenanceTask
import org.taktik.icure.entities.embed.Identifier

interface MaintenanceTaskDAO : GenericDAO<MaintenanceTask> {
	fun listMaintenanceTaskIdsByHcPartyAndIdentifier(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, identifiers: List<Identifier>): Flow<String>
	fun listMaintenanceTaskIdsAfterDate(datastoreInformation: IDatastoreInformation, healthcarePartyId: String, date: Long): Flow<String>
	fun listMaintenanceTaskIdsByHcPartyAndType(datastoreInformation: IDatastoreInformation, healthcarePartyId: String, type: String, startDate: Long? = null, endDate: Long? = null): Flow<String>

	/**
	 * @param datastoreInformation the [IDatastoreInformation] that identify group and CouchDB instance.
	 * @param maintenanceTasksId a [Flow] containing the ids of the [MaintenanceTask] to retrieve.
	 * @return a [Flow] of [ViewQueryResultEvent] that wrap the [MaintenanceTask]s plus the ones needed for pagination
	 */
	fun findMaintenanceTasksByIds(
		datastoreInformation: IDatastoreInformation,
		maintenanceTasksId: Flow<String>
	): Flow<ViewQueryResultEvent>
}
