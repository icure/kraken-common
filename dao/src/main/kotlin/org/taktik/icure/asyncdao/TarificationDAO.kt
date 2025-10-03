/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Tarification
import org.taktik.icure.entities.base.Code

interface TarificationDAO : GenericDAO<Tarification> {
	fun listTarificationsBy(datastoreInformation: IDatastoreInformation, type: String?, code: String?, version: String?): Flow<Tarification>
	fun listTarificationsBy(datastoreInformation: IDatastoreInformation, region: String?, type: String?, code: String?, version: String?): Flow<Tarification>

	/**
	 * Retrieves all the [Tarification]s where [Tarification.regions] contains [region], [Tarification.type] is [type],
	 * [Tarification.code] is [code], and [Tarification.version] is [version] in a format for pagination.
	 * If [version] is null, all the entities that match the region, type, and code will be returned.
	 * If [code] is null, all the entities that match the region and type will be returned, independently of the value
	 * of [version]
	 * If [type] is null, all the entities that match the region will be returned, independently of the value of [code]
	 * and [version].
	 * If [region] is null, all the entities with at least a [Tarification.regions] will be return, independently of
	 * the values of [type], [code] and [version].
	 *
	 * @param datastoreInformation an [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param region a region to match against [Tarification.regions].
	 * @param type the [Tarification.type].
	 * @param code the [Tarification.code].
	 * @param version the [Tarification.version].
	 * @param pagination a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [ViewQueryResultEvent] containing the [Tarification]s.
	 */
	fun findTarificationsBy(datastoreInformation: IDatastoreInformation, region: String?, type: String?, code: String?, version: String?, pagination: PaginationOffset<ComplexKey>): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [Tarification]s where [Tarification.regions] contains [region], and there is at least a [Tarification.label]
	 * or a [Tarification.searchTerms] for that [language] that, when normalized, matches the [label] passed as parameter.
	 * The results will be provided in a format for pagination.
	 * Warning: by passing a null [region] or [language], all the entities in the view will be returned. This is highly
	 * inefficient as the view is not optimized for this usage and can potentially contain many duplicates for each
	 * entity.
	 *
	 * @param datastoreInformation an [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param region the [Tarification.regions] to search. If null, all the entities will be returned.
	 * @param language the language that appears as key in [Tarification.label] or [Tarification.searchTerms]. If null,
	 * all the entities will be returned.
	 * @param label a query that will be normalized and then searched against [Tarification.label] or [Tarification.searchTerms]
	 * for the specified language. This query will act as a prefix, meaning that the view will check if the normalized
	 * query is a prefix of the view value.
	 * @param pagination a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [ViewQueryResultEvent] containing the [Tarification]s.
	 */
	fun findTarificationsByLabel(datastoreInformation: IDatastoreInformation, region: String?, language: String?, label: String?, pagination: PaginationOffset<ComplexKey>): Flow<ViewQueryResultEvent>
	fun findTarificationsByLabel(datastoreInformation: IDatastoreInformation, region: String?, language: String?, type: String?, label: String?, pagination: PaginationOffset<List<String?>>): Flow<ViewQueryResultEvent>
    fun listConflicts(datastoreInformation: IDatastoreInformation): Flow<Tarification>
}
