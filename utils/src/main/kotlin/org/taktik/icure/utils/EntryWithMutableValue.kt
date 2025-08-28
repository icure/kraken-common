package org.taktik.icure.utils

data class EntryWithMutableValue<K, V>(
	val key: K,
	var value: V
)
