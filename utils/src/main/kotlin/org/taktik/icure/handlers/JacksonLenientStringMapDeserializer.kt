package org.taktik.icure.handlers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * De-serializer for `Map<String, String>` that discards the entries whose value is not a string.
 *
 * - `null`, object and array values are discarded.
 * - Number and boolean values are kept using their textual representation, as Jackson does by default for strings.
 */
class JacksonLenientStringMapDeserializer : JsonDeserializer<Map<String, String>>() {

	override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<String, String> {
		val node = p.readValueAsTree<JsonNode>()
		if (node !is ObjectNode) {
			return ctxt.reportInputMismatch(this, "Expected a JSON object but got ${node.nodeType}")
		}
		return node.properties().mapNotNull { (key, value) ->
			if (value.isTextual || value.isNumber || value.isBoolean) key to value.asText() else null
		}.toMap()
	}
}
