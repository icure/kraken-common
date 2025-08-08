package org.taktik.icure.domain.filter.form

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Form

/**
 * Returns all the [Form]s where [Form.uniqueId] is equal to [uniqueId], sorted by [Form.created] in ascending
 * or descending order according to the [descending] parameter.
 * As [Form] is an encryptable entity but this filter does not require a data owner id, a special permission is
 * needed to use this filter.
 */
interface FormByUniqueUuidFilter : Filter<String, Form> {
	val uniqueId: String
	val descending: Boolean?
}
