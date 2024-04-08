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
package org.taktik.icure.services.external.rest.v2.dto.embed

import java.io.Serializable
import java.time.Instant
import java.util.Date
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.taktik.icure.entities.embed.TypedValuesType
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto
import org.taktik.icure.utils.InstantDeserializer
import org.taktik.icure.utils.InstantSerializer

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class TypedValueDto(
	val type: TypedValuesTypeDto? = null,
	val booleanValue: Boolean? = null,
	val integerValue: Long? = null,
	val doubleValue: Double? = null,
	val stringValue: String? = null,

	@JsonSerialize(using = InstantSerializer::class)
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonDeserialize(using = InstantDeserializer::class)
	val dateValue: Instant? = null,
	override val encryptedSelf: Base64StringDto? = null
) : Comparable<TypedValueDto>, EncryptableDto, Serializable {
	companion object {
		fun <T> withValue(value: T?): TypedValueDto? = value?.let { withTypeAndValue(
			when (it) {
				is Boolean -> TypedValuesTypeDto.BOOLEAN
				is Int -> TypedValuesTypeDto.INTEGER
				is Long -> TypedValuesTypeDto.INTEGER
				is Double -> TypedValuesTypeDto.DOUBLE
				is String -> TypedValuesTypeDto.STRING
				is Date -> TypedValuesTypeDto.DATE
				else -> throw IllegalArgumentException("Unknown value type")
			},
			it
		) }

		fun <T> withTypeAndValue(type: TypedValuesTypeDto, value: T?): TypedValueDto? = value?.let {
			when (type) {
				TypedValuesTypeDto.BOOLEAN -> if (it is Boolean) {
					TypedValueDto(booleanValue = it, type = type)
				} else throw IllegalArgumentException("Illegal boolean value")

				TypedValuesTypeDto.INTEGER -> when (it) {
					is Int -> TypedValueDto(integerValue = it.toLong(), type = type)
					is Long -> TypedValueDto(integerValue = it, type = type)
					else -> throw IllegalArgumentException("Illegal integer value")
				}

				TypedValuesTypeDto.DOUBLE -> if (it is Double) {
					TypedValueDto(doubleValue = it, type = type)
				} else throw IllegalArgumentException("Illegal double value")

				TypedValuesTypeDto.STRING, TypedValuesTypeDto.JSON, TypedValuesTypeDto.CLOB -> if (it is String) {
					TypedValueDto(stringValue = it, type = type)
				} else throw IllegalArgumentException("Illegal string value")

				TypedValuesTypeDto.DATE -> if (it is Instant) {
					TypedValueDto(dateValue = it, type = type)
				} else if (it is Date) {
					TypedValueDto(dateValue = (it as Date).toInstant(), type = type)
				} else throw IllegalArgumentException("Illegal date value")
			}
		}
	}

	override fun compareTo(other: TypedValueDto): Int {
		require(other.type == type) { "Cannot compare different TypedValue types" }
		return when (type) {
			TypedValuesTypeDto.BOOLEAN -> booleanValue!!.compareTo(other.booleanValue!!)
			TypedValuesTypeDto.INTEGER -> integerValue!!.compareTo(other.integerValue!!)
			TypedValuesTypeDto.DOUBLE -> doubleValue!!.compareTo(other.doubleValue!!)
			TypedValuesTypeDto.STRING, TypedValuesTypeDto.CLOB, TypedValuesTypeDto.JSON -> stringValue!!.compareTo(other.stringValue!!)
			TypedValuesTypeDto.DATE -> dateValue!!.compareTo(other.dateValue!!)
			null -> 0
		}
	}

	override fun toString(): String {
		if (type != null) {
			when (type) {
				TypedValuesTypeDto.BOOLEAN -> return booleanValue.toString()
				TypedValuesTypeDto.INTEGER -> return integerValue.toString()
				TypedValuesTypeDto.DOUBLE -> return doubleValue.toString()
				TypedValuesTypeDto.STRING, TypedValuesTypeDto.CLOB, TypedValuesTypeDto.JSON -> return stringValue!!
				TypedValuesTypeDto.DATE -> return dateValue.toString()
			}
		}
		return super.toString()
	}
}
