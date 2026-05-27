package org.taktik.icure.entities.designdoc

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.embed.RevisionInfo

@JsonIgnoreProperties(ignoreUnknown = true)
data class DesignDocSchema(
	@param:JsonProperty("_rev") override val rev: String?,
	@JsonIgnore
	val applicationGroupId: String,
	@JsonIgnore
	val version: Int,
	/**
	 * The key of the outermost map is the entity name. In the inner map, the key is the view name and the value is
	 * the index of the partition where that view resides.
	 */
	val viewsByEntity: Map<String, Map<String, Int>>,
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,
	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = mapOf(),
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = listOf(),
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = listOf(),
) : StoredDocument {

	companion object {
		private const val ID_PREFIX = "ddc"
		fun idOf(applicationGroupId: String, version: Int): String = "$ID_PREFIX:$applicationGroupId:$version"

		@JvmStatic
		@JsonCreator
		fun jsonConstructor(
			@JsonProperty("_id") id: String,
			@JsonProperty("_rev") rev: String?,
			viewsByEntity: Map<String, Map<String, Int>>,
			deletionDate: Long? = null,
			@JsonProperty("_attachments") attachments: Map<String, Attachment>? = mapOf(),
			@JsonProperty("_revs_info") revisionsInfo: List<RevisionInfo>? = listOf(),
			@JsonProperty("_conflicts") conflicts: List<String>? = listOf()
		): DesignDocSchema {
			val (groupApplicationId, version) = id.split(":").let {
				require(it.size == 3) { "Invalid id for schema, should have 3 components" }
				require(it[0] == ID_PREFIX) { "Invalid id for configuration, should start with '$ID_PREFIX'" }
				requireNotNull(it[2].toIntOrNull()) { "Invalid id for schema, version must be an int" }
				it[1] to it[2].toInt()
			}
			return DesignDocSchema(
				rev = rev,
				applicationGroupId = groupApplicationId,
				version = version,
				viewsByEntity = viewsByEntity,
				deletionDate = deletionDate,
				attachments = attachments,
				revisionsInfo = revisionsInfo,
				conflicts = conflicts
			)
		}
	}

	@JsonProperty("_id")
	override val id = "$ID_PREFIX:$applicationGroupId:$version"

	override fun withIdRev(
		id: String?,
		rev: String
	): DesignDocSchema {
		val (_, applicationId, version) = id?.split(":", limit = 3) ?: listOf("", applicationGroupId, "$version")
		return copy(
			rev = rev,
			applicationGroupId = applicationId,
			version = version.toInt()
		)
	}

	override fun withDeletionDate(deletionDate: Long?): DesignDocSchema = copy(deletionDate = deletionDate)
}