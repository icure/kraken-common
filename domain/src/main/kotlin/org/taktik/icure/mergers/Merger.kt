package org.taktik.icure.mergers

abstract class Merger<T> {
	abstract fun canMerge(l: T?, r: T?): Boolean
	abstract fun merge(l: T?, r: T?): T?

	protected fun <F> canMergeField(l: F?, r: F?): Boolean = (l == null || r == null || l == r)
}