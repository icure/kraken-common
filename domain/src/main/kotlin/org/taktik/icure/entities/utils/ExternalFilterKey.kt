package org.taktik.icure.entities.utils

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.couchdb.entity.ComplexKey

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed interface ExternalFilterKey {

	val key: Any

	data class ExternalFilterStringKey(override val key: String) : ExternalFilterKey
	data class ExternalFilterLongKey(override val key: Long) : ExternalFilterKey
	data class ExternalFilterComplexKey(override val key: ComplexKey) : ExternalFilterKey
}
