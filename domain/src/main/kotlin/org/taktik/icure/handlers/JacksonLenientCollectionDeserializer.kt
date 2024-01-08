package org.taktik.icure.handlers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.node.ArrayNode

class JacksonLenientCollectionDeserializer(
	private val collectionType: JavaType? = null,
	val elementType: JavaType? = null
) : JsonDeserializer<Collection<*>>(), ContextualDeserializer {

	val mapper: ObjectMapper by lazy { ObjectMapper() }

	override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Collection<*> {
		return p.readValueAsTree<ArrayNode>()
			.filter { !it.isNull }
			.map {
				mapper.treeToValue(it, elementType?.rawClass)
			}.let {
				when(collectionType?.rawClass?.simpleName) {
					"Set" -> it.toSet()
					"List" -> it.toList()
					else -> it
				}
			}
	}

	override fun createContextual(ctxt: DeserializationContext?, property: BeanProperty?): JsonDeserializer<*> {
		return JacksonLenientCollectionDeserializer(
			property?.type,
			property?.type?.containedType(0)
		)
	}
}
