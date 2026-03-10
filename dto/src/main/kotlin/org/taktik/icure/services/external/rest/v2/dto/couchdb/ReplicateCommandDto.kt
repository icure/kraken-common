package org.taktik.icure.services.external.rest.v2.dto.couchdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.base.VersionableDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Data transfer object representing a CouchDB replication command, used to configure and trigger
 * database replication between a source and a target.
 */
data class ReplicateCommandDto(
	/** The unique identifier of this replication command. */
	override val id: String,
	/** The current revision of this replication command document. */
	override val rev: String? = null,
	/** Whether the replication should run continuously. */
	val continuous: Boolean = false,
	/** Whether to create the target database if it does not exist. */
	val createTarget: Boolean = false,
	/** An optional list of document ids to replicate. */
	val docIds: List<String>? = null,
	/** Whether to cancel an existing replication. */
	val cancel: Boolean? = null,
	/** An optional filter function name to apply during replication. */
	val filter: String? = null,
	/** An optional selector for filtering documents during replication. */
	val selector: String? = null,
	/** The source remote endpoint for the replication. */
	@param:Schema(required = true)
	val source: RemoteDto,
	/** The target remote endpoint for the replication. */
	@param:Schema(required = true)
	val target: RemoteDto,
) : VersionableDto<String> {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = id?.let { this.copy(id = it, rev = rev) } ?: this.copy(rev = rev)
}
