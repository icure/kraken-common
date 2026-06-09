package org.taktik.icure.entities.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import org.taktik.icure.entities.RawJson

object RawJsonSerializer : KSerializer<RawJson> {
	override val descriptor: SerialDescriptor get() =
		throw UnsupportedOperationException("RawJson does not have a serial descriptor")

	override fun serialize(encoder: Encoder, value: RawJson) {
		if (encoder !is JsonEncoder) throw SerializationException("RawJson can only be serialized with a Json encoder")
		val adapter = KotlinxJsonGeneratorAdapter()
		value.writeTo(adapter)
		encoder.encodeJsonElement(adapter.result)
	}

	override fun deserialize(decoder: Decoder): RawJson {
		if (decoder !is JsonDecoder) throw SerializationException("RawJson can only be deserialized with a Json decoder")
		return decoder.decodeJsonElement().toRawJson()
	}
}

object RawJsonObjectSerializer : KSerializer<RawJson.JsonObject> {
	override val descriptor: SerialDescriptor
		get() = throw UnsupportedOperationException("RawJson does not have a serial descriptor")

	override fun serialize(encoder: Encoder, value: RawJson.JsonObject) {
		RawJsonSerializer.serialize(encoder, value)
	}

	override fun deserialize(decoder: Decoder): RawJson.JsonObject {
		if (decoder !is JsonDecoder) throw SerializationException("RawJson.JsonObject can only be deserialized with a Json decoder")
		val element = decoder.decodeJsonElement()
		if (element !is JsonObject) throw SerializationException("Expected JSON object but got ${element::class.simpleName}")
		return element.toRawJson() as RawJson.JsonObject
	}
}

private class KotlinxJsonGeneratorAdapter : MultiplatformJsonGenerator {
	private sealed interface Frame {
		class ObjectFrame : Frame {
			val map = mutableMapOf<String, JsonElement>()
			var pendingKey: String? = null
		}
		class ArrayFrame : Frame {
			val items = mutableListOf<JsonElement>()
		}
	}

	private val stack = ArrayDeque<Frame>()
	var result: JsonElement = JsonNull
		private set

	private fun push(element: JsonElement) {
		when (val frame = stack.lastOrNull()) {
			is Frame.ObjectFrame -> {
				val key = checkNotNull(frame.pendingKey) {
					"writeFieldName must be called before writing a value in an object"
				}
				frame.map[key] = element
				frame.pendingKey = null
			}
			is Frame.ArrayFrame -> frame.items.add(element)
			null -> result = element
		}
	}

	override fun writeStartObject() {
		stack.addLast(Frame.ObjectFrame())
	}

	override fun writeFieldName(key: String) {
		(stack.last() as Frame.ObjectFrame).pendingKey = key
	}

	override fun writeEndObject() {
		val frame = stack.removeLast() as Frame.ObjectFrame
		push(JsonObject(frame.map))
	}

	override fun writeStartArray() {
		stack.addLast(Frame.ArrayFrame())
	}

	override fun writeEndArray() {
		val frame = stack.removeLast() as Frame.ArrayFrame
		push(JsonArray(frame.items))
	}

	override fun writeString(value: String) = push(JsonPrimitive(value))
	override fun writeNumber(value: Long) = push(JsonPrimitive(value))
	override fun writeNumber(value: Double) = push(JsonPrimitive(value))
	override fun writeBoolean(bool: Boolean) = push(JsonPrimitive(bool))
	override fun writeNull() = push(JsonNull)
}

private fun JsonElement.toRawJson(): RawJson = when (this) {
	is JsonObject -> RawJson.JsonObject(mapValues { (_, v) -> v.toRawJson() })
	is JsonArray -> RawJson.JsonArray(map { it.toRawJson() })
	is JsonNull -> RawJson.JsonNull
	is JsonPrimitive -> when {
		isString -> RawJson.JsonString(content)
		booleanOrNull != null -> if (boolean) RawJson.JsonBoolean.True else RawJson.JsonBoolean.False
		longOrNull != null -> RawJson.JsonInteger(long)
		content.none { it == '.' || it == 'e' || it == 'E' } ->
			throw SerializationException("Integer value out of range: $content")
		doubleOrNull != null -> {
			val d = double
			if (!d.isFinite()) throw SerializationException("Float value out of range: $content")
			RawJson.JsonFloat(d)
		}
		else -> throw SerializationException("Unexpected JSON primitive: $content")
	}
}