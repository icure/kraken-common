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
	/** The type of the value stored. */
	val type: TypedValuesType? = null,
	/** The boolean value, if type is BOOLEAN. */
	val booleanValue: Boolean? = null,
	/** The integer value, if type is INTEGER. */
	val integerValue: Long? = null,
	/** The double value, if type is DOUBLE. */
	val doubleValue: Double? = null,
	/** The string value, if type is STRING, JSON, or CLOB. */
	val stringValue: String? = null,

	/** The date value as an Instant, if type is DATE. */
	@param:JsonSerialize(using = InstantSerializer::class)
	@param:JsonInclude(JsonInclude.Include.NON_NULL)
	@param:JsonDeserialize(using = InstantDeserializer::class)
	val dateValue: Instant? = null,
	/** The base64-encoded encrypted content. */
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
				TypedValuesType.DATE -> when (value) {
					is Instant -> TypedValue(dateValue = value, type = type)
					is Date -> TypedValue(dateValue = (value as Date).toInstant(), type = type)
					else -> throw IllegalArgumentException("Illegal date value")
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
			return when (type) {
				TypedValuesType.BOOLEAN -> booleanValue.toString()
				TypedValuesType.INTEGER -> integerValue.toString()
				TypedValuesType.DOUBLE -> doubleValue.toString()
				TypedValuesType.STRING, TypedValuesType.CLOB, TypedValuesType.JSON -> stringValue!!
				TypedValuesType.DATE -> dateValue.toString()
			}
		}
		return super.toString()
	}
}
