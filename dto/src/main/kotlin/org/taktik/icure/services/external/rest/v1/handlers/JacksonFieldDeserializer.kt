package org.taktik.icure.services.external.rest.v1.handlers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.scanners.TypeAnnotationsScanner
import org.springframework.boot.jackson.JsonObjectDeserializer
import org.taktik.icure.handlers.JsonDiscriminated
import org.taktik.icure.handlers.JsonDiscriminator
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v1.dto.embed.form.template.Field

class JacksonFieldDeserializer : JsonObjectDeserializer<Field>() {
	private val discriminator = Field::class.java.getAnnotation(JsonDiscriminator::class.java)?.value ?: "type"
	private val subclasses: MutableMap<String, Class<Field>> = HashMap()
	private val reverseSubclasses: MutableMap<Class<*>, String> = HashMap()
	private val scanner = Reflections(Field::class.java, TypeAnnotationsScanner(), SubTypesScanner())

	init {
		val classes = scanner.getTypesAnnotatedWith(JsonPolymorphismRoot::class.java).filter { Field::class.java.isAssignableFrom(it) }
		for (subClass in classes) {
			val discriminated = subClass.getAnnotation(JsonDiscriminated::class.java)
			val discriminatedString = discriminated?.value ?: subClass.simpleName
			subclasses[discriminatedString] = subClass as Class<Field>
			reverseSubclasses[subClass] = discriminatedString
		}
	}

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
