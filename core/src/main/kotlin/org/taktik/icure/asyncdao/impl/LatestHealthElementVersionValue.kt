package org.taktik.icure.asyncdao.impl

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.node.ArrayNode

/**
 * The reduced value of `by_health_element_id_latest` for a health element, queried with `group=true`.
 *
 * The view may be defined in a database with either of two reduce functions, which give the same latest version but
 * in different shapes:
 * - the custom reduce `By_health_element_id_latest_reduce.js`: `[time, documentId]`;
 * - the built-in `_top_1` reducer (CouchDB 3.5+): `[[time, documentId]]`.
 *
 * Both shapes are accepted, so that a backend works with databases where the view has been migrated to `_top_1` and
 * with databases where it has not.
 */
@JsonDeserialize(using = LatestHealthElementVersionValue.Deserializer::class)
internal data class LatestHealthElementVersionValue(
	val documentId: String
) {
	object Deserializer : JsonDeserializer<LatestHealthElementVersionValue>() {
		override fun deserialize(
			p: JsonParser,
			ctxt: DeserializationContext
		): LatestHealthElementVersionValue {
			val jsonList = p.readValueAsTree<ArrayNode>()
			val version = (jsonList[0] as? ArrayNode)?.also {
				check(jsonList.size() == 1) { "Expected exactly 1 item in a _top_1 value" }
			} ?: jsonList
			check(version.size() == 2) { "Expected exactly 2 items in the latest version" }
			check(version[1].isTextual) { "Expected the second item of the latest version to be a document id" }
			return LatestHealthElementVersionValue(version[1].textValue())
		}
	}
}
