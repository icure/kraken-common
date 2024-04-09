package org.taktik.icure.services.external.rest.v2.dto.notification

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifiableDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Notification(val eventType: SubscriptionEventType, val entityClass: String, val entity: IdentifiableDto<String>)
