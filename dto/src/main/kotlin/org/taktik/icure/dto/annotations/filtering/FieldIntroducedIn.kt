package org.taktik.icure.dto.annotations.filtering

@Target(AnnotationTarget.PROPERTY)
annotation class FieldIntroducedIn(val version: String)
