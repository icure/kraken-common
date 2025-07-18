package org.taktik.icure.domain.filter.code

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.base.Code

/**
 * Retrieves all the [Code]s with the specified region, type, code, and version using pagination.
 * If [type] is null, the filter will return all the [Code]s with the specified region.
 * If [code] is null, the filter will return all the [Code]s with the specified type and region.
 * There are three possible options for [version]:
 * - if it is null, the filter will return all the existing versions for each code.
 * - if it is the string "latest", the filter will return only the latest version for each code.
 * - any other non-null value will be interpreted as a specific version and the filter will return only the codes with
 * that specific version.
 * Note: if "latest" is used, then this filter cannot be used in WebSocket.
 */
interface CodeByRegionTypeCodeVersionFilter : Filter<String, Code> {
	val region: String
	val type: String?
	val code: String?
	val version: String?
}
