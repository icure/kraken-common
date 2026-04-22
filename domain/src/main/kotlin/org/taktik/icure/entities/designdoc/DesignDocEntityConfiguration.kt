package org.taktik.icure.entities.designdoc

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Versionable

@JsonIgnoreProperties(ignoreUnknown = true)
data class DesignDocEntityConfiguration(
	@param:JsonProperty("_rev") override val rev: String?,
	val applicationId: String,
	val entity: String,
	val version: Int,
	/**
	 * Links the view name to the partition index.
	 */
	val viewsToPartition: Map<String, Int>,
) : Versionable<String> {

	companion object {
		private const val ID_PREFIX = "ddocConfig"

		fun idOf(applicationId: String, entity: String, version: Int): String =
			"$ID_PREFIX:$applicationId:$entity:$version"
	}

	@JsonProperty("_id")
	override val id = "$ID_PREFIX:$applicationId:$entity:$version"

	override fun withIdRev(
		id: String?,
		rev: String
	): DesignDocEntityConfiguration {
		val (_, applicationId, entity, version) = id?.split(":", limit = 4) ?: listOf("", applicationId, entity, "$version")
		return copy(
			rev = rev,
			applicationId = applicationId,
			entity = entity,
			version = version.toInt()
		)
	}
}