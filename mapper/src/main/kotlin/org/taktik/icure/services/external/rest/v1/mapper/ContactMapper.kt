/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.Contact
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
		Mapping(target = "participants", expression = """kotlin((contactDto.participants.map { org.taktik.icure.services.external.rest.v1.dto.embed.ContactParticipantDto(it.key, it.value) } + contactDto.participantList).distinct().map { org.taktik.icure.entities.embed.ContactParticipant(org.taktik.icure.entities.base.ParticipantType.valueOf(it.type.name), it.hcpId) })"""),
	)
	fun map(contactDto: ContactDto): Contact

	@Mappings(
		Mapping(target = "participants", expression = """kotlin(contact.participants.associate { (type, hcpId) -> org.taktik.icure.services.external.rest.v1.dto.base.ParticipantTypeDto.valueOf(type.name) to hcpId })"""),
		Mapping(target = "participantList", expression = """kotlin(contact.participants.map { org.taktik.icure.services.external.rest.v1.dto.embed.ContactParticipantDto(org.taktik.icure.services.external.rest.v1.dto.base.ParticipantTypeDto.valueOf(it.type.name), it.hcpId) })"""),
	)
	fun map(contact: Contact): ContactDto
}

fun toEntity(contactDto: ContactDto, contactMapper: ContactMapper): Contact = Contact(
	id = contactDto.id,
	participants = (contactDto.participants.map { org.taktik.icure.services.external.rest.v1.dto.embed.ContactParticipantDto(it.key, it.value) } + contactDto.participantList).distinct().map { org.taktik.icure.entities.embed.ContactParticipant(org.taktik.icure.entities.base.ParticipantType.valueOf(it.type.name), it.hcpId) }
)