/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.services.external.rest.v1.dto.HealthcarePartyDto
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeStubMapper
import org.taktik.icure.services.external.rest.v1.mapper.base.IdentifierMapper
import org.taktik.icure.services.external.rest.v1.mapper.base.PropertyStubMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.AddressMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.FinancialInstitutionInformationMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.FlatRateTarificationMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.HealthcarePartyHistoryStatusMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.PersonNameMapper

@Mapper(componentModel = "spring", uses = [IdentifierMapper::class, HealthcarePartyHistoryStatusMapper::class, FinancialInstitutionInformationMapper::class, AddressMapper::class, CodeStubMapper::class, FlatRateTarificationMapper::class, PersonNameMapper::class, PropertyStubMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface HealthcarePartyMapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
		Mapping(target = "publicProperties", ignore = true),
	)
	fun map(healthcarePartyDto: HealthcarePartyDto): HealthcareParty
	fun map(healthcareParty: HealthcareParty): HealthcarePartyDto
}
