/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynccache

import org.springframework.cache.Cache
import java.util.concurrent.Callable

class NoopCache : Cache {
	override fun clear() {}

	override fun evict(key: Any) {}

	override fun get(key: Any): Cache.ValueWrapper? = null

	override fun <T> get(
		key: Any,
		type: Class<T>?,
	): T? = null

	override fun <T> get(
		key: Any,
		valueLoader: Callable<T>,
	): T? = null

	override fun getName(): String = "noop"

	override fun getNativeCache(): Any = Any()

	override fun put(
		key: Any,
		value: Any?,
	) {}

	override fun putIfAbsent(
		key: Any,
		value: Any?,
	): Cache.ValueWrapper? = null
}
