package org.taktik.icure.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class NotBlankValidator : ConstraintValidator<NotBlank?, Any?> {
    override fun initialize(parameters: NotBlank?) {}
    override fun isValid(`object`: Any?, constraintValidatorContext: ConstraintValidatorContext): Boolean {
        return `object` != null &&
                (`object` as? String)?.isNotBlank() == true
    }
}
