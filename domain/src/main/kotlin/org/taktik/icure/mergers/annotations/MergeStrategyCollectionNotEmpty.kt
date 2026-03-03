package org.taktik.icure.mergers.annotations

/**
 * Defines the following behaviors for mergers:
 * - CanMerge: the fields will be mergeable if at least one of them is null or at least one of them is empty or they
 * are content equals.
 * - Merge: take the first one (left to right) that is non-null and non-empty.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class MergeStrategyCollectionNotEmpty
