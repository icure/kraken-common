package org.taktik.icure.entities.utils

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.couchdb.entity.ComplexKey

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed interface ExternalFilterKey<T> {

	val key: T

	data class ExternalFilterStringKey(override val key: String) : ExternalFilterKey<String>
	data class ExternalFilterLongKey(override val key: Long) : ExternalFilterKey<Long>
	data class ExternalFilterComplexKey(override val key: ComplexKey) : ExternalFilterKey<ComplexKey>

}
