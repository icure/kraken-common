package org.taktik.icure.entities

import org.taktik.icure.entities.serialization.MultiplatformJsonGenerator
import kotlin.collections.iterator

sealed interface RawJson {
	fun writeTo(generator: MultiplatformJsonGenerator): Unit

	data class JsonObject(val properties: Map<String, RawJson>) : RawJson {
		override fun writeTo(generator: MultiplatformJsonGenerator) {
			generator.writeStartObject()
			for ((key, value) in properties) {
				generator.writeFieldName(key)
				value.writeTo(generator)
			}
			generator.writeEndObject()
		}

		companion object {
			val empty = JsonObject(emptyMap())
		}
	}

	data class JsonArray(val items: List<RawJson>) : RawJson {
		companion object {
			val empty = JsonArray(emptyList())
		}

		override fun writeTo(generator: MultiplatformJsonGenerator) {
			generator.writeStartArray()
			for (item in items) {
				item.writeTo(generator)
			}
			generator.writeEndArray()
		}
	}


	data class JsonString(val value: String) : RawJson {
		override fun writeTo(generator: MultiplatformJsonGenerator) {
			generator.writeString(value)
		}
	}

	sealed interface JsonNumber : RawJson {
		/**
		 * The value as a double.
		 * This method may return infinity, NaN, or a rounded value if the number cannot be exactly represented as a
		 * double.
		 */
		fun asDouble(): Double
	}

	data class JsonInteger(val value: Long) : JsonNumber {
		override fun writeTo(generator: MultiplatformJsonGenerator) {
			generator.writeNumber(value)
		}

		override fun asDouble(): Double =
			value.toDouble()

		fun asExactIntOrNull(): Int? =
			if (value <= Int.MAX_VALUE.toLong() && value >= Int.MIN_VALUE.toLong()) {
				value.toInt()
			} else {
				null
			}
	}

	// We don't want to support integers outside Long range.
	// Since we don't support using the numeric value on the views they can be instead represented as strings.
//	data class JsonBigInteger(val value: String) : JsonNumber {
//		override fun writeTo(generator: MultiplatformJsonGenerator) {
//			generator.writeNumber(value)
//		}
//
//		override fun asDouble(): Double =
//			value.toDoubleOrNull() ?: Double.NaN
//	}

	data class JsonFloat(val value: Double) : JsonNumber {
		override fun writeTo(generator: MultiplatformJsonGenerator) {
			generator.writeNumber(value)
		}

		override fun asDouble(): Double =
			value
	}

	sealed interface JsonBoolean : RawJson {
		data object True : JsonBoolean {
			override fun writeTo(generator: MultiplatformJsonGenerator) {
				generator.writeBoolean(true)
			}
		}

		data object False : JsonBoolean {
			override fun writeTo(generator: MultiplatformJsonGenerator) {
				generator.writeBoolean(false)
			}
		}
	}

	data object JsonNull : RawJson {
		override fun writeTo(generator: MultiplatformJsonGenerator) {
			generator.writeNull()
		}
	}
}