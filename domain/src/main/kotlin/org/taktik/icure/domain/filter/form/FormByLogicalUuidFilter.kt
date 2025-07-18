package org.taktik.icure.domain.filter.form

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Form

/**
 * Returns all the [Form]s where [Form.logicalUuid] is equal to [logicalUuid], sorted by [Form.created] in ascending
 * or descending order according to the [descending] parameter.
 * As [Form] is an encryptable entity but this filter does not require a data owner id, a special permission is
 * needed to use this filter.
 */
interface FormByLogicalUuidFilter : Filter<String, Form> {
	val logicalUuid: String
	val descending: Boolean?
}
