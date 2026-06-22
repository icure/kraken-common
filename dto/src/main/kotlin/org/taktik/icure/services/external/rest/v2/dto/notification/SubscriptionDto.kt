package org.taktik.icure.services.external.rest.v2.dto.notification

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifiableDto
import org.taktik.icure.services.external.rest.v2.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v2.dto.specializations.AccessControlKeyHexStringDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a real-time event subscription for a specific entity class. Clients use this DTO to
 * declare which event types and optional filter criteria they want to be notified about.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.notification.SubscriptionDto")
data class SubscriptionDto<O : IdentifiableDto<String>>(
	/** The list of event types (CREATE, UPDATE, DELETE) to subscribe to. */
	@param:Schema(required = true)
	@ActiveField val eventTypes: List<SubscriptionEventType>,
	/** The fully-qualified or short class name of the entity to observe. */
	@param:Schema(required = true)
	@ActiveField val entityClass: String,
	/** An optional filter chain that narrows which entity instances trigger notifications. */
	@ActiveField val filter: FilterChain<O>?,
	/** Optional access-control keys used to scope the subscription to specific encrypted data. */
	@ActiveField val accessControlKeys: List<AccessControlKeyHexStringDto>?,
	/** When true, uses Cardinal model serialization for the entity payloads. */
	@ActiveField val useCardinalModelSerialization: Boolean? = null,
	@ActiveField val cardinalSdkVersion: String? = null,
	@ActiveField val includeLegacyFields: Boolean = false
) : java.io.Serializable

/**
 * The types of entity lifecycle events that can be observed via a subscription.
 */
enum class SubscriptionEventType {
	CREATE,
	UPDATE,
	DELETE,
}
