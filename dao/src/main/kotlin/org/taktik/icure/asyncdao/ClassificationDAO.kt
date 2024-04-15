/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.entities.Classification

interface ClassificationDAO : GenericDAO<Classification> {
	fun listClassificationByPatient(datastoreInformation: IDatastoreInformation, patientId: String): Flow<Classification>

	suspend fun getClassification(datastoreInformation: IDatastoreInformation, classificationId: String): Classification?

	/**
	 * Retrieves all the [Classification]s for a set of search keys, that will be matched against the [Classification.delegations]
	 * keys and the secure delegation delegate, and a list of [Classification.secretForeignKeys].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param searchKeys a [Set] of search keys.
	 * @param secretPatientKeys the patient secret foreign keys.
	 * @return a [Flow] of [Classification]s.
	 */
	fun listClassificationsByHCPartyAndSecretPatientKeys(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>): Flow<Classification>

	/**
	 * Retrieves the ids of all the [Classification]s given the delegation keys in [searchKeys] (that are the data owner
	 * ids for non-anonymous data owners and the access keys for the anonymous data owners) and a set of [Classification.secretForeignKeys].
	 * Only the ids of the Classifications where [Classification.created] is not null are returned and the results are sorted by
	 * [Classification.created] in ascending or descending order according to the [descending] parameter.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param searchKeys a [Set] of search keys (Data Owner Id + access keys).
	 * @param secretForeignKeys a [Set] of [Classification.secretForeignKeys].
	 * @param startDate a timestamp. If not null, only the ids of the Contacts where [Classification.created] is greater or equal than [startDate]
	 * will be returned.
	 * @param endDate a timestamp. If not null, only the ids of the Contacts where [Classification.created] is less or equal than [endDate]
	 * will be returned.
	 * @param descending whether to sort the results by [Classification.created] ascending or descending.
	 * @return a [Flow] of Classification ids.
	 */
	fun listClassificationIdsByDataOwnerPatientCreated(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>

}
