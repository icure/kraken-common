/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.spring.asynccache

import java.io.Serializable

interface AsyncCacheManager {
	fun <K : Serializable, V : Any> getCache(name: String): Cache<K, V>
}
