/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.VersionFiltering
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.embed.Identifier

interface HealthElementDAO : ConflictDAO<HealthElement> {
	fun listHealthElementIdsByHcParty(datastoreInformation: IDatastoreInformation, hcPartyId: String): Flow<String>

	fun listHealthElementIdsByHcPartyAndSecretPatientKeys(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>): Flow<String>

	@Deprecated("""
		Use listHealthElementIdsByHcPartyAndCodesAndValueDateAndVersioning instead.
		Equivalent if not specifying value date range and using VersionFiltering.ANY, but uses new more efficient views.
		This method is currently kept to allow groups that do not yet have the updated views to continue to work.
	""")
	fun listHealthElementIdsByHcPartyAndCodes(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, codeType: String, codeNumber: String): Flow<String>

	@Deprecated("""
		Use listHealthElementIdsByHcPartyAndTagsAndValueDateAndVersioning instead.
		Equivalent if not specifying value date range and using VersionFiltering.ANY, but uses new more efficient views.
		This method is currently kept to allow groups that do not yet have the updated views to continue to work.
	""")
	fun listHealthElementIdsByHcPartyAndTags(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, tagType: String, tagCode: String): Flow<String>

	@Deprecated("""
		Use listHealthElementIdsByHcPartyAndStatusAndVersioning instead.
		Equivalent if using VersionFiltering.ANY, but uses new more efficient views.
		This method is currently kept to allow groups that do not yet have the updated views to continue to work.
	""")
	fun listHealthElementIdsByHcPartyAndStatus(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, status: Int?): Flow<String>

	@Deprecated("""
		Use listHealthElementsIdsByHcPartyAndIdentifiersAndVersioning instead.
		Equivalent if using VersionFiltering.ANY, but uses new more efficient views.
		This method is currently kept to allow groups that do not yet have the updated views to continue to work.
	""")
	fun listHealthElementsIdsByHcPartyAndIdentifiers(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, identifiers: List<Identifier>): Flow<String>

	suspend fun getHealthElement(datastoreInformation: IDatastoreInformation, healthElementId: String): HealthElement?

	@Deprecated("This method is inefficient for high volumes of keys, use listHealthElementIdsByDataOwnerPatientOpeningDate instead")
	fun listHealthElementsByHCPartyAndSecretPatientKeys(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>): Flow<HealthElement>

	/**
	 * Retrieves the ids of all the [HealthElement]s given the delegation keys in [searchKeys] (that are the data owner
	 * ids for non-anonymous data owners and the access keys for the anonymous data owners) and a set of [HealthElement.secretForeignKeys].
	 * Only the ids of the HealthElements where [HealthElement.openingDate] is not null are returned and the results are sorted by
	 * [HealthElement.openingDate] in ascending or descending order according to the [descending] parameter.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param searchKeys a [Set] of search keys (Data Owner Id + access keys).
	 * @param secretForeignKeys a [Set] of [HealthElement.secretForeignKeys].
	 * @param startDate a fuzzy date. If not null, only the ids of the HealthElements where [HealthElement.openingDate] is greater or equal than [startDate]
	 * will be returned.
	 * @param endDate a fuzzy date. If not null, only the ids of the HealthElements where [HealthElement.openingDate] is less or equal than [endDate]
	 * will be returned.
	 * @param descending whether to sort the results by [HealthElement.openingDate] ascending or descending.
	 * @return a [Flow] of HealthElement ids.
	 */
	fun listHealthElementIdsByDataOwnerPatientOpeningDate(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>

	fun findHealthElementsByIds(datastoreInformation: IDatastoreInformation, healthElementIds: Flow<String>): Flow<ViewQueryResultEvent>

	fun listHealthElementIdsByHcPartyAndCodesAndValueDateAndVersioning(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, codeType: String, codeCode: String, startValueDate: Long?, endValueDate: Long?, filterVersion: VersionFiltering): Flow<String>

	fun listHealthElementIdsByHcPartyAndTagsAndValueDateAndVersioning(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, tagType: String, tagCode: String, startValueDate: Long?, endValueDate: Long?, filterVersion: VersionFiltering): Flow<String>

	fun listHealthElementIdsByHcPartyAndStatusAndVersioning(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, status: Int?, filterVersion: VersionFiltering): Flow<String>

	fun listHealthElementsIdsByHcPartyAndIdentifiersAndVersioning(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, identifiers: List<Identifier>, filterVersion: VersionFiltering): Flow<String>
}
