package org.taktik.icure.entities.designdoc

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Versionable

@JsonIgnoreProperties(ignoreUnknown = true)
data class DesignDocUpdateTask(
	val taskId: String,
	@param:JsonProperty("_rev") override val rev: String? = null,
	val applicationId: String,
	val version: Int,
	val status: TaskStatus,
	val target: DbTarget
) : Versionable<String> {

	companion object {
		private const val ID_PREFIX = "ddocUpdateTask"
	}

	enum class TaskStatus { Pending, Started, Completed }

	sealed interface DbTarget {
		data object AllChildren : DbTarget
		data class Databases(val dbs: List<String>) : DbTarget
	}

	@JsonProperty("_id")
	override val id: String = "$ID_PREFIX:$taskId"

	override fun withIdRev(
		id: String?,
		rev: String
	): Versionable<String> = copy(
		taskId = id?.split(":", limit = 2)?.last() ?: taskId,
		rev = rev
	)
}