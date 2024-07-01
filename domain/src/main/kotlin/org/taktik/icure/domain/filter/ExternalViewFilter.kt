package org.taktik.icure.domain.filter

import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.entities.utils.ExternalFilterKey

interface ExternalViewFilter<O : Identifiable<String>> : Filter<String, O> {
	val entityQualifiedName: String
	val view: String
	val partition: String
	val startKey: ExternalFilterKey<*>?
	val endKey: ExternalFilterKey<*>?
}