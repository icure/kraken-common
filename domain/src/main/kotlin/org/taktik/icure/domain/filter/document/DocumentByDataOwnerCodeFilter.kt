package org.taktik.icure.domain.filter.document

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Document

/**
 * Retrieves all the [Document]s with a delegation for [dataOwnerId], where [Document.codes] contains at least a code of
 * type [codeType], or if [codeCode] is provided a code with a code of type [codeType] and code [codeCode]
 */
interface DocumentByDataOwnerCodeFilter : Filter<String, Document> {
	val dataOwnerId: String
	val codeType: String
	val codeCode: String?
}
