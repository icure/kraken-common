/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.AccessLog
import org.taktik.icure.services.external.rest.v1.dto.AccessLogDto
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeStubMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.DelegationMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.SecurityMetadataMapper

@Mapper(
	componentModel = "spring",
	uses = [
		CodeStubMapper::class,
		DelegationMapper::class,
		SecurityMetadataMapper::class,
	],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
)
interface AccessLogMapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
	)
	fun map(accessLogDto: AccessLogDto): AccessLog
	fun map(accessLog: AccessLog): AccessLogDto
}
