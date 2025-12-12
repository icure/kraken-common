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
	)
	fun map(contactDto: ContactDto): Contact

	@Mappings()
	fun map(contact: Contact): ContactDto

	companion object {
		fun mapParticipants(contactDto: ContactDto): Map<ParticipantType, String> {
			return contactDto.participantList
				.takeIf { participantList ->
					contactDto.participants.isEmpty() && participantList.groupBy { entry -> entry.type }.all { entry -> entry.value.size == 1 }
				}
				?.let { participant ->
					participant.associate { participantDto ->
						ParticipantType.valueOf(participantDto.type.name) to participantDto.hcpId
					}
				}
				?: contactDto.participants.mapKeys { entry ->
					ParticipantType.valueOf(entry.key.name)
				}
		}
	}
}