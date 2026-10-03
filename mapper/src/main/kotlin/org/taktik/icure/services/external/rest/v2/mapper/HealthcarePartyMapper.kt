/*
 *  iCure Data Stack. Copyright (c) 2020 Taktik SA
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public
 *     License along with this program.  If not, see
 *     <https://www.gnu.org/licenses/>.
 */

package org.taktik.icure.services.external.rest.v2.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import com.icure.cardinal.customentities.mapping.MapperExtensionsValidationContext
import org.mapstruct.PassOnParameter
import org.springframework.stereotype.Service
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.base.DataOwnerGroupLink
import org.taktik.icure.services.external.rest.ModelMappingVersionContext
import org.taktik.icure.services.external.rest.v1.mapper.base.CryptoActorMappingHelper
import org.taktik.icure.services.external.rest.v2.dto.HealthcarePartyDto
import org.taktik.icure.services.external.rest.v2.dto.base.DataOwnerGroupLinkDto
import org.taktik.icure.services.external.rest.v2.mapper.base.CodeStubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.base.DataOwnerGroupLinkV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.base.IdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.base.PropertyStubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.AddressV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.FinancialInstitutionInformationV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.FlatRateTarificationV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.HealthcarePartyHistoryStatusV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.PersonNameV2Mapper

interface HealthcarePartyV2Mapper {
	fun map(
		healthcarePartyDto: HealthcarePartyDto,
		mapperExtensionsValidationContext: MapperExtensionsValidationContext,
	): HealthcareParty
	fun map(
		healthcareParty: HealthcareParty,
		@PassOnParameter modelMappingVersionContext: ModelMappingVersionContext,
	): HealthcarePartyDto
}

@Service
internal class HealthcarePartyV2MapperImpl(
	private val precomputedLinksMapper: HealthcarePartyMapperWithPrecomputedLinks,
	private val dataOwnerGroupLinkV2Mapper: DataOwnerGroupLinkV2Mapper,
) : HealthcarePartyV2Mapper {
	override fun map(
		healthcarePartyDto: HealthcarePartyDto,
		mapperExtensionsValidationContext: MapperExtensionsValidationContext
	): HealthcareParty {
		// Dumb 1:1 copy: whether a link is admin-type or not is now intrinsic to its target, not declared here, so
		// there is nothing to fold/collapse on the way in. Validation and storage-shape normalization happen at the
		// logic layer.
		return precomputedLinksMapper.map(
			healthcarePartyDto,
			healthcarePartyDto.parentId,
			healthcarePartyDto.dataOwnerGroups.map(dataOwnerGroupLinkV2Mapper::map),
			mapperExtensionsValidationContext,
		)
	}

	override fun map(
		healthcareParty: HealthcareParty,
		modelMappingVersionContext: ModelMappingVersionContext,
	): HealthcarePartyDto {
		val (parentId, dataOwnerGroups) = CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(
			healthcareParty,
			dataOwnerGroupLinkV2Mapper,
			modelMappingVersionContext,
		)
		return precomputedLinksMapper.map(healthcareParty, parentId, dataOwnerGroups)
	}
}

@Mapper(
	componentModel = "spring",
	uses = [DataOwnerGroupLinkV2Mapper::class, IdentifierV2Mapper::class, HealthcarePartyHistoryStatusV2Mapper::class, FinancialInstitutionInformationV2Mapper::class, AddressV2Mapper::class, CodeStubV2Mapper::class, FlatRateTarificationV2Mapper::class, PersonNameV2Mapper::class, PropertyStubV2Mapper::class],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
)
internal interface HealthcarePartyMapperWithPrecomputedLinks {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
		Mapping(target = "parentId", expression = """kotlin(parentId)"""),
		Mapping(target = "dataOwnerGroups", expression = """kotlin(dataOwnerGroups)"""),
		Mapping(target = "extensions", expression = "kotlin(mapperExtensionsValidationContext.validateAndMapCurrentExtension(healthcarePartyDto.extensions))"),
	)
	fun map(healthcarePartyDto: HealthcarePartyDto, parentId: String?, dataOwnerGroups: List<DataOwnerGroupLink>, mapperExtensionsValidationContext: MapperExtensionsValidationContext): HealthcareParty
	@Mappings(
		Mapping(target = "parentId", expression = """kotlin(parentId)"""),
		Mapping(target = "dataOwnerGroups", expression = """kotlin(dataOwnerGroups)"""),
	)
	fun map(healthcareParty: HealthcareParty, parentId: String?, dataOwnerGroups: List<DataOwnerGroupLinkDto>): HealthcarePartyDto
}
