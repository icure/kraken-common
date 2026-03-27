package org.taktik.icure.mergers.annotations

@Target(AnnotationTarget.CLASS)
@Repeatable
annotation class CanMergeCondition(val expression: String)
