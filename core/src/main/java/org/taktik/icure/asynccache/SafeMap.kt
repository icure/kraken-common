/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynccache

import java.io.Serializable

class SafeMap<K, V> : Serializable {
	interface ValueProvider<K, V> {
		fun getValue(key: K): V
	}

	private var map: MutableMap<K, V>

	constructor() {
		map = HashMap()
	}

	constructor(map: MutableMap<K, V>) {
		this.map = map
	}

	operator fun get(
		key: K,
		valueProvider: ValueProvider<K, V>,
	): V? {
		var value = map[key]
		if (value == null) {
			synchronized(this) {
				value = map[key]
				if (value == null) {
					value = valueProvider.getValue(key)
					if (value != null) {
						map[key] = value!!
					}
				}
			}
		}
		return value
	}

	fun getIfPresent(key: K): V? = map[key]

	@Synchronized
	fun put(
		key: K,
		value: V,
	) {
		map[key] = value
	}

	fun getMap(): Map<K, V> = map

	@Synchronized
	fun setMap(map: MutableMap<K, V>) {
		this.map = map
	}

	companion object {
		private const val serialVersionUID = 1L
	}
}
