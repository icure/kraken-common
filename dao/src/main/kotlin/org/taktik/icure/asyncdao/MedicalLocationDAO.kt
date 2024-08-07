/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.entities.MedicalLocation

interface MedicalLocationDAO : GenericDAO<MedicalLocation> {

	/**
	 * Retrieves all the [MedicalLocation]s where the postcode in [MedicalLocation.address] is equal to [postCode].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param postCode the post code to search.
	 * @return a [Flow] of [MedicalLocation]s.
	 */
	fun byPostCode(datastoreInformation: IDatastoreInformation, postCode: String): Flow<MedicalLocation>

	/**
	 * Retrieves all the [MedicalLocation.id]s where the postcode in [MedicalLocation.address] is equal to [postCode].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param postCode the post code to search.
	 * @return a [Flow] of [MedicalLocation.id]s.
	 */
	fun idsByPostCode(datastoreInformation: IDatastoreInformation, postCode: String): Flow<String>
}
