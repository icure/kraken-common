/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import java.io.InputStream
import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.base.Code
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.pagination.PaginatedElement

interface CodeLogic : EntityPersister<Code, String> {
	fun getTagTypeCandidates(): List<String>
	fun getRegions(): List<String>
	suspend fun get(id: String): Code?

	suspend fun get(type: String, code: String, version: String): Code?
	fun getCodes(ids: List<String>): Flow<Code>
	suspend fun create(code: Code): Code?

	suspend fun create(batch: List<Code>): List<Code>?

	@Throws(Exception::class)
	suspend fun modify(code: Code): Code?

	fun modify(batch: List<Code>): Flow<Code>

	/**
	 * Retrieves all the types of the code in the db for the specified region and code.
	 * - If the region is null, all the types are returned.
	 * - If only the region is specified, all the types of the code for that region are returned.
	 * - If both parameters are specified, the flow will contain the specified type if there is at least one code with
	 * that type and will be empty otherwise.
	 *
	 * @param region the region of the codes which type should be returned.
	 * @param type the type of the codes to return if present.
	 * @return a [Flow] containing the code types matching the criteria.
	 */
	fun listCodeTypesBy(region: String?, type: String?): Flow<String>

	/**
	 * Retrieves all the [Code]s with the specified type, code and version.
	 * If type is null, all the [Code]s for the group are returned.
	 * If code is null, all the [Code]s with the specified type are returned.
	 * If version is null, all the [Code]s with the specified code and type are returned.
	 *
	 * @param type the type of the codes to return.
	 * @param code the code of the codes to return.
	 * @param version the version of the codes to return.
	 * @return a [Flow] containing the [Code]s matching the criteria.
	 */
	fun findCodesBy(type: String?, code: String?, version: String?): Flow<Code>

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
	 * @param region the region of the codes to return.
	 * @param type the type of the codes to return.
	 * @param code the code of the codes to return.
	 * @param version the version of the codes to return, if not null, or "latest".
	 * @return a [Flow] containing the [Code]s matching the criteria.
	 */
	fun findCodesBy(region: String?, type: String?, code: String?, version: String?): Flow<Code>

	/**
	 * Retrieves all the [Code]s with the specified region, type, code, and version using pagination.
	 * If [region] is null, all the [Code]s for the group are returned.
	 * If [type] is null, all the [Code]s with the specified region are returned.
	 * If [code] is null, all the [Code]s with the specified type are returned.
	 * There are three possible options for [version]:
	 * - if it is null, all the versions for a code are returned.
	 * - if it is the string "latest", only the latest version for each code is returned.
	 * - any other non-null value will be interpreted as a specific version and only the codes with that specific
	 * version will be returned.
	 *
	 * @param region the region of the codes to return.
	 * @param type the type of the codes to return.
	 * @param code the code of the codes to return.
	 * @param version the version of the codes to return, if not null, or "latest".
	 * @param paginationOffset a [PaginationOffset] for pagination.
	 * @return a [Flow] containing the [Code]s matching the criteria, wrapped in a [PaginatedElement] for pagination.
	 */
	fun findCodesBy(region: String?, type: String?, code: String?, version: String?, paginationOffset: PaginationOffset<List<String?>>): Flow<PaginatedElement>

	/**
	 * Returns all the [Code]s which label matches the query passed as parameter in the language passed as parameter.
	 * This method is NOT intended to be used to make wide search on codes. Because of the view structure, the same
	 * code appears more than once if it has more than one word in the label. So, if the label query is not specific
	 * enough, the code will appear more than once in the result.
	 *
	 * @param region the region of the code to match.
	 * @param language the language of the label to search.
	 * @param types a [Set] of [Code.type]s. Only the codes of those types will be returned.
	 * @param label a label or a prefix to search.
	 * @param version the version of the code. It may be null (all the versions will be returned), a specific version or
	 * the string "latest", that will get the latest version of each code.
	 * @param paginationOffset a [PaginationOffset] for getting the successive pages.
	 * @return a [Flow] of [PaginatedElement]s the wrap the [Code]s for pagination.
	 */
	fun findCodesByLabel(region: String?, language: String, types: Set<String>, label: String, version: String?, paginationOffset: PaginationOffset<List<String?>>): Flow<PaginatedElement>

	/**
	 * Retrieves the ids of all the [Code]s of a certain [type] that match the provided [label] in a certain [language].
	 * If a [region] is provided, only the codes for that region will be returned.
	 *
	 * @param region the region of the code to match.
	 * @param language the language of the label to search.
	 * @param type the type of the code to search.
	 * @param label a label or a prefix to search.
	 */
	fun listCodeIdsByLabel(region: String?, language: String, type: String, label: String?): Flow<String>
	fun listCodeIdsByTypeCodeVersionInterval(startType: String?, startCode: String?, startVersion: String?, endType: String?, endCode: String?, endVersion: String?): Flow<String>
	fun findCodesByQualifiedLinkId(region: String?, linkType: String, linkedId: String?, pagination: PaginationOffset<List<String>>): Flow<PaginatedElement>
	fun listCodeIdsByQualifiedLinkId(linkType: String, linkedId: String?): Flow<String>
	suspend fun <T : Enum<*>> importCodesFromEnum(e: Class<T>)

	suspend fun importCodesFromXml(md5: String, type: String, stream: InputStream)

	suspend fun importCodesFromJSON(stream: InputStream)
	fun listCodes(paginationOffset: PaginationOffset<*>?, filterChain: FilterChain<Code>, sort: String?, desc: Boolean?): Flow<ViewQueryResultEvent>

	suspend fun getOrCreateCode(type: String, code: String, version: String): Code?
	suspend fun isValid(type: String?, code: String?, version: String?): Boolean
	suspend fun isValid(code: Code, ofType: String? = null): Boolean
	suspend fun isValid(code: CodeStub, ofType: String? = null): Boolean

	/**
	 * Retrieves a single [Code] that matches the specified region, if specified, type and has a label that matches at least one of the
	 * specified languages, if such a code exists.
	 * NOTE: if there is more than one [Code] that satisfy the criteria or multiple versions of a single code, then this
	 * method does not guarantee which code is returned:
	 * - If multiple codes match, it will return the one which [Code.code] property is the first in lexicographical order.
	 * - If multiple version match, it will return the code which [Code.version] property is the first in lexicographical order.
	 *
	 * @param region the region of the code to match.
	 * @param label a label of the code to match.
	 * @param type the type of the code to match,
	 * @param languages a [List] of ISO language code for the labels.
	 * @return a [Code] that matches the criteria or null.
	 */
	suspend fun getCodeByLabel(region: String?, label: String, type: String, languages: List<String> = listOf("fr", "nl")): Code?
}
