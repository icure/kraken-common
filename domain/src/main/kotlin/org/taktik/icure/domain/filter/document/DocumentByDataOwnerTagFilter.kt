package org.taktik.icure.domain.filter.document

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Document
import java.time.Instant

/**
 * Retrieves all the [Document]s with a delegation for [dataOwnerId], where [Document.tags] contains at least a tag of
 * type [tagType], or if [tagCode] is provided a code with a tag of type [tagType] and code [tagCode]
 */
interface DocumentByDataOwnerTagFilter : Filter<String, Document> {
	val dataOwnerId: String
	val tagType: String
	val tagCode: String?
}
