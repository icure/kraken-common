package org.taktik.icure.services.external.rest.v2.dto.couchdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v2.dto.base.VersionableDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ReplicateCommandDto(
	override val id: String,
	override val rev: String?,
	val continuous: Boolean = false,
	val createTarget: Boolean = false,
	val docIds: List<String>? = null,
	val cancel: Boolean? = null,
	val filter: String? = null,
	val selector: String? = null,
	val source: RemoteDto,
	val target: RemoteDto
) : VersionableDto<String> {

	override fun withIdRev(id: String?, rev: String) = id?.let { this.copy(id = it, rev = rev) } ?: this.copy(rev = rev)
}
