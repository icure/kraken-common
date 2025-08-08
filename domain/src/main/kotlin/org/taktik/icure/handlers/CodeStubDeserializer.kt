/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.handlers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import org.taktik.icure.entities.base.CodeStub

class CodeStubDeserializer : JsonObjectDeserializer<CodeStub>() {
	override fun deserializeObject(jsonParser: JsonParser?, context: DeserializationContext?, codec: ObjectCodec, tree: JsonNode): CodeStub {
		val id = tree["_id"]?.textValue()
		val code = tree["code"]?.textValue()
		val type = tree["type"]?.textValue()
		val version = tree["version"]?.textValue()
		val context = tree["context"]?.textValue()
		val contextLabel = tree["contextLabel"]?.textValue()
		val label = tree["label"]

		val codeStub = CodeStub(
			id = id
				?: "$type|$code|$version",
			code = code,
			type = type,
			version = version,
			context = context,
			contextLabel = contextLabel,
			label = label?.let { codec.treeToValue(it, Map::class.java) as Map<String, String> } ?: mapOf(),
		)
		return codeStub
	}
}
