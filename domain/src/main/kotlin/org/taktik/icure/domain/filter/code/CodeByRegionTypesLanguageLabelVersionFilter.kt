package org.taktik.icure.domain.filter.code

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.base.Code

/**
 * Returns all the [Code]s which label matches the query passed as parameter in the language passed as parameter.
 * This method is NOT intended to be used to make wide search on codes. Because of the view structure, the same
 * code appears more than once if it has more than one word in the label. So, if the label query is not specific
 * enough, the code will appear more than once in the result.
 * [region] is the region of the code to match.
 * [language] the language of the label to search.
 * [types] a [List] of [Code.type]s. Only the codes of those types will be returned. Important: if between 2 different calls (for pages) the order of types changes behaviour is undefined
 * [label] a label or a prefix to search.
 * [version] the version of the code. It may be null (all the versions will be returned), a specific version or
 * the string "latest", that will get the latest version of each code. Note: if "latest" is used, then this filter
 * cannot be used in WebSocket.
 */
interface CodeByRegionTypesLanguageLabelVersionFilter : Filter<String, Code> {
	val region: String?
	val types: List<String>
	val language: String
	val label: String
	val version: String?
}