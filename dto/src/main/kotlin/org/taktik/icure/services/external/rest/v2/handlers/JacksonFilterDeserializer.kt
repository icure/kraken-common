package org.taktik.icure.services.external.rest.v2.handlers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.base.Preconditions
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.scanners.TypeAnnotationsScanner
import org.springframework.boot.jackson.JsonObjectDeserializer
import org.taktik.icure.handlers.JsonDiscriminated
import org.taktik.icure.handlers.JsonDiscriminator
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import java.lang.reflect.Modifier

class JacksonFilterDeserializer : JsonObjectDeserializer<AbstractFilterDto<*>>() {
	private val discriminator = AbstractFilterDto::class.java.getAnnotation(JsonDiscriminator::class.java)?.value ?: "\$type"
	private val secondaryDiscriminator = "type"
	private val secondaryTypeField = "kotlinxType"
	private val subclasses: MutableMap<String, Class<AbstractFilterDto<*>>> = HashMap()
	private val reverseSubclasses: MutableMap<Class<*>, String> = HashMap()
	private val scanner = Reflections(AbstractFilterDto::class.java, TypeAnnotationsScanner(), SubTypesScanner())

	init {
        Preconditions.checkArgument(
            Modifier.isAbstract(AbstractFilterDto::class.java.modifiers),
            "Superclass must be abstract"
        )
		val classes = scanner.getTypesAnnotatedWith(JsonPolymorphismRoot::class.java).filter { AbstractFilterDto::class.java.isAssignableFrom(it) }
		for (subClass in classes) {
			val discriminated = subClass.getAnnotation(JsonDiscriminated::class.java)
			val discriminatedString = discriminated?.value ?: subClass.simpleName
			@Suppress("UNCHECKED_CAST")
			subclasses[discriminatedString] = subClass as Class<AbstractFilterDto<*>>
			reverseSubclasses[subClass] = discriminatedString
		}
	}

	/**
	 * This is needed because Jackson and Kotlinx handle polymorphic serialization in a different way.
	 * In Jackson, we use `$type` as polymorphic discriminator because some concrete filters have a `type` attribute.
	 * However, this is forbidden by Kotlinx serialization, that considers the type attribute when serializing a
	 * polymorphic class a reserved keyword.
	 * As a workaround, the multiplatform SDK will keep using the `type` property as polymorphic discriminator and
	 * will convert the `type` attribute to `kotlinxType`.
	 * Therefore, this method will follow one of two pathways when trying to deserialize a filter:
	 * 1. if the `$type` property is in the JsonObject, it will deserialize the concrete filter directly.
	 * 2. if there is no `$type` property but there are a `type` and a `kotlinxType` properties, then it will edit
	 * the tree setting the value of `type` to the value of `kotlinxType`, and then deserializing the object.
	 * All the other cases are considered erroneous.
	 */
	override fun deserializeObject(jsonParser: JsonParser?, context: DeserializationContext?, codec: ObjectCodec, tree: JsonNode): AbstractFilterDto<*> =
		tree[discriminator]?.textValue()?.let {
			deserializeObjectUsingDiscriminator(it, codec, tree)
		} ?: tree[secondaryDiscriminator]?.textValue()?.let {
			val typeValue = tree[secondaryTypeField]
			if(typeValue != null && tree is ObjectNode) {
				tree.set<JsonNode>(secondaryDiscriminator, typeValue)
				deserializeObjectUsingDiscriminator(it, codec, tree)
			} else throw IllegalArgumentException("Invalid alternative format for Filter JSON")
		} ?: throw IllegalArgumentException("Invalid JSON filter format")

	private fun deserializeObjectUsingDiscriminator(discriminator: String, codec: ObjectCodec, tree: JsonNode): AbstractFilterDto<*> {
		val selectedSubClass = requireNotNull(subclasses[discriminator]) { "Invalid subclass $discriminator in object" }
		return codec.treeToValue(tree, selectedSubClass)
	}
}
