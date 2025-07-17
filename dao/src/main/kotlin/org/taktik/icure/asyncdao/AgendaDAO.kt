/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.entities.Agenda
import org.taktik.icure.entities.base.PropertyStub

interface AgendaDAO : GenericDAO<Agenda> {
	/**
	 * Retrieves all the [Agenda]s where [Agenda.userId] equals [userId]
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param userId the id of the user the [Agenda]s refer to.
	 * @return a [Flow] of [Agenda]s
	 */
	fun getAgendasByUser(datastoreInformation: IDatastoreInformation, userId: String): Flow<Agenda>

	/**
	 * Retrieves all the [Agenda.id]s for the agendas where [Agenda.userId] is equal to [userId]
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param userId the id of the user the [Agenda]s refer to.
	 * @return a [Flow] of [Agenda.id]s
	 */
	fun listAgendaIdsByUser(datastoreInformation: IDatastoreInformation, userId: String): Flow<String>

	/**
	 * Retrieves all the [Agenda]s where one of the [Agenda.rights] contains [userId].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param userId the id of that can read the [Agenda].
	 * @return a [Flow] of [Agenda]s
	 */
	fun getReadableAgendaByUserLegacy(datastoreInformation: IDatastoreInformation, userId: String): Flow<Agenda>

	/**
	 * Retrieves all the [Agenda]s where one of the [Agenda.rights] contains [userId].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param userId the id of that can read the [Agenda].
	 * @return a [Flow] of [Agenda]s
	 */
	fun listReadableAgendaByUserLegacy(datastoreInformation: IDatastoreInformation, userId: String): Flow<String>

	/**
	 * Retrieves all the [Agenda.id]s for the agendas where one of the [Agenda.userRights] contains [userId].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param userId the id of that can read the [Agenda].
	 * @return a [Flow] of [Agenda.id]s
	 */
	fun listReadableAgendaIdsByUserRights(datastoreInformation: IDatastoreInformation, userId: String): Flow<String>

	/**
	 * Retrieves all the [Agenda.id]s of the agendas that have at least one [Agenda.properties] that matches the id
	 * and typedValue of the [property] passed as parameter.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param property a [PropertyStub] to match against the agendas.
	 * @return a [Flow] of [Agenda.id]s
	 * @throws IllegalArgumentException if [property] has a null id or no typedValue that is not null.
	 */
	fun listAgendasIdsByTypedProperty(datastoreInformation: IDatastoreInformation, property: PropertyStub): Flow<String>

	fun listAgendasByTypedProperty(datastoreInformation: IDatastoreInformation, property: PropertyStub): Flow<Agenda>

	/**
	 * Retrieves all the [Agenda.id]s of the agendas that have at least one [Agenda.properties] where the id matches
	 * [propertyId].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param propertyId the id of a property to search.
	 * @return a [Flow] of [Agenda.id]s
	 */
	fun listAgendasWithProperty(datastoreInformation: IDatastoreInformation, propertyId: String): Flow<String>
}
