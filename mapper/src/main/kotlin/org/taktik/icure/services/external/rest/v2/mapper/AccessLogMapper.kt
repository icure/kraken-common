/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.mapper

import org.mapstruct.DefaultPassOnParameter
import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.AccessLog
import org.taktik.icure.services.external.rest.v2.dto.AccessLogDto
import org.taktik.icure.services.external.rest.v2.mapper.base.CodeStubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.DelegationV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.SecurityMetadataV2Mapper

@Mapper(
	componentModel = "spring",
	uses = [CodeStubV2Mapper::class, DelegationV2Mapper::class, SecurityMetadataV2Mapper::class],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
	defaultPassOnParameters = [
		DefaultPassOnParameter(
			type = org.taktik.icure.customentities.mapping.MapperExtensionsValidationContext::class,
			valueExpression = "org.taktik.icure.customentities.mapping.MapperExtensionsValidationContext.Empty",
			parameterName = "mapperExtensionsValidationContext",
		)
	]
)
interface AccessLogV2Mapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
	)
	// TODO update with proper validation context in future
	fun map(accessLogDto: AccessLogDto): AccessLog
	fun map(accessLog: AccessLog): AccessLogDto
}
