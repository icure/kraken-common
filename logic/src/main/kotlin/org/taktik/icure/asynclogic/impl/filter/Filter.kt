/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl.filter

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.Filter
import java.io.Serializable

interface Filter<T : Serializable, O : Identifiable<T>, F : Filter<T, O>> {
	/**
	 * The entity owning the configuration view(s) this filter relies on. It has no backing field: implementations
	 * derive it from the [entityClass][org.taktik.icure.asyncdao.GenericDAO.entityClass] of the DAO the filter queries.
	 * Note that this is the entity owning the view, which is not necessarily the entity returned by the filter (e.g. a
	 * filter on services queries views defined on `Contact`). `null` for filters that do not query a configuration view
	 * (e.g. id-based, "all", or composition filters).
	 */
	val entity: Class<*>? get() = null

	/**
	 * The configuration view(s) (design-doc schema views) this filter relies on when resolving, i.e. the
	 * `configurationView`(s) passed to the DAO query method(s) invoked in [resolve]. All views are defined on [entity].
	 * Defaults to an empty list for filters that do not query a configuration view (e.g. id-based, "all", or
	 * composition filters).
	 */
	val views: List<String> get() = emptyList()

	fun resolve(filter: F, context: Filters, datastoreInformation: IDatastoreInformation): Flow<T>
}
