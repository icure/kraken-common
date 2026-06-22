package org.taktik.icure.services.external.rest.v2.dto.couchdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.taktik.couchdb.handlers.ZonedDateTimeDeserializer
import org.taktik.couchdb.handlers.ZonedDateTimeSerializer
import java.io.Serializable
import java.time.ZonedDateTime
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Data transfer object containing statistics about a CouchDB replication process.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.couchdb.ReplicationStatsDto")
data class ReplicationStatsDto(
	/** The number of revisions checked during replication. */
	@ActiveField val revisionsChecked: Int? = null,
	/** The number of missing revisions found on the target. */
	@ActiveField val missingRevisionsFound: Int? = null,
	/** The number of documents read from the source. */
	@ActiveField val docsRead: Int? = null,
	/** The number of documents written to the target. */
	@ActiveField val docsWritten: Int? = null,
	/** The number of changes still pending replication. */
	@ActiveField val changesPending: Int? = null,
	/** The number of document write failures on the target. */
	@ActiveField val docWriteFailures: Int? = null,
	/** The last checkpointed source sequence identifier. */
	@ActiveField val checkpointedSourceSeq: String? = null,
	/** The time when the replication started. */
	@param:JsonSerialize(using = ZonedDateTimeSerializer::class)
	@param:JsonDeserialize(using = ZonedDateTimeDeserializer::class)
	@ActiveField val startTime: ZonedDateTime? = null,
	/** An error message if the replication encountered an error. */
	@ActiveField val error: String? = null,
) : Serializable
