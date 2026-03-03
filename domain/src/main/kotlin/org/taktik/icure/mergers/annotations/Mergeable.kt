package org.taktik.icure.mergers.annotations

/**
 * Instructs the codegen to create a merger for the annotated class.
 * The [identifiers] property must contain all the fields of the annotated class that will be used to determine if
 * two instances are equal.
 */
@Target(AnnotationTarget.CLASS)
annotation class Mergeable(val identifiers: Array<String>)
