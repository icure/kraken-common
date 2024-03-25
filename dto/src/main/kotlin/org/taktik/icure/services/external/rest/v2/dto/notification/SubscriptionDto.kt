package org.taktik.icure.services.external.rest.v2.dto.notification

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifiableDto
import org.taktik.icure.services.external.rest.v2.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v2.dto.specializations.AccessControlKeyHexStringDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class SubscriptionDto<O : IdentifiableDto<String>>(
	val eventTypes: List<EventType>,
	val entityClass: String,
	val filter: FilterChain<O>?,
	val accessControlKeys: List<AccessControlKeyHexStringDto>?
) : java.io.Serializable

enum class EventType {
	CREATE, UPDATE, DELETE
}
