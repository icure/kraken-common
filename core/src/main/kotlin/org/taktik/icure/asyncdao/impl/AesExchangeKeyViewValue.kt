package org.taktik.icure.asyncdao.impl

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.node.ArrayNode

/**
 * Value of a row of the `by_delegate_aes_exchange_keys` views of HealthcareParty, Patient and Device, emitted as
 * `[ownerId, ownerPublicKeyFingerprint, delegatePublicKeyFingerprint, encryptedExchangeKey]`.
 *
 * The views read the raw documents, so a corrupted `aesExchangeKeys` or legacy `hcPartyKeys` entry can produce a row
 * value that does not have this shape (e.g. a null exchange key). These row values are deserialized as null, so that
 * they are dropped instead of being returned to the clients.
 */
@JsonDeserialize(using = AesExchangeKeyViewValue.Deserializer::class)
internal data class AesExchangeKeyViewValue(
	val ownerId: String,
	val ownerPublicKeyFingerprint: String,
	val delegatePublicKeyFingerprint: String,
	val encryptedExchangeKey: String,
) {
	/**
	 * Number and boolean elements are kept using their textual representation, as Jackson does by default for strings.
	 * Never throws on a row value of the wrong shape: the couchdb client would otherwise fail the whole query.
	 */
	class Deserializer : JsonDeserializer<AesExchangeKeyViewValue?>() {
		override fun deserialize(p: JsonParser, ctxt: DeserializationContext): AesExchangeKeyViewValue? {
			val node = p.readValueAsTree<JsonNode>()
			val values = (node as? ArrayNode)
				?.takeIf { it.size() == 4 }
				?.mapNotNull { element -> element.takeIf { it.isTextual || it.isNumber || it.isBoolean }?.asText() }
				?.takeIf { it.size == 4 }
//			if (values == null) {
//				log.warn("Discarded malformed aes exchange key view row of owner {}", node.get(0)?.textValue() ?: "<unknown>")
//			}
			return values?.let { (ownerId, ownerPublicKeyFingerprint, delegatePublicKeyFingerprint, encryptedExchangeKey) ->
				AesExchangeKeyViewValue(ownerId, ownerPublicKeyFingerprint, delegatePublicKeyFingerprint, encryptedExchangeKey)
			}
		}

//		companion object {
//			private val log = LoggerFactory.getLogger(Deserializer::class.java)
//		}
	}
}

/**
 * Groups the values of the rows of a `by_delegate_aes_exchange_keys` view as
 * `{ ownerId: { ownerPublicKeyFingerprint: { delegatePublicKeyFingerprint: encryptedExchangeKey } } }`.
 * If the same entry is emitted more than once the last one is kept.
 */
internal fun List<AesExchangeKeyViewValue>.toAesExchangeKeysMap(): Map<String, Map<String, Map<String, String>>> = groupBy { it.ownerId }.mapValues { (_, ownerValues) ->
	ownerValues.groupBy { it.ownerPublicKeyFingerprint.takeLast(32) }.mapValues { (_, keyValues) ->
		keyValues.associate { it.delegatePublicKeyFingerprint to it.encryptedExchangeKey }
	}
}
