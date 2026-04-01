package org.taktik.icure.mergers.annotations

/**
 * Defines the merge and canMerge behavior to expressions that are then used by the generator in the merge and canMerge
 * implementations.
 * The expressions can use the {{LEFT}} and {{RIGHT}} placeholders that will be replaced with the actual parameter names
 * in the implementation.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class MergeStrategyUse(
	val canMerge: String,
	val merge: String,
	val imports: Array<String> = [],
)
