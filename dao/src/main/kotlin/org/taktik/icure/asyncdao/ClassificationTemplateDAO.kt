/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.ClassificationTemplate

interface ClassificationTemplateDAO : GenericDAO<ClassificationTemplate> {
	suspend fun getClassificationTemplate(datastoreInformation: IDatastoreInformation, classificationTemplateId: String): ClassificationTemplate?

	/**
	 * Retrieves all the [ClassificationTemplate]s for a set of search keys, that will be matched against the
	 * [ClassificationTemplate.delegations] keys and the secure delegation delegate, and a list of
	 * [ClassificationTemplate.secretForeignKeys].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param searchKeys a [Set] of search keys.
	 * @param secretPatientKeys the patient secret foreign keys.
	 * @return a [Flow] of [ClassificationTemplate]s.
	 */
	fun listClassificationsByHCPartyAndSecretPatientKeys(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>): Flow<ClassificationTemplate>

	/**
	 * Retrieves all the [ClassificationTemplate]s for the given search key, that will be matched against the
	 * [ClassificationTemplate.delegations] keys and the secure delegation delegate, and a single
	 * [ClassificationTemplate.secretForeignKeys] in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param searchKey the search key.
	 * @param secretPatientKey the patient secret foreign key.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [ViewQueryResultEvent] wrapping the [ClassificationTemplate]s.
	 */
	fun listClassificationsByHCPartyAndSecretPatientKey(datastoreInformation: IDatastoreInformation, searchKey: String, secretPatientKey: String, paginationOffset: PaginationOffset<ComplexKey>): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [ClassificationTemplate]s in a group in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [ViewQueryResultEvent] wrapping the [ClassificationTemplate]s.
	 */
	fun findClassificationTemplates(datastoreInformation: IDatastoreInformation, paginationOffset: PaginationOffset<String>): Flow<ViewQueryResultEvent>
}
