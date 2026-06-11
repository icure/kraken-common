/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl.filter

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.ConfigurationView
import org.taktik.icure.domain.filter.Filter
import java.io.Serializable

interface Filter<T : Serializable, O : Identifiable<T>, F : Filter<T, O>> {
	/**
	 * The configuration view(s) (design-doc schema views) this filter relies on when resolving, i.e. the
	 * `configurationView`(s) passed to the DAO query method(s) invoked in [resolve]. Defaults to an empty list for
	 * filters that do not query a configuration view (e.g. id-based, "all", or composition filters).
	 */
	val configurationViews: List<ConfigurationView> get() = emptyList()

	fun resolve(filter: F, context: Filters, datastoreInformation: IDatastoreInformation): Flow<T>
}
