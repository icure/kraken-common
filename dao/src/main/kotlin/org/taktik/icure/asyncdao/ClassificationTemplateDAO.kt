/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.ClassificationTemplate

interface ClassificationTemplateDAO : GenericDAO<ClassificationTemplate> {
	suspend fun getClassificationTemplate(datastoreInformation: IDatastoreInformation, classificationTemplateId: String): ClassificationTemplate?

	/**
	 * Retrieves all the [ClassificationTemplate]s in a group in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [ViewQueryResultEvent] wrapping the [ClassificationTemplate]s.
	 */
	fun findClassificationTemplates(datastoreInformation: IDatastoreInformation, paginationOffset: PaginationOffset<String>): Flow<ViewQueryResultEvent>
}
