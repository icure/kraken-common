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

	/**
	 * Retrieves all the [MaintenanceTask.id]s that a data owner can access through their data owner id and their search
	 * keys that have in [MaintenanceTask.identifier] at least one of the provided [identifiers].
	 *
	 * @param datastoreInformation the [IDatastoreInformation] that identify group and CouchDB instance.
	 * @param searchKeys the search keys for the data owner (data owner id + access control keys).
	 * @param identifiers the [Identifier]s to search.
	 * @return a [Flow] of [MaintenanceTask.id]s.
	 */
	fun listMaintenanceTaskIdsByHcPartyAndIdentifier(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, identifiers: List<Identifier>): Flow<String>

	/**
	 * Retrieves all the [MaintenanceTask.id]s for which [healthcarePartyId] has a delegation where [MaintenanceTask.created]
	 * is not null and greater than [date].
	 *
	 * @param datastoreInformation the [IDatastoreInformation] that identify group and CouchDB instance.
	 * @param healthcarePartyId the data owner id. Can also be a search key.
	 * @param date the reference date as timestamp.
	 * @return a [Flow] of [MaintenanceTask.id]s.
	 */
	fun listMaintenanceTaskIdsAfterDate(datastoreInformation: IDatastoreInformation, healthcarePartyId: String, date: Long): Flow<String>

	/**
	 * Retrieves all the [MaintenanceTask.id]s where [MaintenanceTask.taskType] is equal to [type] and [MaintenanceTask.created]
	 * is between [startDate] (if provided) and [endDate] (if provided).
	 *
	 * @param datastoreInformation the [IDatastoreInformation] that identify group and CouchDB instance.
	 * @param healthcarePartyId the data owner id. Can also be a search key.
	 * @param startDate the starting timestamp.
	 * @param endDate the ending timestamp.
	 * @return a [Flow] of [MaintenanceTask.id].
	 */
	fun listMaintenanceTaskIdsByHcPartyAndType(datastoreInformation: IDatastoreInformation, healthcarePartyId: String, type: String, startDate: Long? = null, endDate: Long? = null): Flow<String>

	/**
	 * Retrieves one or more [MaintenanceTask]s based on the provided [maintenanceTasksId].
	 *
	 * @param datastoreInformation the [IDatastoreInformation] that identify group and CouchDB instance.
	 * @param maintenanceTasksId a [Flow] containing the ids of the [MaintenanceTask] to retrieve.
	 * @return a [Flow] of [ViewQueryResultEvent] that wrap the [MaintenanceTask]s plus the ones needed for pagination
	 */
	fun findMaintenanceTasksByIds(
		datastoreInformation: IDatastoreInformation,
		maintenanceTasksId: Flow<String>
	): Flow<ViewQueryResultEvent>
}
