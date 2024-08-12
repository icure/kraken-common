package org.taktik.icure.domain.filter.document

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.embed.DocumentType

/**
 * Retrieves all the [Document]s with a delegation for [dataOwnerId], where [Document.secretForeignKeys]
 * contains at least one of [secretPatientKeys] and where [Document.documentType] is equal to [documentType].
 * If [dataOwnerId] is the data owner making the request, then also the available secret access keys will be used to
 * retrieve the results.
 * This filter explicitly requires a [dataOwnerId], so it does not require any security precondition.
 */
interface DocumentByTypeDataOwnerPatientFilter : Filter<String, Document> {
	val dataOwnerId: String
	val documentType: DocumentType
	val secretPatientKeys: Set<String>
}
