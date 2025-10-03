package org.taktik.icure.domain.filter.pricing

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Tarification

/**
 * Retrieves all the codes where [Tarification.type] is among the provided [types] that either in [Tarification.label] or in
 * [Tarification.searchTerms] have the specified [language] and, for that language, at least a word that starts with [label].
 * In a single string, are considered different words the substring separated by the characters ' ', '|', '/' and '`'.
 * If [region] is not null, then the filter will return only the codes with that region in [Tarification.regions].
 * There are three possible options for [version]:
 * - if it is null, the filter will return all the existing versions for each code.
 * - if it is the string "latest", the filter will return only the latest version for each code.
 * - any other non-null value will be interpreted as a specific version and the filter will return only the codes with
 * that specific version.
 * Note: if "latest" is used, then this filter cannot be used in WebSocket.
 */
interface PricingByRegionTypesLanguageLabelFilter : Filter<String, Tarification> {
	val region: String?
	val types: List<String>
	val language: String
	val label: String
}
