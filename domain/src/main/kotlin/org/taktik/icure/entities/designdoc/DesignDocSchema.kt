package org.taktik.icure.entities.designdoc

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Versionable

@JsonIgnoreProperties(ignoreUnknown = true)
data class DesignDocSchema(
	@param:JsonProperty("_rev") override val rev: String?,
	val applicationId: String,
	val version: Int,
	/**
	 * The key of the outermost map is the entity name. In the inner map, the key is the view name and the value is
	 * the index of the partition where that view resides.
	 */
	val viewsByEntity: Map<String, Map<String, Int>>
) : Versionable<String> {

	companion object {
		private const val ID_PREFIX = "ddocConfig"

		fun idOf(applicationId: String, version: Int): String = "$ID_PREFIX:$applicationId:$version"
	}

	@JsonProperty("_id")
	override val id = "$ID_PREFIX:$applicationId:$version"

	override fun withIdRev(
		id: String?,
		rev: String
	): DesignDocSchema {
		val (_, applicationId, version) = id?.split(":", limit = 3) ?: listOf("", applicationId, "$version")
		return copy(
			rev = rev,
			applicationId = applicationId,
			version = version.toInt()
		)
	}
}