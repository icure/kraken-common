/*
 *  iCure Data Stack. Copyright (c) 2020 Taktik SA
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public
 *     License along with this program.  If not, see
 *     <https://www.gnu.org/licenses/>.
 */
package org.taktik.icure.services.external.rest.v2.dto.filter

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifiableDto
import org.taktik.icure.services.external.rest.v2.handlers.JacksonFilterDeserializer
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

sealed interface FilterDto<O : IdentifiableDto<*>> : Serializable {
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
