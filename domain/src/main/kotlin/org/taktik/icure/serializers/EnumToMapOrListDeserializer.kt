package org.taktik.icure.serializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode

abstract class EnumToMapOrListDeserializer<T>(private val enumClass: Class<T>) : JsonDeserializer<Map<T, Map<String, String>>>() {
	override fun deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): Map<T, Map<String, String>> = jsonParser.codec.readTree<ObjectNode>(jsonParser).fields().asSequence().associate { (k, v) ->
		val parsedK = ctxt.readValue(TextNode(k).traverse().also { it.nextToken() }, enumClass)
		parsedK to when {
			v.isArray -> deserializeArray(v)
			v.isObject -> deserializeObject(v)
			else -> throw IllegalArgumentException("Expected array or object, got $v")
		}
	}

	private fun deserializeObject(node: JsonNode): Map<String, String> = (node as ObjectNode).fields().asSequence().associate { (k, v) ->
		val vText = requireNotNull(v.textValue()) { "Expected textual value, got $v" }
		k to vText
	}

	/**
	 * The purpose of this function is to transform the actual list of String (in JsonNode) to a Map<String, String>
	 * This fixes entities that are stored in a wrong format
	 */
	private fun deserializeArray(node: JsonNode): Map<String, String> = (node as ArrayNode).elements().asSequence().associate { v ->
		val text = requireNotNull(v.textValue()) { "Expected textual value, got $v" }
		text to text
	}
}
