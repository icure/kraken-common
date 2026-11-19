package org.taktik.icure.services.external.rest.v2.handlers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.boot.jackson.JsonObjectDeserializer
import org.taktik.icure.handlers.JsonDiscriminator
import org.taktik.icure.handlers.scanPolymorphicSubclasses
import org.taktik.icure.services.external.rest.v2.dto.filter.predicate.Predicate

class JacksonPredicateDeserializer : JsonObjectDeserializer<Predicate>() {
	companion object {
		private val discriminator = Predicate::class.java.getAnnotation(JsonDiscriminator::class.java)?.value ?: "\$type"

		// Classpath scan: done once per class, not per deserializer instance.
		private val subclasses: Map<String, Class<Predicate>> = scanPolymorphicSubclasses(Predicate::class.java, requireAbstractRoot = true)
	}

	override fun isCachable(): Boolean = true

	override fun deserializeObject(
		jsonParser: JsonParser?,
		context: DeserializationContext?,
		codec: ObjectCodec,
		tree: JsonNode,
	): Predicate {
		val discr = tree[discriminator].textValue() ?: throw IllegalArgumentException("Missing discriminator $discriminator in object")
		val selectedSubClass = subclasses[discr] ?: throw IllegalArgumentException("Invalid subclass $discr in object")
		return codec.treeToValue(tree, selectedSubClass)
	}
}
