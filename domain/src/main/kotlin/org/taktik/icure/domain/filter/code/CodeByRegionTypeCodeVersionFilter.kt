package org.taktik.icure.domain.filter.code

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.base.Code

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
 */
interface CodeByRegionTypeCodeVersionFilter : Filter<String, Code> {
	val region: String
	val type: String?
	val code: String?
	val version: String?
}