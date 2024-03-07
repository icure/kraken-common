/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.taktik.icure.entities.base.CodeIdentification

class ValidCodeValidator : ConstraintValidator<ValidCode?, Any?> {
	override fun initialize(parameters: ValidCode?) {}
	@Suppress("UNCHECKED_CAST")
	override fun isValid(`object`: Any?, constraintValidatorContext: ConstraintValidatorContext): Boolean {
		return if (`object` is Collection<*>) {
			val c = `object` as Collection<Any>
			c.isEmpty() || c.stream().allMatch { isValidItem(it) }
		} else {
			isValidItem(`object`)
		}
	}

	private fun isValidItem(`object`: Any?): Boolean {
		return (
			`object` == null ||
				(`object` is CodeIdentification && `object`.code != null && `object`.type != null && `object`.id.startsWith(`object`.type + "|" + `object`.code))
			)
	}
}
