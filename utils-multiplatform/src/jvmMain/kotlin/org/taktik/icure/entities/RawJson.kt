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
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.Serializers
import org.taktik.icure.entities.serialization.MultiplatformJsonGenerator
import kotlin.collections.iterator
 
class RawJsonJacksonModule : SimpleModule("RawJson") {
	init {
		addSerializer(RawJson::class.java, RawJsonSerializer)
		addDeserializer(RawJson::class.java, RawJsonDeserializer)
		addSerializer(RawJson.JsonObject::class.java, RawJsonSerializer)
		addDeserializer(RawJson.JsonObject::class.java, RawJsonObjectDeserializer)
	}
}

private class MultiplatformGeneratorAdapter(
	private val generator: JsonGenerator
) : MultiplatformJsonGenerator {
	override fun writeStartObject() {
		generator.writeStartObject()
	}

	override fun writeFieldName(key: String) {
		generator.writeFieldName(key)
	}

	override fun writeEndObject() {
		generator.writeEndObject()
	}

	override fun writeStartArray() {
		generator.writeStartArray()
	}

	override fun writeEndArray() {
		generator.writeEndArray()
	}

	override fun writeString(value: String) {
		generator.writeString(value)
	}

	override fun writeNumber(value: Long) {
		generator.writeNumber(value)
	}

	override fun writeNumber(value: Double) {
		generator.writeNumber(value)
	}

	override fun writeBoolean(bool: Boolean) {
		generator.writeBoolean(bool)
	}

	override fun writeNull() {
		generator.writeNull()
	}
}

private object RawJsonSerializer : JsonSerializer<RawJson>() {
	override fun serialize(
		value: RawJson,
		generator: JsonGenerator,
		serializers: SerializerProvider
	) {
		value.writeTo(MultiplatformGeneratorAdapter(generator))
	}
}


private object RawJsonObjectDeserializer : JsonDeserializer<RawJson.JsonObject>() {
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

private object RawJsonDeserializer : JsonDeserializer<RawJson>() {
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
