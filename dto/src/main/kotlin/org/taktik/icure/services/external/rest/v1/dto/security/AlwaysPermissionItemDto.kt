package org.taktik.icure.services.external.rest.v1.dto.security

import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.services.external.rest.v1.dto.filter.predicate.AlwaysPredicate

@JsonDeserialize(using = JsonDeserializer.None::class)
data class AlwaysPermissionItemDto(
	override val type: PermissionTypeDto,
) : PermissionItemDto {
	override val predicate = AlwaysPredicate()
}
