package org.taktik.icure.mergers.annotations

@Target(AnnotationTarget.PROPERTY)
annotation class MergeStrategyUseReference(val canonicalName: String)
