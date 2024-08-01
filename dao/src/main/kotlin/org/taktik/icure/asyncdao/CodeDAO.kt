/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.base.Code

interface CodeDAO : GenericDAO<Code> {

	/**
	 * Retrieves all the types of the [Code]s in the db for the specified region and code.
	 * - If the region is null, all the types are returned.
	 * - If only the region is specified, all the types of the code for that region are returned.
	 * - If both parameters are specified, the flow will contain the specified type if there is at least one code with
	 * that type and will be empty otherwise.
	 *
	 * @param datastoreInformation an [IDatastoreInformation] instance containing the group and db info.
	 * @param region the region of the codes which type should be returned.
	 * @param type the type of the codes to return if present.
	 * @return a [Flow] containing the code types matching the criteria.
	 */
	fun listCodeTypesByRegionAndType(datastoreInformation: IDatastoreInformation, region: String?, type: String?): Flow<String>

	/**
	 * Retrieves all the [Code]s with the specified type, code and version.
	 * If [type] is null, all the [Code]s for the group are returned.
	 * If [code] is null, all the [Code]s with the specified type are returned.
	 * If [version] is null, all the [Code]s with the specified code and type are returned.
	 *
	 * @param datastoreInformation an [IDatastoreInformation] instance containing the group and db info.
	 * @param type the type of the codes to return.
	 * @param code the code of the codes to return.
	 * @param version the version of the codes to return.
	 * @return a [Flow] containing the [Code]s matching the criteria.
	 */
	fun listCodesBy(datastoreInformation: IDatastoreInformation, type: String?, code: String?, version: String?): Flow<Code>

	/**
	 * Retrieves all the [Code]s with the specified region, type, code, and version.
	 * If [region] is null, all the [Code]s for the group are returned.
	 * If [type] is null, all the [Code]s with the specified region are returned.
	 * If [code] is null, all the [Code]s with the specified type are returned.
	 * There are three possible options for [version]:
	 * - if it is null, all the versions for a code are returned.
	 * - if it is the string "latest", only the latest version for each code is returned.
	 * - any other non-null value will be interpreted as a specific version and only the codes with that specific
	 * version will be returned.
	 *
	 * @param datastoreInformation an [IDatastoreInformation] instance containing the group and db info.
	 * @param region the region of the codes to return.
	 * @param type the type of the codes to return.
	 * @param code the code of the codes to return.
	 * @param version the version of the codes to return, if not null, or "latest".
	 * @return a [Flow] containing the [Code]s matching the criteria.
	 */
	fun listCodesBy(datastoreInformation: IDatastoreInformation, region: String?, type: String?, code: String?, version: String?): Flow<Code>

	/**
	 * Retrieves all the [Code]s with the specified region, type, code, and version with pagination.
	 * If [region] is null, all the [Code]s for the group are returned.
	 * If [type] is null, all the [Code]s with the specified region are returned.
	 * If [code] is null, all the [Code]s with the specified type are returned.
	 * There are three possible options for [version]:
	 * - if it is null, all the versions for a code are returned.
	 * - if it is the string "latest", only the latest version for each code is returned.
	 * - any other non-null value will be interpreted as a specific version and only the codes with that specific
	 * version will be returned.
	 *
	 * @param datastoreInformation an [IDatastoreInformation] instance containing the group and db info.
	 * @param region the region of the codes to return.
	 * @param type the type of the codes to return.
	 * @param code the code of the codes to return.
	 * @param version the version of the codes to return, if not null, or "latest".
	 * @param paginationOffset a [PaginationOffset] for pagination.
	 * @return a [Flow] containing the [Code]s matching the criteria, wrapped in a [ViewQueryResultEvent] for pagination.
	 */
	fun findCodesBy(datastoreInformation: IDatastoreInformation, region: String?, type: String?, code: String?, version: String?, paginationOffset: PaginationOffset<List<String?>>): Flow<ViewQueryResultEvent>

	/**
	 * Returns all the [Code]s which label matches the query passed as parameter in the language passed as parameter.
	 * This method is NOT intended to be used to make wide search on codes. Because of the view structure, the same
	 * code appears more than once if it has more than one word in the label. So, if the label query is not specific
	 * enough, the code will appear more than once in the result.
	 *
	 * @param datastoreInformation an [IDatastoreInformation] instance containing the group and db info.
	 * @param region the region of the code to match.
	 * @param language the language of the label to search.
	 * @param type the type of the code to search.
	 * @param label a label or a prefix to search.
	 * @param version the version of the code. It may be null (all the versions will be returned), a specific version or
	 * the string "latest", that will get the latest version of each code.
	 * @param paginationOffset a [PaginationOffset] for getting the successive pages.
	 * @return a [Flow] of [ViewQueryResultEvent]s the wrap the [Code]s for pagination.
	 */
	fun findCodesByLabel(datastoreInformation: IDatastoreInformation, region: String?, language: String, type: String, label: String, version: String?, paginationOffset: PaginationOffset<List<String?>>): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves the ids of all the [Code]s of a certain [type] that match the provided [label] in a certain [language].
	 * If a [region] is provided, only the codes for that region will be returned.
	 *
	 * @param datastoreInformation an [IDatastoreInformation] instance containing the group and db info.
	 * @param region the region of the code to match.
	 * @param language the language of the label to search.
	 * @param type the type of the code to search.
	 * @param label a label or a prefix to search.
	 */
	fun listCodeIdsByLabel(datastoreInformation: IDatastoreInformation, region: String?, language: String, type: String, label: String?): Flow<String>
	fun listCodeIdsByTypeCodeVersionInterval(datastoreInformation: IDatastoreInformation, startType: String?, startCode: String?, startVersion: String?, endType: String?, endCode: String?, endVersion: String?): Flow<String>
	suspend fun isValid(datastoreInformation: IDatastoreInformation, codeType: String, codeCode: String, codeVersion: String?): Boolean

	/**
	 * Retrieves a single [Code] that matches the specified region, if specified, type and has a label that matches at least one of the
	 * specified languages, if such a code exists.
	 * NOTE: if there is more than one [Code] that satisfy the criteria or multiple versions of a single code, then this
	 * method does not guarantee which code is returned:
	 * - If multiple codes match, it will return the one which [Code.code] property is the first in lexicographical order.
	 * - If multiple version match, it will return the code which [Code.version] property is the first in lexicographical order.
	 *
	 * @param datastoreInformation an [IDatastoreInformation] instance containing the group and db info.
	 * @param region the region of the code to match.
	 * @param label a label of the code to match.
	 * @param type the type of the code to match,
	 * @param languages a [List] of ISO language code for the labels.
	 * @return a [Code] that matches the criteria or null.
	 */
	suspend fun getCodeByLabel(datastoreInformation: IDatastoreInformation, region: String?, label: String, type: String, languages: List<String> = listOf("fr", "nl")): Code?

	/**
	 * Retrieves all the [Code]s where [Code.qualifiedLinks] contains [linkType]. If [linkedId] is not null, then it
	 * will only include the codes that have [linkedId] as value for [linkType].
	 *
	 * @param datastoreInformation an [IDatastoreInformation] instance containing the group and db info.
	 * @param linkType the link type that is a key in [Code.qualifiedLinks].
	 * @param linkedId the value corresponding to [linkType] in [Code.qualifiedLinks]. If null, it will suffice that the
	 * code has [linkType] in [Code.qualifiedLinks] to be included in th result.
	 * @param paginationOffset a [PaginationOffset] of [List] of [String] for pagination.
	 * @return a [Flow] of codes, wrapped in a [ViewQueryResultEvent] for pagination.
	 */
	fun findCodesByQualifiedLinkId(datastoreInformation: IDatastoreInformation, linkType: String, linkedId: String?, paginationOffset: PaginationOffset<List<String>>): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [Code.id]s where [Code.qualifiedLinks] contains [linkType]. If [linkedId] is not null, then it
	 * will only include the codes that have [linkedId] as value for [linkType].
	 *
	 * @param datastoreInformation an [IDatastoreInformation] instance containing the group and db info.
	 * @param linkType the link type that is a key in [Code.qualifiedLinks].
	 * @param linkedId the value corresponding to [linkType] in [Code.qualifiedLinks]. If null, it will suffice that the
	 * code has [linkType] in [Code.qualifiedLinks] to be included in th result.
	 * @return a [Flow] of [Code.id]s.
	 */
	fun listCodeIdsByQualifiedLinkId(datastoreInformation: IDatastoreInformation, linkType: String, linkedId: String?): Flow<String>
	fun getCodesByIdsForPagination(datastoreInformation: IDatastoreInformation, ids: List<String>): Flow<ViewQueryResultEvent>
    fun listConflicts(datastoreInformation: IDatastoreInformation): Flow<Code>

	/**
	 * Retrieves all the [Code.id]s with the specified region, type, code, and version.
	 * If [region] is null, all the [Code]s for the group are returned.
	 * If [type] is null, all the [Code]s with the specified region are returned.
	 * If [code] is null, all the [Code]s with the specified type are returned.
	 * There are three possible options for [version]:
	 * - if it is null, all the versions for a code are returned.
	 * - if it is the string "latest", only the latest version for each code is returned.
	 * - any other non-null value will be interpreted as a specific version and only the codes with that specific
	 * version will be returned.
	 *
	 * @param datastoreInformation an [IDatastoreInformation] instance containing the group and db info.
	 * @param region the region of the codes to return.
	 * @param type the type of the codes to return.
	 * @param code the code of the codes to return.
	 * @param version the version of the codes to return, if not null, or "latest".
	 * @return a [Flow] containing the [Code.id]s matching the criteria.
	 */
	fun listCodeIdsBy(
		datastoreInformation: IDatastoreInformation,
		region: String?,
		type: String?,
		code: String?,
		version: String?
	): Flow<String>
}
