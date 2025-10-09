package org.taktik.icure.handlers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.nio.ByteBuffer
import java.util.Base64

class LongAsBase64Deserializer : JsonDeserializer<Long>() {
	override fun deserialize(
		p: JsonParser,
		ctxt: DeserializationContext?
	): Long =
		base64ToLong(p.valueAsString)

	companion object {
		fun base64ToLong(base64: String): Long {
			return ByteBuffer.wrap(Base64.getDecoder().decode(base64)).long
		}
	}
}

class LongAsBase64Serializer : JsonSerializer<Long>() {
	override fun serialize(
		value: Long,
		gen: JsonGenerator,
		serializers: SerializerProvider
	) =
		gen.writeString(longToBase64(value))

	companion object {
		fun longToBase64(value: Long): String {
			return Base64.getEncoder().encodeToString(ByteBuffer.allocate(Long.SIZE_BYTES).putLong(value).array()).take(11)
		}
	}
}
