package org.taktik.icure.services.external.rest.v2.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.SecureDelegationKeyMap
import org.taktik.icure.services.external.rest.v2.dto.SecureDelegationKeyMapDto
import org.taktik.icure.services.external.rest.v2.mapper.embed.DelegationV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.SecurityMetadataV2Mapper

@Mapper(
	componentModel = "spring",
	uses = [
		DelegationV2Mapper::class,
		SecurityMetadataV2Mapper::class,
	],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
)
interface SecureDelegationKeyMapV2Mapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
	)
	fun map(exchangeDataMapDto: SecureDelegationKeyMapDto): SecureDelegationKeyMap
	fun map(exchangeData: SecureDelegationKeyMap): SecureDelegationKeyMapDto
}
