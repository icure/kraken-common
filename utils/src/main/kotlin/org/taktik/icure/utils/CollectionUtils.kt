package org.taktik.icure.utils

import kotlin.NoSuchElementException

fun <T : Any, R : Comparable<R>> List<T>.indexOfMinBy(selector: (T) -> R): Int {
	if (this.isEmpty()) throw NoSuchElementException("List is empty.")
	var minIndex = 0
	var minValue: R = selector(this[0])
	for (i in 1 until size) {
		val currSelected = selector(this[i])
		if (currSelected < minValue) {
			minIndex = i
			minValue = currSelected
		}
	}
	return minIndex
}

fun <T : Any, R : Comparable<R>> mergeSortedBy(
	sortedLists: List<List<T>>,
	selector: (T) -> R
): List<T> {
	val res = ArrayList<T>(sortedLists.sumOf { it.size })
	val remaining = sortedLists.mapIndexedNotNullTo(ArrayDeque(sortedLists.size)) { i, list ->
		if (list.isEmpty()) null else EntryWithMutableValue(i, 0)
	}
	while (remaining.isNotEmpty()) {
		val nextItemIndex = remaining.indexOfMinBy { entry ->
			selector(sortedLists[entry.key][entry.value])
		}
		val nextItem = remaining[nextItemIndex]
		res.add(sortedLists[nextItem.key][nextItem.value])
		nextItem.value++
		if (nextItem.value >= sortedLists[nextItem.key].size) {
			remaining.removeAt(nextItemIndex)
		}
	}
	return res
}