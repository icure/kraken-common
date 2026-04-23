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

import org.mapstruct.DefaultPassOnParameter
import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.PassOnParameter
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.base.ParticipantType
import org.taktik.icure.entities.embed.ContactParticipant
import org.taktik.icure.services.external.rest.ModelMappingVersionContext
import org.taktik.icure.services.external.rest.v2.dto.ContactDto
import org.taktik.icure.services.external.rest.v2.dto.base.ParticipantTypeDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ContactParticipantDto
import org.taktik.icure.services.external.rest.v2.mapper.base.CodeStubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.base.IdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.AddressV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.AnnotationV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.ContactParticipantV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.DelegationV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.SecurityMetadataV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.ServiceV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.SubContactV2Mapper

@Mapper(
	componentModel = "spring",
	uses = [
		IdentifierV2Mapper::class,
		SubContactV2Mapper::class,
		CodeStubV2Mapper::class,
		DelegationV2Mapper::class,
		ServiceV2Mapper::class,
		SecurityMetadataV2Mapper::class,
		AnnotationV2Mapper::class,
		AddressV2Mapper::class,
		ContactParticipantV2Mapper::class,
	],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
	defaultPassOnParameters = [
		DefaultPassOnParameter(
			type = org.taktik.icure.customentities.mapping.MapperExtensionsValidationContext::class,
			valueExpression = "org.taktik.icure.customentities.mapping.MapperExtensionsValidationContext.Empty",
			parameterName = "mapperExtensionsValidationContext",
		)
	]
)
interface ContactV2Mapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
		Mapping(target = "participants", expression = """kotlin(ContactV2Mapper.mapParticipants(contactDto, modelMappingVersionContext))"""),
		Mapping(target = "participantList", expression = """kotlin(ContactV2Mapper.mapParticipantList(contactDto, contactParticipantV2Mapper, modelMappingVersionContext))"""),
	)
	// TODO update with proper validation context in future
	fun map(contactDto: ContactDto, @PassOnParameter modelMappingVersionContext: ModelMappingVersionContext): Contact

	@Mappings(
		Mapping(target = "participants", expression = """kotlin(ContactV2Mapper.mapParticipants(contact, modelMappingVersionContext))"""),
		Mapping(target = "participantList", expression = """kotlin(ContactV2Mapper.mapParticipantList(contact, contactParticipantV2Mapper, modelMappingVersionContext))"""),
	)
	fun map(contact: Contact, @PassOnParameter modelMappingVersionContext: ModelMappingVersionContext): ContactDto

	companion object {
		fun mapParticipants(contactDto: ContactDto, modelMappingVersionContext: ModelMappingVersionContext): Map<ParticipantType, String> {
			require(contactDto.participants.isEmpty() || contactDto.participantList.isEmpty()) {
				"ContactDto cannot have both participants map and participantList populated"
			}

			return contactDto.participantList.takeIf {
				it.isNotEmpty() && !modelMappingVersionContext.shouldUseCardinalModel()
			}?.associate { participantDto ->
				ParticipantType.valueOf(participantDto.type.name) to participantDto.hcpId
			}?.takeIf { it.size == contactDto.participantList.size && it.none { (type) -> type == ParticipantType.Recorder } }
				?: contactDto.participants.mapKeys { entry ->
					ParticipantType.valueOf(entry.key.name)
				}
		}

		fun mapParticipantList(contactDto: ContactDto, participantMapper: ContactParticipantV2Mapper, modelMappingVersionContext: ModelMappingVersionContext): List<ContactParticipant> =
			contactDto.participantList.takeIf {
				it.groupingBy { participantDto -> participantDto.type }.eachCount().any { entry -> entry.value > 1 } // If there are multiple participant with the same type
					|| it.any { (type) -> type == ParticipantTypeDto.Recorder } // or at least one participant with type "Recorder"
					|| modelMappingVersionContext.shouldUseCardinalModel()   // or the contact was created with the Cardinal SDK
			}.orEmpty().map { participantMapper.map(it) }

		fun mapParticipants(contact: Contact, modelMappingVersionContext: ModelMappingVersionContext): Map<ParticipantTypeDto, String> =
			if (modelMappingVersionContext.shouldUseCardinalModel()) {
				emptyMap()
			} else {
				contact.participants.mapKeys { (k, _) -> ParticipantTypeDto.valueOf(k.name) }
			}

		fun mapParticipantList(
			contact: Contact,
			participantMapper: ContactParticipantV2Mapper,
			modelMappingVersionContext: ModelMappingVersionContext
		): List<ContactParticipantDto> =
			if (modelMappingVersionContext.shouldUseCardinalModel()) {
				require(contact.participants.isEmpty() || contact.participantList.isEmpty()) {
					"Invalid Contact: cannot have both participants map and participantList populated"
				}
				when {
					contact.participantList.isNotEmpty() -> contact.participantList.map(participantMapper::map)
					contact.participants.isNotEmpty() -> contact.participants.map { (k, v) ->
						ContactParticipantDto(type = ParticipantTypeDto.valueOf(k.name), hcpId = v)
					}
					else -> emptyList()
				}
			} else {
				contact.participantList.map(participantMapper::map)
			}
	}
}