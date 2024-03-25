/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import java.io.Serializable
import java.time.Instant
import java.util.Date
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.taktik.icure.utils.InstantDeserializer
import org.taktik.icure.utils.InstantSerializer

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class TypedValueDto<T>(
    val type: TypedValuesTypeDto? = null,
    val booleanValue: Boolean? = null,
    val integerValue: Long? = null,
    val doubleValue: Double? = null,
    val stringValue: String? = null,

    @JsonSerialize(using = InstantSerializer::class)
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonDeserialize(using = InstantDeserializer::class)
	val dateValue: Instant? = null,
    override val encryptedSelf: String? = null
) : Comparable<TypedValueDto<T>>, EncryptedDto, Serializable {
	companion object {
		fun <T> withValue(value: T?): TypedValueDto<T>? = value?.let { withTypeAndValue(
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

		fun <T> withTypeAndValue(type: TypedValuesTypeDto, value: T?): TypedValueDto<T>? = value?.let {
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

	@JsonIgnore
	fun <T> getValue(): T? {
		if (type == null) {
			return null
		}
		return when (type) {
			TypedValuesTypeDto.BOOLEAN -> booleanValue as? T
			TypedValuesTypeDto.INTEGER -> integerValue as? T
			TypedValuesTypeDto.DOUBLE -> doubleValue as? T
			TypedValuesTypeDto.STRING, TypedValuesTypeDto.CLOB, TypedValuesTypeDto.JSON -> stringValue as? T
			TypedValuesTypeDto.DATE -> dateValue as? T
		}
	}

	override fun compareTo(other: TypedValueDto<T>): Int {
		return (other.getValue<T>() as Comparable<T>).compareTo(getValue<T>()!!)
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
