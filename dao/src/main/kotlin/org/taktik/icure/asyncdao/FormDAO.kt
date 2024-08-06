/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Form

interface FormDAO : GenericDAO<Form> {
	fun listFormsByHcPartyPatient(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>): Flow<Form>

	/**
	 * Retrieves all the [Form.id]s given the delegation keys in [searchKeys] (that are the data owner
	 * ids for non-anonymous data owners and the access keys for the anonymous data owners) and a set of [Form.secretForeignKeys].
	 * Only the ids of the Forms where [Form.openingDate] is not null are returned and the results are sorted by
	 * [Form.openingDate] in ascending or descending order according to the [descending] parameter.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param searchKeys a [Set] of search keys (Data Owner Id + access keys).
	 * @param secretForeignKeys a [Set] of [Form.secretForeignKeys].
	 * @param startDate a fuzzy date. If not null, only the ids of the Forms where [Form.openingDate] is greater or equal than [startDate]
	 * will be returned.
	 * @param endDate a fuzzy date. If not null, only the ids of the Forms where [Form.openingDate] is less or equal than [endDate]
	 * will be returned.
	 * @param descending whether to sort the results by [Form.openingDate] ascending or descending.
	 * @return a [Flow] of [Form.id]s.
	 */
	fun listFormIdsByDataOwnerPatientOpeningDate(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>

	/**
	 * Retrieves all the [Form]s given the delegation keys in [searchKeys] (that are the data owner
	 * ids for non-anonymous data owners and the access keys for the anonymous data owners) and where [Form.parent] is
	 * equal to [formId] will be returned.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param searchKeys a [Set] of search keys (Data Owner Id + access keys).
	 * @param formId the [Form.parent] id to search.
	 * @return a [Flow] of [Form]s.
	 */
	fun listFormsByHcPartyAndParentId(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, formId: String): Flow<Form>

	/**
	 * Retrieves all the [Form.id]s given the delegation keys in [searchKeys] (that are the data owner
	 * ids for non-anonymous data owners and the access keys for the anonymous data owners) and where [Form.parent] is
	 * equal to [formId] will be returned.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param searchKeys a [Set] of search keys (Data Owner Id + access keys).
	 * @param formId the [Form.parent] id to search.
	 * @return a [Flow] of [Form.id]s.
	 */
	fun listFormIdsByDataOwnerAndParentId(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, formId: String): Flow<String>

	fun findForms(datastoreInformation: IDatastoreInformation, pagination: PaginationOffset<String>): Flow<ViewQueryResultEvent>

	fun listConflicts(datastoreInformation: IDatastoreInformation): Flow<Form>

	suspend fun getAllByLogicalUuid(datastoreInformation: IDatastoreInformation, formUuid: String): List<Form>

	suspend fun getAllByUniqueId(datastoreInformation: IDatastoreInformation, externalUuid: String): List<Form>
}
