package org.taktik.icure.services.external.rest.v1.dto.filter

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.services.external.rest.v1.dto.base.IdentifiableDto
import org.taktik.icure.services.external.rest.v1.handlers.JacksonFilterDeserializer
import java.io.Serializable

@JsonDeserialize(using = JacksonFilterDeserializer::class)
interface AbstractFilterDto<O : IdentifiableDto<String>> : FilterDto<O> {
	/**
	 * Ignore, used only for correct serialization of classes
	 */
	@JsonProperty("\$type") // Fix serialization from kotlin
	fun includeDiscriminator(): String = this::class.simpleName!!

	val desc: String?
}

interface FilterDto<O : IdentifiableDto<*>> : Serializable {
	interface IdsFilter<T : Serializable, O : IdentifiableDto<T>> : FilterDto<O> {
		val ids: Set<T>
	}

	interface UnionFilter<O : IdentifiableDto<*>> : FilterDto<O> {
		val filters: List<FilterDto<O>>
	}

	interface IntersectionFilter<O : IdentifiableDto<*>> : FilterDto<O> {
		val filters: List<FilterDto<O>>
	}

	interface ComplementFilter<O : IdentifiableDto<*>> : FilterDto<O> {
		val superSet: FilterDto<O>
		val subSet: FilterDto<O>
	}

	interface AllFilter<O : IdentifiableDto<*>> : FilterDto<O>

	interface ByHcpartyFilter<O : IdentifiableDto<*>> : FilterDto<O> {
		val hcpId: String
	}
}
