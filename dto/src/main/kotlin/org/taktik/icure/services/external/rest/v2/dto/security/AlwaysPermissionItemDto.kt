package org.taktik.icure.services.external.rest.v2.dto.security

import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonDiscriminated
import org.taktik.icure.services.external.rest.v2.dto.filter.predicate.AlwaysPredicate

@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonDiscriminated("AlwaysPermissionItemDto")
data class AlwaysPermissionItemDto(override val type: PermissionTypeDto) : PermissionItemDto {
	override val predicate = AlwaysPredicate()
}
