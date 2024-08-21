/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.entities.base.HasEncryptionMetadata
import java.io.Serializable

interface AbstractFilter<O : Identifiable<String>> : Filter<String, O>, Serializable {
	val desc: String?

	/**
	 * If the filter includes any search for data that is not limited to data for a (set of) specific data owner id(s)
	 * AND may leak information on the content of an entity this method returns true.
	 * This could be for example the case if:
	 * - The filter is a composition (union/intersection) of other filters and in at least one of the sub-filters
	 *   [requiresSecurityPrecondition] returns true
	 * - The filter does not have a field for data-owner id (or it is optional and set to null, returning values for
	 *   all hcps) and allows to filter by codes or tags in the entity
	 * Note that a filter "byIds" only will always have this value as false, since the only information it could "leak"
	 * is the id of the entity, already given in input to the filter.
	 *
	 * Note that even if [requestedDataOwnerIds] is  not empty this method may still return true.
	 */
	val requiresSecurityPrecondition: Boolean

	/**
	 * Whether this filter can be used to filter entities in a WebSocket subscription. If false, the WebSocket will
	 * throw a [UnsupportedOperationException] on connecting with this filter.
	 */
	val canBeUsedInWebsocket: Boolean

	/**
	 * Returns the ids of all data owners for which data is requested by this filter.
	 * This includes:
	 * - If the filter is a composition (union/intersection) of other filters, all the [requestedDataOwnerIds] of the
	 *   sub-filters
	 * - In simple filters with a field for data-owner id, the value of that field if non-null.
	 */
	fun requestedDataOwnerIds(): Set<String>

	override fun applyTo(items: Flow<O>, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Flow<O> {
		return items.filter { item -> this.matches(item, searchKeyMatcher) }
	}

	override fun applyTo(items: List<O>, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): List<O> {
		return items.filter { item -> this.matches(item, searchKeyMatcher) }
	}

	override fun applyTo(items: Set<O>, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Set<O> {
		return items.filter { item -> this.matches(item, searchKeyMatcher) }.toSet()
	}
}
