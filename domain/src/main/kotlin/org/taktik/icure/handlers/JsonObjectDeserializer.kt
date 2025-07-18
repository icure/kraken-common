package org.taktik.icure.handlers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Helper base class for [JsonDeserializer] implementations that deserialize
 * objects.
 *
 * @param <T> the supported object type
 * @author Phillip Webb
 * @since 1.4.0
 * @see JsonObjectSerializer
</T> */
abstract class JsonObjectDeserializer<T> : JsonDeserializer<T>() {
	@Throws(IOException::class)
	override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): T = try {
		val codec = jp.codec
		val tree = codec.readTree<JsonNode>(jp)
		deserializeObject(jp, ctxt, codec, tree)
	} catch (ex: Exception) {
		if (ex is IOException) {
			throw ex
		}
		throw JsonMappingException(jp, "Object deserialize error", ex)
	}

	/**
	 * Deserialize JSON content into the value type this serializer handles.
	 * @param jsonParser the source parser used for reading JSON content
	 * @param context context that can be used to access information about this
	 * deserialization activity
	 * @param codec the [ObjectCodec] associated with the parser
	 * @param tree deserialized JSON content as tree expressed using set of
	 * [TreeNode] instances
	 * @return the deserialized object
	 * @throws IOException on error
	 * @see .deserialize
	 */
	@Throws(IOException::class)
	protected abstract fun deserializeObject(
		jsonParser: JsonParser?,
		context: DeserializationContext?,
		codec: ObjectCodec,
		tree: JsonNode,
	): T

	/**
	 * Helper method to extract a value from the given `jsonNode` or return
	 * `null` when the node itself is `null`.
	 * @param jsonNode the source node (may be `null`)
	 * @param type the data type. May be [String], [Boolean], [Long],
	 * [Integer], [Short], [Double], [Float], [BigDecimal]
	 * or [BigInteger].
	 * @param <D> the data type requested
	 * @return the node value or `null`
	 </D> */
	protected fun <D> nullSafeValue(jsonNode: JsonNode?, type: Class<D>): D? = when {
		jsonNode == null -> null
		type == String::class.java -> jsonNode.textValue() as D
		type == Boolean::class.java -> java.lang.Boolean.valueOf(jsonNode.booleanValue()) as D
		type == Long::class.java -> java.lang.Long.valueOf(jsonNode.longValue()) as D
		type == Int::class.java -> Integer.valueOf(jsonNode.intValue()) as D
		type == Short::class.java -> jsonNode.shortValue() as D
		type == Double::class.java -> java.lang.Double.valueOf(jsonNode.doubleValue()) as D
		type == Float::class.java -> java.lang.Float.valueOf(jsonNode.floatValue()) as D
		type == BigDecimal::class.java -> jsonNode.decimalValue() as D
		type == BigInteger::class.java -> jsonNode.bigIntegerValue() as D
		else -> throw IllegalArgumentException("Unsupported value type " + type.name)
	}

	/**
	 * Helper method to return a [JsonNode] from the tree.
	 * @param tree the source tree
	 * @param fieldName the field name to extract
	 * @return the [JsonNode]
	 */
	protected fun getRequiredNode(tree: JsonNode, fieldName: String): JsonNode? {
		val node = tree[fieldName]
		require(node != null && node !is NullNode) { "Missing JSON field '$fieldName'" }
		return node
	}
}
