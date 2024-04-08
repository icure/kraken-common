/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter

import java.io.Serializable
import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.entities.base.HasEncryptionMetadata

interface Filter<T : Serializable, O : Identifiable<T>> {
	fun matches(item: O, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean
	fun applyTo(items: List<O>, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): List<O>
	fun applyTo(items: Set<O>, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Set<O>
	fun applyTo(items: Flow<O>, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Flow<O>
}
