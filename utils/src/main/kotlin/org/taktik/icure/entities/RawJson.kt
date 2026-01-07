package org.taktik.icure.entities

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.exc.InputCoercionException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize


object RawJsonSerializer : JsonSerializer<RawJson>() {
	override fun serialize(
		value: RawJson,
		generator: JsonGenerator,
		serializers: SerializerProvider
	) {
		value.writeTo(generator)
	}
}


object RawJsonObjectDeserializer : JsonDeserializer<RawJson.JsonObject>() {
	override fun deserialize(
		p: JsonParser,
		ctxt: DeserializationContext
	): RawJson.JsonObject {
		if (p.currentToken != JsonToken.START_OBJECT) {
			throw JsonMappingException.from(
				p,
				"Expected json object"
			)
		}
		return RawJsonDeserializer.deserialize(p, ctxt) as RawJson.JsonObject
	}
}

object RawJsonDeserializer : JsonDeserializer<RawJson>() {
	override fun getNullValue(ctxt: DeserializationContext): RawJson {
		return RawJson.JsonNull
	}

	override fun deserialize(
		p: JsonParser,
		ctxt: DeserializationContext
	): RawJson {
		return when (p.currentToken()) {
			JsonToken.START_OBJECT -> {
				val properties = mutableMapOf<String, RawJson>()
				while (p.nextToken() != JsonToken.END_OBJECT) {
					val fieldName = p.currentName()
					p.nextToken()
					val value = deserialize(p, ctxt)
					properties[fieldName] = value
				}
				RawJson.JsonObject(properties)
			}
			JsonToken.START_ARRAY -> {
				val items = mutableListOf<RawJson>()
				while (p.nextToken() != JsonToken.END_ARRAY) {
					val item = deserialize(p, ctxt)
					items.add(item)
				}
				RawJson.JsonArray(items)
			}
			JsonToken.VALUE_STRING -> {
				RawJson.JsonString(p.valueAsString)
			}
			JsonToken.VALUE_NUMBER_INT -> {
				if (p.numberType == JsonParser.NumberType.BIG_INTEGER) {
					throw JsonMappingException.from(p, "Integer value out of range")
				} else {
					RawJson.JsonInteger(p.longValue)
				}
			}
			JsonToken.VALUE_NUMBER_FLOAT -> {
				try {
					RawJson.JsonFloat(p.doubleValue)
				} catch (e: InputCoercionException) {
					throw JsonMappingException.from(p, "Float value out of range", e)
				}.also {
					if (!it.value.isFinite()) throw JsonMappingException.from(p, "Float value out of range")
				}
			}
			JsonToken.VALUE_TRUE -> {
				RawJson.JsonBoolean.True
			}
			JsonToken.VALUE_FALSE -> {
				RawJson.JsonBoolean.False
			}
			JsonToken.VALUE_NULL -> {
				RawJson.JsonNull
			}
			else -> {
				throw JsonMappingException.from(p, "Unexpected token: ${p.currentToken()}")
			}
		}
	}
}


@JsonDeserialize(using = RawJsonDeserializer::class)
@JsonSerialize(using = RawJsonSerializer::class)
sealed interface RawJson {
	fun writeTo(generator: JsonGenerator): Unit

	@JsonDeserialize(using = RawJsonObjectDeserializer::class)
	@JsonSerialize(using = RawJsonSerializer::class)
	data class JsonObject(val properties: Map<String, RawJson>) : RawJson {
		override fun writeTo(generator: JsonGenerator) {
			generator.writeStartObject()
			for ((key, value) in properties) {
				generator.writeFieldName(key)
				value.writeTo(generator)
			}
			generator.writeEndObject()
		}
	}

	data class JsonArray(val items: List<RawJson>) : RawJson {
		override fun writeTo(generator: JsonGenerator) {
			generator.writeStartArray()
			for (item in items) {
				item.writeTo(generator)
			}
			generator.writeEndArray()
		}
	}


	data class JsonString(val value: String) : RawJson {
		override fun writeTo(generator: JsonGenerator) {
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
		override fun writeTo(generator: JsonGenerator) {
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
//		override fun writeTo(generator: JsonGenerator) {
//			generator.writeNumber(value)
//		}
//
//		override fun asDouble(): Double =
//			value.toDoubleOrNull() ?: Double.NaN
//	}

	data class JsonFloat(val value: Double) : JsonNumber {
		override fun writeTo(generator: JsonGenerator) {
			generator.writeNumber(value)
		}

		override fun asDouble(): Double =
			value
	}

	sealed interface JsonBoolean : RawJson {
		data object True : JsonBoolean {
			override fun writeTo(generator: JsonGenerator) {
				generator.writeBoolean(true)
			}
		}

		data object False : JsonBoolean {
			override fun writeTo(generator: JsonGenerator) {
				generator.writeBoolean(false)
			}
		}
	}

	data object JsonNull : RawJson {
		override fun writeTo(generator: JsonGenerator) {
			generator.writeNull()
		}
	}
}