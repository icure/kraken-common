package org.taktik.icure.handlers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * De-serializer for maps with string keys whose values are either strings or nested maps of the same kind, such as
 * `Map<String, Map<String, Map<String, String>>>`. The nesting depth is taken from the declared type of the property.
 *
 * At every level, the entries whose value does not match the declared type are discarded:
 * - where a nested map is expected, anything other than a JSON object (including `null`) is discarded;
 * - where a string is expected, `null`, object and array values are discarded. Number and boolean values are kept using
 *   their textual representation, as Jackson does by default for strings.
 */
class JacksonLenientNestedStringMapDeserializer(
	private val depth: Int = 1,
) : JsonDeserializer<Map<String, Any>>(),
	ContextualDeserializer {

	override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<String, Any> {
		val node = p.readValueAsTree<JsonNode>()
		if (node !is ObjectNode) {
			return ctxt.reportInputMismatch(this, "Expected a JSON object but got ${node.nodeType}")
		}
		return node.toLenientMap(depth)
	}

	private fun ObjectNode.toLenientMap(depth: Int): Map<String, Any> = properties().mapNotNull { (key, value) ->
		when {
			depth > 1 -> (value as? ObjectNode)?.toLenientMap(depth - 1)
			value.isTextual || value.isNumber || value.isBoolean -> value.asText()
			else -> null
		}?.let { key to it }
	}.toMap()

	override fun createContextual(ctxt: DeserializationContext, property: BeanProperty?): JsonDeserializer<*> = JacksonLenientNestedStringMapDeserializer(
		generateSequence(property?.type) { it.contentType }.takeWhile { it.isMapLikeType }.count().coerceAtLeast(1),
	)
}
