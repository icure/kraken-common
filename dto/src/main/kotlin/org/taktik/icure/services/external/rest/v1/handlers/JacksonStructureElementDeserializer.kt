package org.taktik.icure.services.external.rest.v1.handlers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.boot.jackson.JsonObjectDeserializer
import org.taktik.icure.services.external.rest.v1.dto.embed.form.template.Field
import org.taktik.icure.services.external.rest.v1.dto.embed.form.template.Group
import org.taktik.icure.services.external.rest.v1.dto.embed.form.template.StructureElement

class JacksonStructureElementDeserializer : JsonObjectDeserializer<StructureElement>() {
	override fun deserializeObject(
		jsonParser: JsonParser?,
		context: DeserializationContext?,
		codec: ObjectCodec,
		tree: JsonNode,
	): StructureElement = if (tree["field"] != null) codec.treeToValue(tree, Field::class.java) else codec.treeToValue(tree, Group::class.java)
}
