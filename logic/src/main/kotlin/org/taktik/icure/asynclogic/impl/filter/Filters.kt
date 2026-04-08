/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl.filter

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactor.ReactorContext
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.web.server.ServerWebExchange
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.AbstractFilter
import java.io.Serializable
import kotlin.coroutines.coroutineContext

class Filters : ApplicationContextAware {
	private var applicationContext: ApplicationContext? = null
	private val filtersCache: MutableMap<String, Filter<*, *, *>> = HashMap()

	override fun setApplicationContext(applicationContext: ApplicationContext) {
		this.applicationContext = applicationContext
	}

	fun <T : Serializable, O : Identifiable<T>> resolve(filter: org.taktik.icure.domain.filter.Filter<T, O>, datastoreInformation: IDatastoreInformation) = flow<T> {
		val desc = (filter as? AbstractFilter<*>)?.desc
		val startTime = if (desc != null) System.currentTimeMillis() else 0L

		val truncatedFullClassName = filter.javaClass.name.replace(".+?filter\\.impl\\.".toRegex(), "").replace(".+?dto\\.filter\\.".toRegex(), "")
		val filterClass = try {
			Class.forName("org.taktik.icure.asynclogic.impl.filter.$truncatedFullClassName")
		} catch (e: ClassNotFoundException) {
			throw IllegalStateException("Could not find class for filter $truncatedFullClassName", e)
		}

		@Suppress("UNCHECKED_CAST")
		val filterToBeResolved = (filtersCache[truncatedFullClassName] as Filter<T, O, org.taktik.icure.domain.filter.Filter<T, O>>?) ?: kotlin.run {
			try {
				// Note that generic type is erased: at this point we only verify that the bean is a Filter, not a Filter<T, O, ..>
				(applicationContext!!.getBean(filterClass) as? Filter<T, O, org.taktik.icure.domain.filter.Filter<T, O>>)?.also { filterBean ->
					filtersCache[truncatedFullClassName] = filterBean
				}
			} catch (e: Exception) {
				throw IllegalStateException("Could not find bean resolver for filter $truncatedFullClassName", e)
			} ?: throw IllegalStateException("Filter bean found for $truncatedFullClassName is not a filter")
		}
		val ids = hashSetOf<Serializable>()
		(filterToBeResolved.resolve(filter, this@Filters, datastoreInformation)).collect {
			if (!ids.contains(it)) {
				emit(it)
				ids.add(it)
			}
		}

		if (desc != null) {
			val elapsed = System.currentTimeMillis() - startTime
			val headers = currentCoroutineContext()[ReactorContext.Key]?.context
				?.getOrEmpty<ServerWebExchange>(ServerWebExchange::class.java)
				?.orElse(null)
				?.response?.headers
			if (headers != null && headers::class.simpleName?.startsWith("ReadOnly") == false) {
				try {
					headers.add("x-filter-timing-$desc", "$elapsed ms")
				} catch (_: UnsupportedOperationException) { }
			}
		}
	}
}
