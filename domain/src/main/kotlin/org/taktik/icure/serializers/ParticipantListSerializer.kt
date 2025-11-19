package org.taktik.icure.serializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.taktik.icure.entities.base.ParticipantType
import org.taktik.icure.entities.embed.ContactParticipant

class ParticipantListSerializer: JsonDeserializer<Collection<ContactParticipant>>() {
	override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Collection<ContactParticipant> {
		return when (val node = p.readValueAsTree<JsonNode>()) {
			is ArrayNode -> {
				// Should be a list of ContactParticipant
				node.map {
					p.codec.treeToValue(it, ContactParticipant::class.java)
				}
			}
			is ObjectNode -> {
				// Should be a Map<ParticipantType, String>

				node.properties().map { (k, v) ->
					ContactParticipant(
						type = ParticipantType.valueOf(k),
						hcpId = v.asText()
					)
				}.toList()
			}
			else -> throw IllegalArgumentException("Expected array or object, got $node")
		}
	}
}