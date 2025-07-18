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
data class ReplicationStatsDto(
	val revisionsChecked: Int? = null,
	val missingRevisionsFound: Int? = null,
	val docsRead: Int? = null,
	val docsWritten: Int? = null,
	val changesPending: Int? = null,
	val docWriteFailures: Int? = null,
	val checkpointedSourceSeq: String? = null,
	@JsonSerialize(using = ZonedDateTimeSerializer::class)
	@JsonDeserialize(using = ZonedDateTimeDeserializer::class)
	val startTime: ZonedDateTime? = null,
	val error: String? = null,
) : Serializable
