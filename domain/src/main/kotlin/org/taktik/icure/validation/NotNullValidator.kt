/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.validation

import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class NotNullValidator : ConstraintValidator<NotNull?, Any?> {
	override fun initialize(parameters: NotNull?) {}
	override fun isValid(`object`: Any?, constraintValidatorContext: ConstraintValidatorContext): Boolean {
		return `object` != null
	}
}
