/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Tarification
import org.taktik.icure.pagination.PaginationElement

interface TarificationLogic : EntityPersister<Tarification> {
	suspend fun getTarification(id: String): Tarification?
	suspend fun getTarification(type: String, tarification: String, version: String): Tarification?
	fun getTarifications(ids: List<String>): Flow<Tarification>
	suspend fun createTarification(tarification: Tarification): Tarification?
	suspend fun modifyTarification(tarification: Tarification): Tarification?
	fun findTarificationsBy(type: String?, tarification: String?, version: String?): Flow<Tarification>
	fun findTarificationsBy(region: String?, type: String?, tarification: String?, version: String?): Flow<Tarification>

	/**
	 * Retrieves all the [Tarification]s where [Tarification.regions] contains [region], [Tarification.type] is [type],
	 * [Tarification.code] is [tarification], and [Tarification.version] is [version] in a format for pagination.
	 * If [version] is null, all the entities that match the region, type, and code will be returned.
	 * If [tarification] is null, all the entities that match the region and type will be returned, independently of the value
	 * of [version]
	 * If [type] is null, all the entities that match the region will be returned, independently of the value of [tarification]
	 * and [version].
	 * If [region] is null, all the entities with at least a [Tarification.regions] will be return, independently of
	 * the values of [type], [tarification] and [version].
	 *
	 * @param region a region to match against [Tarification.regions].
	 * @param type the [Tarification.type].
	 * @param tarification the [Tarification.code].
	 * @param version the [Tarification.version].
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [PaginationElement] containing the [Tarification]s.
	 */
	fun findTarificationsBy(region: String?, type: String?, tarification: String?, version: String?, paginationOffset: PaginationOffset<ComplexKey>): Flow<PaginationElement>

	/**
	 * Retrieves all the [Tarification]s where [Tarification.regions] contains [region], and there is at least a [Tarification.label]
	 * or a [Tarification.searchTerms] for that [language] that, when normalized, matches the [label] passed as parameter.
	 * If [types] is not empty, only the entities where [Tarification.type] is included in [types] will be retrieved.
	 * The results will be provided in a format for pagination.
	 * Warning: by passing a null [region] or [language], all the entities in the view will be returned. This is highly
	 * inefficient as the view is not optimized for this usage and can potentially contain many duplicates for each
	 * entity.
	 *
	 * @param region the [Tarification.regions] to search. If null, all the entities will be returned.
	 * @param language the language that appears as key in [Tarification.label] or [Tarification.searchTerms]. If null,
	 * all the entities will be returned.
	 * @param label a query that will be normalized and then searched against [Tarification.label] or [Tarification.searchTerms]
	 * for the specified language. This query will act as a prefix, meaning that the view will check if the normalized
	 * query is a prefix of the view value.
	 * @param types a [Set] of [Tarification.type] of the entities to retrieve. If null, the [Tarification]s of all types will be returned.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [PaginationElement] containing the [Tarification]s.
	 */
	fun findTarificationsOfTypesByLabel(region: String?, language: String?, label: String?, types: Set<String>?, paginationOffset: PaginationOffset<ComplexKey>): Flow<PaginationElement>
	fun findTarificationsByLabel(region: String?, language: String?, type: String?, label: String?, paginationOffset: PaginationOffset<List<String?>>): Flow<ViewQueryResultEvent>
	suspend fun getOrCreateTarification(type: String, tarification: String): Tarification?
}
