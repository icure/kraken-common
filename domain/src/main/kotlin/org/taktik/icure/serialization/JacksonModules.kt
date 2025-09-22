package org.taktik.icure.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.addDeserializer
import com.fasterxml.jackson.module.kotlin.addSerializer

object JacksonModules {
	/**
	 * A module that replaces the standard [Float] and [Double] serializers with a stricter implementation:
	 * - NaN and infinite values are not allowed as input or output
	 * - There is no coercion done, regardless of the configurations
	 */
	val strictFloatsModule = SimpleModule().apply {
		addDeserializer(Double::class, StrictDoubleDeserializer)
		addSerializer(Double::class, StrictDoubleSerializer)
		addDeserializer(Float::class, StrictFloatDeserializer)
		addSerializer(Float::class, StrictFloatSerializer)
	}
}

private object StrictDoubleDeserializer : JsonDeserializer<Double>() {
	override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Double {
		return if (p.currentToken.isNumeric) {
			val res = p.doubleValue
			if (res.isFinite()) res else throw JsonMappingException.from(p, "Number out of range for Double")
		} else throw MismatchedInputException.from(p, Double::class.java, "Expected numeric value")
	}
}

private object StrictFloatDeserializer : JsonDeserializer<Float>() {
	override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Float {
		return if (p.currentToken.isNumeric) {
			val res = p.floatValue
			if (res.isFinite()) res else throw JsonMappingException.from(p, "Number out of range for Float")
		} else throw MismatchedInputException.from(p, Float::class.java, "Expected numeric value")
	}
}

private object StrictDoubleSerializer : JsonSerializer<Double>() {
	override fun serialize(
		value: Double,
		gen: JsonGenerator,
		serializers: SerializerProvider?
	) {
		check (value.isFinite()) { "Invalid double value: NaN/Infinity" }
		gen.writeNumber(value)
	}
}

private object StrictFloatSerializer : JsonSerializer<Float>() {
	override fun serialize(
		value: Float,
		gen: JsonGenerator,
		serializers: SerializerProvider?
	) {
		check (value.isFinite()) { "Invalid float value: NaN/Infinity" }
		gen.writeNumber(value)
	}
}
