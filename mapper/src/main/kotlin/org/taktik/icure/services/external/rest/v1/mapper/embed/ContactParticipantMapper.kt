package org.taktik.icure.services.external.rest.v1.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.embed.ContactParticipant
import org.taktik.icure.services.external.rest.v1.dto.embed.ContactParticipantDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface ContactParticipantMapper {
	fun map(participantDto: ContactParticipantDto): ContactParticipant

	fun map(participant: ContactParticipant): ContactParticipantDto
}