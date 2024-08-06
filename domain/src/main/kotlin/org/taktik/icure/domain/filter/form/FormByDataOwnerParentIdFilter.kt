package org.taktik.icure.domain.filter.form

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Form

/**
 * Retrieves all the [Form]s that the data owner with id [dataOwnerId] can access and [Form.parent] is equal to [parentId].
 * If [dataOwnerId] is the data owner making the request, than also the available secret access keys will be used to
 * retrieve the results.
 * This filter explicitly requires a [dataOwnerId], so it does not require any security precondition.
 */
interface FormByDataOwnerParentIdFilter : Filter<String, Form> {
	val dataOwnerId: String
	val parentId: String
}