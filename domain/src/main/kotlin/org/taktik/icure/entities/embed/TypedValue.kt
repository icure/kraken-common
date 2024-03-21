/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import java.io.Serializable
import java.time.Instant
import java.util.Date
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.taktik.icure.entities.embed.TypedValuesType
import org.taktik.icure.utils.InstantDeserializer
import org.taktik.icure.utils.InstantSerializer

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class TypedValue<T>(
    val type: TypedValuesType? = null,
    val booleanValue: Boolean? = null,
    val integerValue: Long? = null,
    val doubleValue: Double? = null,
    val stringValue: String? = null,

    @JsonSerialize(using = InstantSerializer::class)
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonDeserialize(using = InstantDeserializer::class)
	val dateValue: Instant? = null,
    override val encryptedSelf: String? = null
) : Comparable<TypedValue<T>>, Encrypted, Serializable {
	companion object {
		fun <T> withValue(value: T?): TypedValue<T>? = value?.let {
			withTypeAndValue(
				when (value) {
					is Boolean -> TypedValuesType.BOOLEAN
					is Int -> TypedValuesType.INTEGER
					is Long -> TypedValuesType.INTEGER
					is Double -> TypedValuesType.DOUBLE
					is String -> TypedValuesType.STRING
					is Date -> TypedValuesType.DATE
					else -> throw IllegalArgumentException("Unknown value type")
				},
				value
			)
		}

		fun <T> withTypeAndValue(type: TypedValuesType, value: T?): TypedValue<T>? = value?.let {
			when (type) {
				TypedValuesType.BOOLEAN -> if (value is Boolean) {
					TypedValue(booleanValue = value, type = type)
				} else throw IllegalArgumentException("Illegal boolean value")
				TypedValuesType.INTEGER -> when (value) {
					is Int -> TypedValue(integerValue = value.toLong(), type = type)
					is Long -> TypedValue(integerValue = value, type = type)
					else -> throw IllegalArgumentException("Illegal integer value")
				}
				TypedValuesType.DOUBLE -> if (value is Double) {
					TypedValue(doubleValue = value, type = type)
				} else throw IllegalArgumentException("Illegal double value")
				TypedValuesType.STRING, TypedValuesType.JSON, TypedValuesType.CLOB -> if (value is String) {
					TypedValue(stringValue = value, type = type)
				} else throw IllegalArgumentException("Illegal string value")
				TypedValuesType.DATE -> if (value is Instant) {
					TypedValue(dateValue = value, type = type)
				} else if (value is Date) {
					TypedValue(dateValue = (value as Date).toInstant(), type = type)
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
			TypedValuesType.BOOLEAN -> booleanValue as? T
			TypedValuesType.INTEGER -> integerValue as? T
			TypedValuesType.DOUBLE -> doubleValue as? T
			TypedValuesType.STRING, TypedValuesType.CLOB, TypedValuesType.JSON -> stringValue as? T
			TypedValuesType.DATE -> dateValue as? T
		}
	}

	override fun compareTo(other: TypedValue<T>): Int {
		return (other.getValue<T>() as Comparable<T>).compareTo(getValue<T>()!!)
	}

	override fun toString(): String {
		if (type != null) {
			when (type) {
				TypedValuesType.BOOLEAN -> return booleanValue.toString()
				TypedValuesType.INTEGER -> return integerValue.toString()
				TypedValuesType.DOUBLE -> return doubleValue.toString()
				TypedValuesType.STRING, TypedValuesType.CLOB, TypedValuesType.JSON -> return stringValue!!
				TypedValuesType.DATE -> return dateValue.toString()
			}
		}
		return super.toString()
	}
}
