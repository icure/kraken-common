/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.taktik.icure.utils.InstantDeserializer
import org.taktik.icure.utils.InstantSerializer
import java.io.Serializable
import java.time.Instant
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class TypedValue(
	val type: TypedValuesType? = null,
	val booleanValue: Boolean? = null,
	val integerValue: Long? = null,
	val doubleValue: Double? = null,
	val stringValue: String? = null,

	@JsonSerialize(using = InstantSerializer::class)
	@param:JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonDeserialize(using = InstantDeserializer::class)
	val dateValue: Instant? = null,
	override val encryptedSelf: String? = null,
) : Comparable<TypedValue>,
	Encryptable,
	Serializable {
	companion object {
		fun <T> withValue(value: T?): TypedValue? = value?.let {
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
				value,
			)
		}

		fun <T> withTypeAndValue(type: TypedValuesType, value: T?): TypedValue? = value?.let {
			when (type) {
				TypedValuesType.BOOLEAN -> if (value is Boolean) {
					TypedValue(booleanValue = value, type = type)
				} else {
					throw IllegalArgumentException("Illegal boolean value")
				}
				TypedValuesType.INTEGER -> when (value) {
					is Int -> TypedValue(integerValue = value.toLong(), type = type)
					is Long -> TypedValue(integerValue = value, type = type)
					else -> throw IllegalArgumentException("Illegal integer value")
				}
				TypedValuesType.DOUBLE -> if (value is Double) {
					TypedValue(doubleValue = value, type = type)
				} else {
					throw IllegalArgumentException("Illegal double value")
				}
				TypedValuesType.STRING, TypedValuesType.JSON, TypedValuesType.CLOB -> if (value is String) {
					TypedValue(stringValue = value, type = type)
				} else {
					throw IllegalArgumentException("Illegal string value")
				}
				TypedValuesType.DATE -> if (value is Instant) {
					TypedValue(dateValue = value, type = type)
				} else if (value is Date) {
					TypedValue(dateValue = (value as Date).toInstant(), type = type)
				} else {
					throw IllegalArgumentException("Illegal date value")
				}
			}
		}
	}

	override fun compareTo(other: TypedValue): Int {
		require(other.type == type) { "Cannot compare different TypedValue types" }
		return when (type) {
			TypedValuesType.BOOLEAN -> booleanValue!!.compareTo(other.booleanValue!!)
			TypedValuesType.INTEGER -> integerValue!!.compareTo(other.integerValue!!)
			TypedValuesType.DOUBLE -> doubleValue!!.compareTo(other.doubleValue!!)
			TypedValuesType.STRING, TypedValuesType.CLOB, TypedValuesType.JSON -> stringValue!!.compareTo(other.stringValue!!)
			TypedValuesType.DATE -> dateValue!!.compareTo(other.dateValue!!)
			null -> 0
		}
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
