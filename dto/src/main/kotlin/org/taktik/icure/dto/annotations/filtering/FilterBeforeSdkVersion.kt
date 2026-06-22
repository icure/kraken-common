package org.taktik.icure.dto.annotations.filtering

@Target(AnnotationTarget.PROPERTY)
annotation class FilterBeforeSdkVersion(val version: String)
