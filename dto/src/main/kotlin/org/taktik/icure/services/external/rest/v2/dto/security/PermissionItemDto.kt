package org.taktik.icure.services.external.rest.v2.dto.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonDiscriminator
import org.taktik.icure.services.external.rest.v2.dto.filter.predicate.Predicate
import org.taktik.icure.services.external.rest.v2.handlers.JacksonPermissionItemDeserializer
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDiscriminator("itemType")
@JsonDeserialize(using = JacksonPermissionItemDeserializer::class)
/**
 * Sealed interface representing a single permission entry that pairs a permission type with a predicate.
 * The predicate defines the condition under which the permission applies.
 */
sealed interface PermissionItemDto : Serializable {
	val itemType: String
		get() = this::class.simpleName!!
	val type: PermissionTypeDto
	val predicate: Predicate
}
