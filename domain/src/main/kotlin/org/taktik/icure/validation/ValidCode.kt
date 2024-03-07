/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FIELD, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Constraint(validatedBy = [ValidCodeValidator::class])
annotation class ValidCode(val message: String = "{org.taktik.icure.validation.ValidCode.message}", val groups: Array<KClass<*>> = [], val payload: Array<KClass<out Payload>> = [], val autoFix: AutoFix = AutoFix.NOFIX) {
	/**
	 * Defines several [ValidCode] annotations on the same element.
	 *
	 * @see jakarta.validation.constraints.NotNull
	 */
	@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FIELD, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.VALUE_PARAMETER)
	@Retention(AnnotationRetention.RUNTIME)
	@MustBeDocumented
	annotation class List(vararg val value: ValidCode)
}
