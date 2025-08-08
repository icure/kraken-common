package org.taktik.icure.entities.base

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Versionable

@JsonIgnoreProperties(ignoreUnknown = true)
data class EntityInfo(
	@param:JsonProperty("_id", access = JsonProperty.Access.WRITE_ONLY) override val id: String,
	@param:JsonProperty("_rev", access = JsonProperty.Access.WRITE_ONLY) override val rev: String?,
	@param:JsonProperty("rev_history", access = JsonProperty.Access.WRITE_ONLY) override val revHistory: Map<String, String>?,
	@param:JsonProperty("java_type", access = JsonProperty.Access.WRITE_ONLY) val fullyQualifiedName: String,
) : Versionable<String> {
	override fun withIdRev(id: String?, rev: String): Versionable<String> = throw UnsupportedOperationException()
}
