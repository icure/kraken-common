/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.base.ParticipantType
import org.taktik.icure.entities.embed.ContactParticipant
import org.taktik.icure.services.external.rest.v1.dto.ContactDto
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeStubMapper
import org.taktik.icure.services.external.rest.v1.mapper.base.IdentifierMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.AddressMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.ContactParticipantMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.DelegationMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.SecurityMetadataMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.ServiceMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.SubContactMapper

@Mapper(componentModel = "spring", uses = [IdentifierMapper::class, SubContactMapper::class, CodeStubMapper::class, DelegationMapper::class, ServiceMapper::class, SecurityMetadataMapper::class, AddressMapper::class, ContactParticipantMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface ContactMapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
		Mapping(target = "notes", ignore = true),
		Mapping(target = "participants", expression = """kotlin(org.taktik.icure.services.external.rest.v1.mapper.ContactMapper.Companion.mapParticipants(contactDto))"""),
		Mapping(target = "participantList", expression = """kotlin(org.taktik.icure.services.external.rest.v1.mapper.ContactMapper.Companion.mapParticipantList(contactDto, this.contactParticipantMapper))"""),
	)
	fun map(contactDto: ContactDto): Contact

	@Mappings()
	fun map(contact: Contact): ContactDto

	companion object {
		fun mapParticipants(contactDto: ContactDto): Map<ParticipantType, String> {
			require(contactDto.participants.isEmpty() || contactDto.participantList.isEmpty()) {
				"ContactDto cannot have both participants map and participantList populated"
			}

			return contactDto.participantList.associate { participantDto ->
				ParticipantType.valueOf(participantDto.type.name) to participantDto.hcpId
			}.takeIf { it.size == contactDto.participantList.size }
				?: contactDto.participants.mapKeys { entry ->
					ParticipantType.valueOf(entry.key.name)
				}
		}

		fun mapParticipantList(contactDto: ContactDto, participantMapper: ContactParticipantMapper): List<ContactParticipant> {
			return contactDto.participantList.takeIf {
				it.groupingBy { participantDto -> participantDto.type }.eachCount().any { entry -> entry.value > 1 }
			}.orEmpty().map { participantMapper.map(it) }
		}
	}

	//TODO: Write a test
}