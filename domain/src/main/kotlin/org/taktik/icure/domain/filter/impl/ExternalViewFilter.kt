package org.taktik.icure.domain.filter.impl

import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.utils.ExternalFilterKey

data class ExternalViewFilter<O : Identifiable<String>> (
	override val entityQualifiedName: String,
	override val view: String,
	override val partition: String,
	override val startKey: ExternalFilterKey?,
	override val endKey: ExternalFilterKey?,
	override val desc: String?,
) : AbstractFilter<O>, org.taktik.icure.domain.filter.ExternalViewFilter<O> {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: O, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean) = true

}