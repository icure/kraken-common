package org.taktik.icure.utils

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.chain.FilterChain
import java.time.Instant

fun <T : AbstractFilter<*>> T?.orThrow(): T = this ?: throw IllegalArgumentException("Unsupported filter: the provided filter is not known by the backend version or is for another type of entity")

fun <T : FilterChain<*>> T?.orThrow(): T = this ?: throw IllegalArgumentException("Unsupported filter chain: the filter used is not known by the backend version or is for another type of entity")

fun sortTimeBounds(startDate: Instant?, endDate: Instant?, descending: Boolean?): Pair<Long, Long> = if (descending != true) {
	(startDate?.toEpochMilli() ?: 0) to (endDate?.toEpochMilli() ?: Long.MAX_VALUE)
} else {
	(endDate?.toEpochMilli() ?: Long.MAX_VALUE) to (startDate?.toEpochMilli() ?: 0)
}
