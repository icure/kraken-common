/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl.filter

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.Filter
import java.io.Serializable

interface Filter<T : Serializable, O : Identifiable<T>, F : Filter<T, O>> {
	fun resolve(filter: F, context: Filters, datastoreInformation: IDatastoreInformation): Flow<T>
}
