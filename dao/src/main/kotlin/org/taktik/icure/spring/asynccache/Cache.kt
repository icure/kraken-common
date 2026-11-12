/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.spring.asynccache

import org.springframework.cache.Cache

interface Cache<K : Any, V> {
	fun getName(): String

	suspend fun get(key: K): V?

	fun clear()

	fun invalidate(): Boolean

	suspend fun evict(key: K)

	suspend fun put(
		key: K,
		value: V,
	)

	fun iterator(): Iterator<Map.Entry<K, V>>
}
