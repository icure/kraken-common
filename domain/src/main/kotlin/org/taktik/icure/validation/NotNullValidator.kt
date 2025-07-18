/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class NotNullValidator : ConstraintValidator<NotNull?, Any?> {
	override fun initialize(parameters: NotNull?) {}
	override fun isValid(`object`: Any?, constraintValidatorContext: ConstraintValidatorContext): Boolean = `object` != null
}
