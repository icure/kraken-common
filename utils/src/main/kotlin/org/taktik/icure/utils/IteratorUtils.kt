/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.utils

fun <T, R> Iterator<T>.map(mapper: (T) -> R): Iterator<R> = object : Iterator<R> {
	override fun hasNext() = this@map.hasNext()
	override fun next() = this@map.next().let(mapper)
}

/**
 * Merges a [List] of [Iterator]s providing a single iterator that will produce sorted values.
 */
fun <T : Comparable<T>> List<Iterator<T>>.sortedMerge(): Iterator<T> = object : Iterator<T> {
	val buffer: MutableList<T?> = this@sortedMerge.map { null }.toMutableList()
	var previous: T? = null

	override fun hasNext() = this@sortedMerge.any { it.hasNext() } || buffer.any { it != null }
	override fun next(): T {
		// Drain the iterators
		this@sortedMerge.forEachIndexed { idx, it ->
			if (it.hasNext() && buffer[idx] == null) {
				buffer[idx] = it.next()
			}
		}
		val (value, idx) = buffer.foldIndexed((null to -1) as Pair<T?, Int>) { i, (acc: T?, idx), t ->
			when {
				acc == null -> t to i
				t == null -> acc to idx
				acc < t -> acc to idx
				else -> t to i
			}
		}
		if (idx >= 0) {
			buffer[idx] = null
		}
		return value?.also { v ->
			previous?.let { p -> if (v < p) throw IllegalStateException("Provided iterators must be sorted") }
			previous = v
		} ?: throw NoSuchElementException()
	}
}
