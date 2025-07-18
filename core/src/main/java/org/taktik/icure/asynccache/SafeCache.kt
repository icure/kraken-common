/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynccache

import org.springframework.cache.Cache

class SafeCache<K : Any, V> {
	interface ValueProvider<K, V> {
		fun getValue(key: K): V
	}

	@set:Synchronized
	var cache: Cache? = null

	constructor()
	constructor(cache: Cache?) {
		this.cache = cache
	}

	val name: String
		get() = cache!!.name

	@Synchronized
	fun clear() {
		cache!!.clear()
	}

	@Synchronized
	fun evict(key: K) {
		cache!!.evict(key)
	}

	operator fun get(
		key: K,
		valueProvider: ValueProvider<K, V>,
	): V? {
		var value: V?
		var valueWrapper = cache!!.get(key)
		if (valueWrapper == null) {
			synchronized(this) {
				valueWrapper = cache!!.get(key)
				if (valueWrapper == null) {
					value = valueProvider.getValue(key)
					cache!!.put(key, value)
				} else {
					value = valueWrapper!!.get() as V?
				}
			}
		} else {
			value = valueWrapper!!.get() as V?
		}
		return value
	}

	fun getIfPresent(key: K): V? {
		val valueWrapper = cache!!.get(key)
		return if (valueWrapper != null) valueWrapper.get() as V? else null
	}

	@Synchronized
	fun put(
		key: K,
		value: V,
	) {
		cache!!.put(key, value)
	}
}
