package org.taktik.icure.services.external.rest.v2.dto.couchdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.taktik.couchdb.handlers.ZonedDateTimeDeserializer
import org.taktik.couchdb.handlers.ZonedDateTimeSerializer
import org.taktik.icure.services.external.rest.v2.dto.base.VersionableDto
import java.time.ZonedDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Data transfer object representing a CouchDB replicator document that tracks the state of a replication task.
 */
data class ReplicatorDocumentDto(
	/** The unique identifier of this replicator document. */
	override val id: String,
	/** The current revision of this document. */
	override val rev: String? = null,
	/** The source remote endpoint for the replication. */
	val source: RemoteDto? = null,
	/** The target remote endpoint for the replication. */
	val target: RemoteDto? = null,
	/** The owner of this replication task. */
	val owner: String? = null,
	/** Whether to create the target database if it does not exist. */
	val create_target: Boolean? = null,
	/** Whether the replication runs continuously. */
	val continuous: Boolean? = null,
	/** An optional list of document ids to replicate. */
	val doc_ids: List<String>? = null,
	/** The current state of the replication (e.g. triggered, completed, error). */
	val replicationState: String? = null,
	/** The time when the replication state was last updated. */
	@param:JsonSerialize(using = ZonedDateTimeSerializer::class)
	@param:JsonDeserialize(using = ZonedDateTimeDeserializer::class)
	val replicationStateTime: ZonedDateTime? = null,
	/** Statistics about the replication process. */
	val replicationStats: ReplicationStatsDto? = null,
	/** The number of errors encountered during replication. */
	val errorCount: Int? = null,
	/** Information about the document revisions. */
	val revsInfo: List<Map<String, String>>? = null,
	/** A map of the document revision history. */
	val revHistory: Map<String, String>? = null,
) : VersionableDto<String> {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = id?.let { this.copy(id = it, rev = rev) } ?: this.copy(rev = rev)
}
