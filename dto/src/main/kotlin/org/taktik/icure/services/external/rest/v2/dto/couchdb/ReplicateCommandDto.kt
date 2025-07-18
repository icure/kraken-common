package org.taktik.icure.services.external.rest.v2.dto.couchdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.base.VersionableDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ReplicateCommandDto(
	override val id: String,
	override val rev: String? = null,
	val continuous: Boolean = false,
	val createTarget: Boolean = false,
	val docIds: List<String>? = null,
	val cancel: Boolean? = null,
	val filter: String? = null,
	val selector: String? = null,
	@Schema(required = true)
	val source: RemoteDto,
	@Schema(required = true)
	val target: RemoteDto,
) : VersionableDto<String> {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = id?.let { this.copy(id = it, rev = rev) } ?: this.copy(rev = rev)
}
