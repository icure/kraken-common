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
@Constraint(validatedBy = [NotNullValidator::class])
annotation class NotNull(
	val message: String = "{org.taktik.icure.validation.NotNull.message}",
	val groups: Array<KClass<*>> = [],
	val payload: Array<KClass<out Payload>> = [],
	val autoFix: AutoFix = AutoFix.NOFIX,
	val applyOnModify: Boolean = true,
	val doNotApplyOnCardinalModel: Boolean = false, // If true, it will not applied when the cardinal model header is present
) {

	/**
	 * Defines several [NotNull] annotations on the same element.
	 *
	 * @see jakarta.validation.constraints.NotNull
	 */
	@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FIELD, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.VALUE_PARAMETER)
	@Retention(AnnotationRetention.RUNTIME)
	@MustBeDocumented
	annotation class List(vararg val value: NotNull)
}
