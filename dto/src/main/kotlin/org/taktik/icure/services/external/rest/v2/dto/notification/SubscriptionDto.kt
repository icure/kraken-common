package org.taktik.icure.services.external.rest.v2.dto.notification

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifiableDto
import org.taktik.icure.services.external.rest.v2.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v2.dto.specializations.AccessControlKeyHexStringDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class SubscriptionDto<O : IdentifiableDto<String>>(
	@get:Schema(required = true)
	val eventTypes: List<SubscriptionEventType>,
	@get:Schema(required = true)
	val entityClass: String,
	val filter: FilterChain<O>?,
	val accessControlKeys: List<AccessControlKeyHexStringDto>?,
) : java.io.Serializable

enum class SubscriptionEventType {
	CREATE,
	UPDATE,
	DELETE,
}
