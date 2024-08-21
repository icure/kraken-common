package org.taktik.icure.domain.filter.form

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Form

/**
 * Retrieves all the [Form]s with a delegation for [dataOwnerId] and where [Form.parent] is equal to [parentId].
 * This filter explicitly requires a [dataOwnerId], so it does not require any security precondition.
 */
interface FormByDataOwnerParentIdFilter : Filter<String, Form> {
	val dataOwnerId: String
	val parentId: String
}
