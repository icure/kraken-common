package org.taktik.icure.services.external.rest.v2.dto.security

import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.handlers.JsonDiscriminated
import org.taktik.icure.services.external.rest.v2.dto.filter.predicate.AlwaysPredicate

@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonDiscriminated("AlwaysPermissionItemDto")
data class AlwaysPermissionItemDto(
	@param:Schema(required = true)
	override val type: PermissionTypeDto,
) : PermissionItemDto {
	@Schema(defaultValue = "AlwaysPredicate()")
	override val predicate = AlwaysPredicate()
}
