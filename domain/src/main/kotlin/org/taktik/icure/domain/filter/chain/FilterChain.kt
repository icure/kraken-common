/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.chain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class FilterChain<O : Identifiable<String>>(val filter: AbstractFilter<O>, val predicate: org.taktik.icure.domain.filter.predicate.Predicate? = null) {
	/**
	 * Refer to [AbstractFilter.requiresSecurityPrecondition]
	 */
	val includesFiltersUnboundByDataOwnerId = filter.requiresSecurityPrecondition

	/**
	 * Refer to [AbstractFilter.requestedDataOwnerIds]
	 */
	fun requestedDataOwnerIds(): Set<String> = filter.requestedDataOwnerIds()

	fun applyTo(items: List<O>,  searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): List<O> {
		val filteredItems: List<O> = filter.applyTo(items, searchKeyMatcher)
		return if (predicate == null) filteredItems else filteredItems.filter { input: O -> predicate.apply(input) }
	}

	fun applyTo(items: Set<O>, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Set<O> {
		val filteredItems: Set<O> = filter.applyTo(items, searchKeyMatcher)
		return if (predicate == null) filteredItems else filteredItems.filter { input: O -> predicate.apply(input) }.toSet()
	}

	fun applyTo(items: Flow<O>, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Flow<O> {
		val filteredItems: Flow<O> = filter.applyTo(items, searchKeyMatcher)
		return if (predicate == null) filteredItems else filteredItems.filter { input: O -> predicate.apply(input) }
	}
}
