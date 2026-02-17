package org.taktik.icure.mergers.annotations

@Target(AnnotationTarget.CLASS)
annotation class PrecomputeForMerge(val name: String, val expression: String)
