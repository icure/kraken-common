package org.taktik.icure.services.external.rest.v1.dto.couchdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.taktik.couchdb.handlers.ZonedDateTimeDeserializer
import org.taktik.couchdb.handlers.ZonedDateTimeSerializer
import org.taktik.icure.services.external.rest.v1.dto.base.VersionableDto
import java.time.ZonedDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ReplicatorDocumentDto(
	override val id: String,
	override val rev: String?,
	val source: RemoteDto? = null,
	val target: RemoteDto? = null,
	val owner: String? = null,
	val create_target: Boolean? = null,
	val continuous: Boolean? = null,
	val doc_ids: List<String>? = null,
	val replicationState: String? = null,
	@JsonSerialize(using = ZonedDateTimeSerializer::class)
	@JsonDeserialize(using = ZonedDateTimeDeserializer::class)
	val replicationStateTime: ZonedDateTime? = null,
	val replicationStats: ReplicationStatsDto? = null,
	val errorCount: Int? = null,
	val revsInfo: List<Map<String, String>>? = null,
	val revHistory: Map<String, String>? = null,
) : VersionableDto<String> {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = id?.let { this.copy(id = it, rev = rev) } ?: this.copy(rev = rev)
}
