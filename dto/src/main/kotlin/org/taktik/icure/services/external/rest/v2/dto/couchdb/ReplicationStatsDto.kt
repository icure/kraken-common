package org.taktik.icure.services.external.rest.v2.dto.couchdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.taktik.couchdb.handlers.ZonedDateTimeDeserializer
import org.taktik.couchdb.handlers.ZonedDateTimeSerializer
import java.io.Serializable
import java.time.ZonedDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Data transfer object containing statistics about a CouchDB replication process.
 */
data class ReplicationStatsDto(
	/** The number of revisions checked during replication. */
	val revisionsChecked: Int? = null,
	/** The number of missing revisions found on the target. */
	val missingRevisionsFound: Int? = null,
	/** The number of documents read from the source. */
	val docsRead: Int? = null,
	/** The number of documents written to the target. */
	val docsWritten: Int? = null,
	/** The number of changes still pending replication. */
	val changesPending: Int? = null,
	/** The number of document write failures on the target. */
	val docWriteFailures: Int? = null,
	/** The last checkpointed source sequence identifier. */
	val checkpointedSourceSeq: String? = null,
	/** The time when the replication started. */
	@JsonSerialize(using = ZonedDateTimeSerializer::class)
	@JsonDeserialize(using = ZonedDateTimeDeserializer::class)
	val startTime: ZonedDateTime? = null,
	/** An error message if the replication encountered an error. */
	val error: String? = null,
) : Serializable
