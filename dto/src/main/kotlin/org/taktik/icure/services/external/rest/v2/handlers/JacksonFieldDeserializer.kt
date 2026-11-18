package org.taktik.icure.services.external.rest.v2.handlers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.boot.jackson.JsonObjectDeserializer
import org.taktik.icure.handlers.JsonDiscriminator
import org.taktik.icure.handlers.scanPolymorphicSubclasses
import org.taktik.icure.services.external.rest.v2.dto.embed.form.template.Field

class JacksonFieldDeserializer : JsonObjectDeserializer<Field>() {
	companion object {
		private val discriminator = Field::class.java.getAnnotation(JsonDiscriminator::class.java)?.value ?: "type"

		// Classpath scan: done once per class, not per deserializer instance.
		private val subclasses: Map<String, Class<Field>> = scanPolymorphicSubclasses(Field::class.java)
	}

	override fun isCachable(): Boolean = true

	override fun deserializeObject(
		jsonParser: JsonParser?,
		context: DeserializationContext?,
		codec: ObjectCodec,
		tree: JsonNode,
	): Field {
		val discr = tree[discriminator].textValue() ?: throw IllegalArgumentException("Missing discriminator $discriminator in object")
		val selectedSubClass = subclasses[discr] ?: throw IllegalArgumentException("Invalid subclass $discr in object")
		return codec.treeToValue(tree, selectedSubClass)
	}
}
